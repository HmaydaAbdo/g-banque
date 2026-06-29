package staport.finance.gBanque.Agios.service.calculateurs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Agios.enums.TypeAgios;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories.NatureLigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.repositories.LigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.repositories.LigneFonctionnelleSpecifiqueRepository;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.repositories.MarcheRepository;
import staport.finance.gBanque.prefinancement.enums.Type;
import staport.finance.gBanque.prefinancement.repository.PrefinancementRepository;

import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Calculateur d'agios pour le type {@link TypeAgios#PREFINANCEMENT}.
 *
 * <p>Principe de calcul :</p>
 * <ol>
 *   <li>Pour chaque marché de la banque, résoudre le taux applicable :
 *       <ol>
 *         <li>Ligne spécifique : {@code LigneFonctionnelleSpecifique} active rattachée directement au marché.</li>
 *         <li>Ligne générale  : {@code LigneFonctionnelle} active dont la collection de marchés inclut ce marché.</li>
 *         <li>Si aucune ligne n'est trouvée, le marché est ignoré.</li>
 *       </ol>
 *   </li>
 *   <li>Reconstituer l'encours jour J :
 *       {@code Encours_j = Σ(DEBLOCAGE.montant | PAYE, dateValeur ≤ j) − Σ(REMBOURSEMENT.montant | PAYE, dateValeur ≤ j)}</li>
 *   <li>Si {@code Encours_j > 0} :
 *       {@code AgiosHT_j = Encours_j × (Taux / 100) / 360}</li>
 * </ol>
 *
 * <p>La TVA (10 %) et le TTC sont calculés par l'orchestrateur.</p>
 */
@Component
public class PrefinancementAgiosCalculateur implements AgiosCalculateurMultiMarche {

    private static final Logger log = LoggerFactory.getLogger(PrefinancementAgiosCalculateur.class);

    private static final String NATURE_PREFI_CODE  = "PREFINANCEMENT";  // code immuable en base
    private static final String NATURE_PREFI_LABEL = "Préfinancement";  // fallback label
    private static final BigDecimal DIVISEUR_360 = new BigDecimal("360");
    private static final int SCALE = 8;
    private static final int SCALE_FINAL = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final MarcheRepository marcheRepository;
    private final LigneFonctionnelleSpecifiqueRepository ligneSpecifiqueRepository;
    private final LigneFonctionnelleRepository ligneFonctionnelleRepository;
    private final NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository;
    private final PrefinancementRepository prefinancementRepository;

    /**
     * Cache du taux résolu par marché (valable pour toute la durée d'un recalcul trimestriel).
     * Clé : marcheId → taux (Optional.empty = pas de ligne PREFI pour ce marché).
     */
    private final Map<Long, Optional<BigDecimal>> cacheTauxParMarche = new ConcurrentHashMap<>();

    /**
     * Cache de la NatureLigneFonctionnelle Préfinancement — chargée une seule fois au démarrage du calcul.
     * Valeur null signifie que la nature n'a pas encore été résolue.
     */
    private volatile NatureLigneFonctionnelle naturePrefi = null;

    public PrefinancementAgiosCalculateur(
            MarcheRepository marcheRepository,
            LigneFonctionnelleSpecifiqueRepository ligneSpecifiqueRepository,
            LigneFonctionnelleRepository ligneFonctionnelleRepository,
            NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository,
            PrefinancementRepository prefinancementRepository) {
        this.marcheRepository = marcheRepository;
        this.ligneSpecifiqueRepository = ligneSpecifiqueRepository;
        this.ligneFonctionnelleRepository = ligneFonctionnelleRepository;
        this.natureLigneFonctionnelleRepository = natureLigneFonctionnelleRepository;
        this.prefinancementRepository = prefinancementRepository;
    }

    @Override
    public TypeAgios getType() {
        return TypeAgios.PREFINANCEMENT;
    }

    /**
     * Une banque est éligible si elle possède au moins un marché avec une ligne
     * préfinancement active (spécifique ou générale).
     */
    @Override
    @Transactional(readOnly = true)
    public boolean estEligible(Banque banque) {
        if (resolveNaturePrefi().isEmpty()) {
            log.warn("Nature '{}' (code={}) introuvable en base — aucune banque éligible PREFI",
                    NATURE_PREFI_LABEL, NATURE_PREFI_CODE);
            return false;
        }
        List<Marche> marches = marcheRepository.findByBanqueId(banque.getId());
        return marches.stream().anyMatch(m -> resoudreTaux(m).isPresent());
    }

    /**
     * Calcule les agios HT de chaque marché actif de la banque pour le jour donné.
     *
     * @return liste des résultats par marché (vide si aucun marché avec ligne PREFI)
     */
    @Override
    @Transactional(readOnly = true)
    public List<ResultatParMarche> calculerJourParMarche(Banque banque, LocalDate jour) {
        List<Marche> marches = marcheRepository.findByBanqueId(banque.getId());
        List<ResultatParMarche> resultats = new ArrayList<>();

        for (Marche marche : marches) {
            Optional<BigDecimal> tauxOpt = resoudreTaux(marche);
            if (tauxOpt.isEmpty()) {
                log.debug("Marché {} — aucune ligne PREFI active, ignoré", marche.getId());
                continue;
            }

            BigDecimal taux = tauxOpt.get();
            BigDecimal encours = calculerEncours(marche, jour);

            if (encours.compareTo(BigDecimal.ZERO) <= 0) {
                // Encours nul ou négatif : pas d'agios ce jour pour ce marché
                resultats.add(new ResultatParMarche(marche, AgiosCalculateur.ResultatJournalier.ZERO));
                continue;
            }

            // AgiosHT = Encours × (Taux / 100) / 360
            BigDecimal agiosHT = encours
                    .multiply(taux)
                    .divide(BigDecimal.valueOf(100).multiply(DIVISEUR_360), SCALE, ROUNDING)
                    .setScale(SCALE_FINAL, ROUNDING);

            String metadata = String.format("marche_id=%d|taux=%s|encours=%s",
                    marche.getId(), taux.toPlainString(), encours.toPlainString());

            log.debug("Marché {} — encours={} taux={}% agiosHT={} [{}]",
                    marche.getId(), encours, taux, agiosHT, jour);

            resultats.add(new ResultatParMarche(
                    marche,
                    new AgiosCalculateur.ResultatJournalier(encours, taux, agiosHT, metadata)
            ));
        }

        return resultats;
    }

    /** Vide les caches — peut être appelé entre deux banques dans l'orchestrateur si nécessaire. */
    public void viderCache() {
        cacheTauxParMarche.clear();
        naturePrefi = null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Résolution de la nature
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Résout la {@link NatureLigneFonctionnelle} correspondant au Préfinancement.
     * Cherche d'abord par label exact, puis par label LIKE en fallback.
     */
    private Optional<NatureLigneFonctionnelle> resolveNaturePrefi() {
        if (naturePrefi != null) {
            return Optional.of(naturePrefi);
        }

        // Priorité : code immuable (champ updatable=false en base)
        Optional<NatureLigneFonctionnelle> found =
                natureLigneFonctionnelleRepository.findByCode(NATURE_PREFI_CODE);

        // Fallback : label exact
        if (found.isEmpty()) {
            found = natureLigneFonctionnelleRepository.findByLabel(NATURE_PREFI_LABEL);
        }

        found.ifPresent(n -> {
            naturePrefi = n;
            log.info("Nature Préfinancement résolue : id={} code={} label={}",
                    n.getId(), n.getCode(), n.getLabel());
        });

        if (found.isEmpty()) {
            log.error("Nature '{}' (code={}) introuvable en base", NATURE_PREFI_LABEL, NATURE_PREFI_CODE);
        }

        return found;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Résolution du taux
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Résout le taux applicable pour un marché :
     * <ol>
     *   <li>Ligne spécifique active rattachée directement au marché (nature PREFI).</li>
     *   <li>Ligne générale active dont la collection de marchés inclut ce marché.</li>
     * </ol>
     *
     * @return Optional vide si aucune ligne PREFI n'est trouvée pour ce marché
     */
    private Optional<BigDecimal> resoudreTaux(Marche marche) {
        return cacheTauxParMarche.computeIfAbsent(marche.getId(), id -> {

            Optional<NatureLigneFonctionnelle> natureOpt = resolveNaturePrefi();
            if (natureOpt.isEmpty()) {
                return Optional.empty();
            }
            NatureLigneFonctionnelle nature = natureOpt.get();

            // 1. Ligne spécifique — utilise la méthode dédiée du repository (pas de Specification)
            Optional<LigneFonctionnelleSpecifique> specifique =
                    ligneSpecifiqueRepository.findByMarcheAndNatureLigneFonctionnelleAndActifTrue(
                            marche, nature);

            if (specifique.isPresent()) {
                log.debug("Marché {} — ligne spécifique PREFI id={} taux={}",
                        id, specifique.get().getId(), specifique.get().getTaux());
                return Optional.of(specifique.get().getTaux());
            }

            // 2. Ligne générale — utilise la query JPQL dédiée du repository
            Optional<LigneFonctionnelle> generale =
                    ligneFonctionnelleRepository.findActiveLineForMarche(id, nature.getId());

            if (generale.isPresent()) {
                log.debug("Marché {} — ligne générale PREFI id={} taux={}",
                        id, generale.get().getId(), generale.get().getTaux());
                return Optional.of(generale.get().getTaux());
            }

            log.debug("Marché {} — aucune ligne PREFI (ni spécifique ni générale)", id);
            return Optional.empty();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Calcul de l'encours
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Encours préfinancement au jour J pour un marché :
     * {@code Encours_j = Σ(DEBLOCAGE PAYE, dateValeur ≤ j) − Σ(REMBOURSEMENT PAYE, dateValeur ≤ j)}
     *
     * <p>Seuls les prefinancements avec {@code Status.PAYE} ont un effet financier réel.
     * Les {@code Status.PREVU} représentent des opérations futures non encore réalisées.</p>
     */
    private BigDecimal calculerEncours(Marche marche, LocalDate jour) {
        BigDecimal totalDeblocages = prefinancementRepository
                .sumMontantPaye(marche, Type.DEBLOCAGE, jour);
        BigDecimal totalRemboursements = prefinancementRepository
                .sumMontantPaye(marche, Type.REMBOURSEMENT, jour);

        if (totalDeblocages == null) totalDeblocages = BigDecimal.ZERO;
        if (totalRemboursements == null) totalRemboursements = BigDecimal.ZERO;

        return totalDeblocages.subtract(totalRemboursements);
    }
}
