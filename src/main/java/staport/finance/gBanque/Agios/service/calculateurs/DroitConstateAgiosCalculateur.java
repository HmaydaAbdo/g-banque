package staport.finance.gBanque.Agios.service.calculateurs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Agios.enums.TypeAgios;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories.NatureLigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.repositories.LigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.repositories.LigneFonctionnelleSpecifiqueRepository;
import staport.finance.gBanque.droitConstate.repositories.DroitConstateRepository;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.repositories.MarcheRepository;
import staport.finance.gBanque.prefinancement.enums.Type;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Calculateur d'agios pour le type {@link TypeAgios#DROIT_CONSTATE}.
 *
 * <p>Principe de calcul (identique au Préfinancement) :</p>
 * <ol>
 *   <li>Pour chaque marché de la banque, résoudre le taux applicable :
 *       <ol>
 *         <li>Ligne spécifique active rattachée directement au marché (nature DROIT_CONSTATE).</li>
 *         <li>Ligne générale active dont la collection de marchés inclut ce marché (nature DROIT_CONSTATE).</li>
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
public class DroitConstateAgiosCalculateur implements AgiosCalculateurMultiMarche {

    private static final Logger log = LoggerFactory.getLogger(DroitConstateAgiosCalculateur.class);

    private static final String NATURE_DC_CODE  = "DROIT_CONSTATE";
    private static final String NATURE_DC_LABEL = "Droit constaté";
    private static final BigDecimal DIVISEUR_360 = new BigDecimal("360");
    private static final int SCALE = 8;
    private static final int SCALE_FINAL = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final MarcheRepository marcheRepository;
    private final LigneFonctionnelleSpecifiqueRepository ligneSpecifiqueRepository;
    private final LigneFonctionnelleRepository ligneFonctionnelleRepository;
    private final NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository;
    private final DroitConstateRepository droitConstateRepository;

    /** Cache du taux résolu par marché — valide pour toute la durée d'un recalcul trimestriel. */
    private final Map<Long, Optional<BigDecimal>> cacheTauxParMarche = new ConcurrentHashMap<>();

    /** Cache de la NatureLigneFonctionnelle Droits Constatés. */
    private volatile NatureLigneFonctionnelle natureDC = null;

    public DroitConstateAgiosCalculateur(
            MarcheRepository marcheRepository,
            LigneFonctionnelleSpecifiqueRepository ligneSpecifiqueRepository,
            LigneFonctionnelleRepository ligneFonctionnelleRepository,
            NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository,
            DroitConstateRepository droitConstateRepository) {
        this.marcheRepository = marcheRepository;
        this.ligneSpecifiqueRepository = ligneSpecifiqueRepository;
        this.ligneFonctionnelleRepository = ligneFonctionnelleRepository;
        this.natureLigneFonctionnelleRepository = natureLigneFonctionnelleRepository;
        this.droitConstateRepository = droitConstateRepository;
    }

    @Override
    public TypeAgios getType() {
        return TypeAgios.DROIT_CONSTATE;
    }

    /**
     * Une banque est éligible si elle possède au moins un marché avec une ligne
     * droits constatés active (spécifique ou générale).
     */
    @Override
    @Transactional(readOnly = true)
    public boolean estEligible(Banque banque) {
        if (resolveNatureDC().isEmpty()) {
            log.warn("Nature '{}' (code={}) introuvable en base — aucune banque éligible DROIT_CONSTATE",
                    NATURE_DC_LABEL, NATURE_DC_CODE);
            return false;
        }
        List<Marche> marches = marcheRepository.findByBanqueId(banque.getId());
        return marches.stream().anyMatch(m -> resoudreTaux(m).isPresent());
    }

    /**
     * Calcule les agios HT de chaque marché actif de la banque pour le jour donné.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ResultatParMarche> calculerJourParMarche(Banque banque, LocalDate jour) {
        List<Marche> marches = marcheRepository.findByBanqueId(banque.getId());
        List<ResultatParMarche> resultats = new ArrayList<>();

        for (Marche marche : marches) {
            Optional<BigDecimal> tauxOpt = resoudreTaux(marche);
            if (tauxOpt.isEmpty()) {
                log.debug("Marché {} — aucune ligne DROIT_CONSTATE active, ignoré", marche.getId());
                continue;
            }

            BigDecimal taux = tauxOpt.get();
            BigDecimal encours = calculerEncours(marche, jour);

            if (encours.compareTo(BigDecimal.ZERO) <= 0) {
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

    /** Vide les caches — appelé par l'orchestrateur entre deux banques. */
    public void viderCache() {
        cacheTauxParMarche.clear();
        natureDC = null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Résolution de la nature
    // ─────────────────────────────────────────────────────────────────────────

    private Optional<NatureLigneFonctionnelle> resolveNatureDC() {
        if (natureDC != null) return Optional.of(natureDC);

        Optional<NatureLigneFonctionnelle> found =
                natureLigneFonctionnelleRepository.findByCode(NATURE_DC_CODE);

        if (found.isEmpty()) {
            found = natureLigneFonctionnelleRepository.findByLabel(NATURE_DC_LABEL);
        }

        found.ifPresent(n -> {
            natureDC = n;
            log.info("Nature Droits Constatés résolue : id={} code={} label={}",
                    n.getId(), n.getCode(), n.getLabel());
        });

        if (found.isEmpty()) {
            log.error("Nature '{}' (code={}) introuvable en base", NATURE_DC_LABEL, NATURE_DC_CODE);
        }

        return found;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Résolution du taux
    // ─────────────────────────────────────────────────────────────────────────

    private Optional<BigDecimal> resoudreTaux(Marche marche) {
        return cacheTauxParMarche.computeIfAbsent(marche.getId(), id -> {

            Optional<NatureLigneFonctionnelle> natureOpt = resolveNatureDC();
            if (natureOpt.isEmpty()) return Optional.empty();

            NatureLigneFonctionnelle nature = natureOpt.get();

            // 1. Ligne spécifique
            Optional<LigneFonctionnelleSpecifique> specifique =
                    ligneSpecifiqueRepository.findByMarcheAndNatureLigneFonctionnelleAndActifTrue(
                            marche, nature);

            if (specifique.isPresent()) {
                log.debug("Marché {} — ligne spécifique DC id={} taux={}",
                        id, specifique.get().getId(), specifique.get().getTaux());
                return Optional.of(specifique.get().getTaux());
            }

            // 2. Ligne générale
            Optional<LigneFonctionnelle> generale =
                    ligneFonctionnelleRepository.findActiveLineForMarche(id, nature.getId());

            if (generale.isPresent()) {
                log.debug("Marché {} — ligne générale DC id={} taux={}",
                        id, generale.get().getId(), generale.get().getTaux());
                return Optional.of(generale.get().getTaux());
            }

            log.debug("Marché {} — aucune ligne DROIT_CONSTATE (ni spécifique ni générale)", id);
            return Optional.empty();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Calcul de l'encours
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Encours droits constatés au jour J pour un marché :
     * {@code Encours_j = Σ(DEBLOCAGE PAYE, dateValeur ≤ j) − Σ(REMBOURSEMENT PAYE, dateValeur ≤ j)}
     */
    private BigDecimal calculerEncours(Marche marche, LocalDate jour) {
        BigDecimal totalDeblocages = droitConstateRepository
                .sumMontantPaye(marche, Type.DEBLOCAGE, jour);
        BigDecimal totalRemboursements = droitConstateRepository
                .sumMontantPaye(marche, Type.REMBOURSEMENT, jour);

        if (totalDeblocages == null) totalDeblocages = BigDecimal.ZERO;
        if (totalRemboursements == null) totalRemboursements = BigDecimal.ZERO;

        return totalDeblocages.subtract(totalRemboursements);
    }
}
