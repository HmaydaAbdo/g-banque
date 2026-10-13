package staport.finance.gBanque.Agios.service.calculateurs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Agios.enums.TypeAgios;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.INatureLigneFonctionnelleService;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.NatureLigneFonctionnelleService;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.services.LigneFonctionnelleReader;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.services.LigneFonctionnelleSpecifiqueReader;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.service.IMarcheService;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;
import staport.finance.gBanque.prefinancement.services.PrefinancementReader;

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
 *       {@code Encours_j = Σ(DEBLOCAGE.montant | PAYE, dateValeur ≤ j) − Σ(REMBOURSEMENT.montant | PAYE, dateValeur ≤ j − 1)}</li>
 *       <li>Un remboursement n'agit que le lendemain de sa date de valeur : le jour de remboursement est encore facturé
 *           sur l'ancien encours (règle du 13/10/2026). Les déblocages agissent dès leur date de valeur.</li>
 *   <li>Si {@code Encours_j > 0} :
 *       {@code AgiosHT_j = Encours_j × (Taux / 100) / 360}</li>
 * </ol>
 *
 * <p>La TVA (10 %) et le TTC sont calculés par l'orchestrateur.</p>
 *
 * <p>Résolution des lignes/marché/encours via les façades de lecture cross-domaine
 * ({@link LigneFonctionnelleReader}, {@link LigneFonctionnelleSpecifiqueReader},
 * {@link INatureLigneFonctionnelleService}, {@link IMarcheService}, {@link PrefinancementReader})
 * — jamais via leurs Repository respectifs directement (encapsulation des domaines,
 * architecture.md).</p>
 */
@Component
public class PrefinancementAgiosCalculateur implements AgiosCalculateurMultiMarche {

    private static final Logger log = LoggerFactory.getLogger(PrefinancementAgiosCalculateur.class);

    private static final String NATURE_PREFI_LABEL = "Préfinancement";  // fallback label
    private static final BigDecimal DIVISEUR_360 = new BigDecimal("360");
    private static final int SCALE = 8;
    private static final int SCALE_FINAL = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final IMarcheService marcheService;
    private final LigneFonctionnelleSpecifiqueReader ligneSpecifiqueReader;
    private final LigneFonctionnelleReader ligneFonctionnelleReader;
    private final INatureLigneFonctionnelleService natureLigneFonctionnelleService;
    private final PrefinancementReader prefinancementReader;

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
            IMarcheService marcheService,
            LigneFonctionnelleSpecifiqueReader ligneSpecifiqueReader,
            LigneFonctionnelleReader ligneFonctionnelleReader,
            INatureLigneFonctionnelleService natureLigneFonctionnelleService,
            PrefinancementReader prefinancementReader) {
        this.marcheService = marcheService;
        this.ligneSpecifiqueReader = ligneSpecifiqueReader;
        this.ligneFonctionnelleReader = ligneFonctionnelleReader;
        this.natureLigneFonctionnelleService = natureLigneFonctionnelleService;
        this.prefinancementReader = prefinancementReader;
    }

    @Override
    public TypeAgios getType() {
        return TypeAgios.PREFINANCEMENT;
    }

    /**
     * Une banque est éligible si elle possède au moins un marché avec une ligne
     * préfinancement active (spécifique ou générale) ET au moins une opération de préfinancement payée.
     * Produit un log INFO détaillant la répartition des marchés (avec/sans ligne).
     */
    @Override
    @Transactional(readOnly = true)
    public boolean estEligible(Banque banque) {
        if (resolveNaturePrefi().isEmpty()) {
            log.warn("PREFI — {} : nature '{}' (code={}) introuvable en base → banque ignorée",
                    banque.getNom(), NATURE_PREFI_LABEL, NatureLigneFonctionnelleService.PREFINANCEMENT_CODE);
            return false;
        }
        List<Marche> marches = marcheService.findByBanqueId(banque.getId());
        if (marches.isEmpty()) {
            log.info("PREFI — {} : aucun marché rattaché → banque ignorée", banque.getNom());
            return false;
        }

        // Pré-chauffe le cache et classe chaque marché
        List<String> avecLigne = new ArrayList<>();
        List<String> sansLigne = new ArrayList<>();
        List<String> sansOperation = new ArrayList<>();
        for (Marche m : marches) {
            if (resoudreTaux(m).isEmpty())       sansLigne.add(m.getNom() + " #" + m.getId());
            else if (!aDesOperationsPayees(m))   sansOperation.add(m.getNom() + " #" + m.getId());
            else                                 avecLigne.add(m.getNom() + " #" + m.getId());
        }

        if (avecLigne.isEmpty()) {
            log.info("PREFI — {} : {} marchés, aucun avec ligne PREFI active ET opération payée → banque ignorée | sans ligne : {} | sans opération payée : {}",
                    banque.getNom(), marches.size(), sansLigne, sansOperation);
            return false;
        }

        log.info("PREFI — {} : {}/{} marchés avec ligne [{}]{}",
                banque.getNom(),
                avecLigne.size(), marches.size(),
                String.join(", ", avecLigne),
                sansLigne.isEmpty() ? "" : " | " + sansLigne.size() + " sans ligne : " + sansLigne);
        return true;
    }

    /**
     * Calcule les agios HT de chaque marché actif de la banque pour le jour donné.
     *
     * @return liste des résultats par marché (vide si aucun marché avec ligne PREFI)
     */
    @Override
    @Transactional(readOnly = true)
    public List<ResultatParMarche> calculerJourParMarche(Banque banque, LocalDate jour) {
        List<Marche> marches = marcheService.findByBanqueId(banque.getId());
        List<ResultatParMarche> resultats = new ArrayList<>();

        for (Marche marche : marches) {
            Optional<BigDecimal> tauxOpt = resoudreTaux(marche);
            if (tauxOpt.isEmpty()) {
                log.debug("PREFI — {} — {} : marché '{}' #{} sans ligne active, ignoré",
                        banque.getNom(), jour, marche.getNom(), marche.getId());
                continue;
            }

            BigDecimal taux    = tauxOpt.get();
            BigDecimal encours = calculerEncours(marche, jour);

            if (encours.compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("PREFI — {} — {} : marché '{}' #{} encours={} ≤ 0, pas d'agios",
                        banque.getNom(), jour, marche.getNom(), marche.getId(), encours);
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

            log.debug("PREFI — {} — {} : marché '{}' #{} encours={} taux={}% → agiosHT={}",
                    banque.getNom(), jour, marche.getNom(), marche.getId(), encours, taux, agiosHT);

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
                natureLigneFonctionnelleService.findByCode(NatureLigneFonctionnelleService.PREFINANCEMENT_CODE);

        // Fallback : label exact
        if (found.isEmpty()) {
            found = natureLigneFonctionnelleService.findByLabel(NATURE_PREFI_LABEL);
        }

        found.ifPresent(n -> {
            naturePrefi = n;
            log.info("Nature Préfinancement résolue : id={} code={} label={}",
                    n.getId(), n.getCode(), n.getLabel());
        });

        if (found.isEmpty()) {
            log.error("Nature '{}' (code={}) introuvable en base",
                    NATURE_PREFI_LABEL, NatureLigneFonctionnelleService.PREFINANCEMENT_CODE);
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

            // 1. Ligne spécifique — via la façade de lecture cross-domaine
            Optional<LigneFonctionnelleSpecifique> specifique =
                    ligneSpecifiqueReader.findActiveSansVerrou(
                            marche, nature);

            if (specifique.isPresent()) {
                log.debug("Marché {} — ligne spécifique PREFI id={} taux={}",
                        id, specifique.get().getId(), specifique.get().getTaux());
                return Optional.of(specifique.get().getTaux());
            }

            // 2. Ligne générale — via la façade de lecture cross-domaine
            Optional<LigneFonctionnelle> generale =
                    ligneFonctionnelleReader.findActiveLineForMarcheSansVerrou(id, nature.getId());

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
     * Un marché ne produit des agios de préfinancement que s'il a au moins une opération payée : sans elle, l'encours est
     * nul à tout moment et la période calculée serait vide (0 jour, 0 MAD).
     */
    private boolean aDesOperationsPayees(Marche marche) {
        return prefinancementReader.findByMarche(marche).stream()
                .anyMatch(o -> o.getStatus() == Status.PAYE);
    }

    /**
     * Encours préfinancement au jour J pour un marché :
     * {@code Encours_j = Σ(DEBLOCAGE PAYE, dateValeur ≤ j) − Σ(REMBOURSEMENT PAYE, dateValeur ≤ j − 1)}
     *
     * <p>Le jour d'un remboursement est donc encore facturé : le remboursement n'agit que le lendemain de sa date
     * de valeur. Les déblocages, eux, agissent dès leur date de valeur.</p>
     *
     * <p>Seuls les prefinancements avec {@code Status.PAYE} ont un effet financier réel.
     * Les {@code Status.PREVU} représentent des opérations futures non encore réalisées.</p>
     */
    private BigDecimal calculerEncours(Marche marche, LocalDate jour) {
        BigDecimal totalDeblocages = prefinancementReader.sumMontantPaye(marche, Type.DEBLOCAGE, jour);
        BigDecimal totalRemboursements = prefinancementReader.sumMontantPaye(marche, Type.REMBOURSEMENT, jour.minusDays(1));

        return totalDeblocages.subtract(totalRemboursements);
    }
}
