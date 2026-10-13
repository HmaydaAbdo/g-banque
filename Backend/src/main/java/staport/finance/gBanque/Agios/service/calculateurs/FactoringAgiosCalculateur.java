package staport.finance.gBanque.Agios.service.calculateurs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Agios.enums.TypeAgios;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.INatureLigneFonctionnelleService;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.NatureLigneFonctionnelleService;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.services.LigneFonctionnelleReader;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.services.LigneFonctionnelleSpecifiqueReader;
import staport.finance.gBanque.factoring.service.FactoringReader;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.service.IMarcheService;
import staport.finance.gBanque.prefinancement.enums.Status;
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
 * Calculateur d'agios pour le type {@link TypeAgios#FACTORING}.
 *
 * <p>Périodicité : <b>mensuelle</b> (un {@code AgiosTrimestriel} par mois, le champ
 * {@code trimestre} contient le numéro de mois 1–12).</p>
 *
 * <p>Principe de calcul (identique au Préfinancement et aux Droits Constatés) :</p>
 * <ol>
 *   <li>Pour chaque marché de la banque, résoudre le taux applicable :
 *     <ol>
 *       <li>Ligne spécifique active rattachée directement au marché (nature FACTORING).</li>
 *       <li>Ligne générale active dont la collection de marchés inclut ce marché (nature FACTORING).</li>
 *       <li>Si aucune ligne n'est trouvée, le marché est ignoré silencieusement.</li>
 *     </ol>
 *   </li>
 *   <li>Reconstituer l'encours au jour J :
 *       {@code Encours_j = Σ(DEBLOCAGE.montant | PAYE, dateValeur ≤ j) − Σ(REMBOURSEMENT.montant | PAYE, dateValeur ≤ j)}</li>
 *   <li>Si {@code Encours_j > 0} :
 *       {@code AgiosHT_j = Encours_j × (Taux / 100) / 360}</li>
 * </ol>
 *
 * <p>La TVA (10 %) et le TTC sont calculés par l'orchestrateur.</p>
 *
 * <p>Résolution des lignes/marché/encours via les façades de lecture cross-domaine
 * ({@link LigneFonctionnelleReader}, {@link LigneFonctionnelleSpecifiqueReader},
 * {@link INatureLigneFonctionnelleService}, {@link IMarcheService}, {@link FactoringReader})
 * — jamais via leurs Repository respectifs directement (encapsulation des domaines,
 * architecture.md).</p>
 */
@Component
public class FactoringAgiosCalculateur implements AgiosCalculateurMultiMarche {

    private static final Logger log = LoggerFactory.getLogger(FactoringAgiosCalculateur.class);

    private static final String NATURE_FACTORING_LABEL = "Factoring";
    private static final BigDecimal DIVISEUR_360 = new BigDecimal("360");
    private static final int SCALE       = 8;
    private static final int SCALE_FINAL = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final IMarcheService                        marcheService;
    private final LigneFonctionnelleSpecifiqueReader    ligneSpecifiqueReader;
    private final LigneFonctionnelleReader              ligneFonctionnelleReader;
    private final INatureLigneFonctionnelleService      natureLigneFonctionnelleService;
    private final FactoringReader                       factoringReader;

    /** Cache du taux résolu par marché — valide pour toute la durée d'un recalcul mensuel. */
    private final Map<Long, Optional<BigDecimal>> cacheTauxParMarche = new ConcurrentHashMap<>();

    /** Cache de la NatureLigneFonctionnelle Factoring — chargée une seule fois par banque. */
    private volatile NatureLigneFonctionnelle natureFactoring = null;

    public FactoringAgiosCalculateur(
            IMarcheService                        marcheService,
            LigneFonctionnelleSpecifiqueReader    ligneSpecifiqueReader,
            LigneFonctionnelleReader              ligneFonctionnelleReader,
            INatureLigneFonctionnelleService      natureLigneFonctionnelleService,
            FactoringReader                       factoringReader) {
        this.marcheService               = marcheService;
        this.ligneSpecifiqueReader        = ligneSpecifiqueReader;
        this.ligneFonctionnelleReader     = ligneFonctionnelleReader;
        this.natureLigneFonctionnelleService = natureLigneFonctionnelleService;
        this.factoringReader              = factoringReader;
    }

    @Override
    public TypeAgios getType() {
        return TypeAgios.FACTORING;
    }

    /**
     * Une banque est éligible si au moins un de ses marchés possède une ligne
     * Factoring active (spécifique ou générale) ET au moins une opération de factoring payée.
     * Produit un log INFO avec la répartition marchés avec/sans ligne.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean estEligible(Banque banque) {
        if (resolveNatureFactoring().isEmpty()) {
            log.warn("FACTORING — {} : nature '{}' (code={}) introuvable en base → banque ignorée",
                    banque.getNom(), NATURE_FACTORING_LABEL, NatureLigneFonctionnelleService.FACTORING_CODE);
            return false;
        }
        List<Marche> marches = marcheService.findByBanqueId(banque.getId());
        if (marches.isEmpty()) {
            log.info("FACTORING — {} : aucun marché rattaché → banque ignorée", banque.getNom());
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
            log.info("FACTORING — {} : {} marchés, aucun avec ligne Factoring active ET opération payée → banque ignorée | sans ligne : {} | sans opération payée : {}",
                    banque.getNom(), marches.size(), sansLigne, sansOperation);
            return false;
        }

        log.info("FACTORING — {} : {}/{} marchés avec ligne [{}]{}",
                banque.getNom(),
                avecLigne.size(), marches.size(),
                String.join(", ", avecLigne),
                sansLigne.isEmpty() ? "" : " | " + sansLigne.size() + " sans ligne : " + sansLigne);
        return true;
    }

    /**
     * Calcule les agios HT de chaque marché actif de la banque pour le jour donné.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ResultatParMarche> calculerJourParMarche(Banque banque, LocalDate jour) {
        List<Marche> marches = marcheService.findByBanqueId(banque.getId());
        List<ResultatParMarche> resultats = new ArrayList<>();

        for (Marche marche : marches) {
            Optional<BigDecimal> tauxOpt = resoudreTaux(marche);
            if (tauxOpt.isEmpty()) {
                log.debug("FACTORING — {} — {} : marché '{}' #{} sans ligne active, ignoré",
                        banque.getNom(), jour, marche.getNom(), marche.getId());
                continue;
            }

            BigDecimal taux    = tauxOpt.get();
            BigDecimal encours = calculerEncours(marche, jour);

            if (encours.compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("FACTORING — {} — {} : marché '{}' #{} encours={} ≤ 0, pas d'agios",
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

            log.debug("FACTORING — {} — {} : marché '{}' #{} encours={} taux={}% → agiosHT={}",
                    banque.getNom(), jour, marche.getNom(), marche.getId(), encours, taux, agiosHT);

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
        natureFactoring = null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Résolution de la nature
    // ─────────────────────────────────────────────────────────────────────────

    private Optional<NatureLigneFonctionnelle> resolveNatureFactoring() {
        if (natureFactoring != null) return Optional.of(natureFactoring);

        Optional<NatureLigneFonctionnelle> found =
                natureLigneFonctionnelleService.findByCode(NatureLigneFonctionnelleService.FACTORING_CODE);
        if (found.isEmpty()) {
            found = natureLigneFonctionnelleService.findByLabel(NATURE_FACTORING_LABEL);
        }

        found.ifPresent(n -> {
            natureFactoring = n;
            log.info("Nature Factoring résolue : id={} code={} label={}",
                    n.getId(), n.getCode(), n.getLabel());
        });

        if (found.isEmpty()) {
            log.error("Nature '{}' (code={}) introuvable en base",
                    NATURE_FACTORING_LABEL, NatureLigneFonctionnelleService.FACTORING_CODE);
        }
        return found;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Résolution du taux
    // ─────────────────────────────────────────────────────────────────────────

    private Optional<BigDecimal> resoudreTaux(Marche marche) {
        return cacheTauxParMarche.computeIfAbsent(marche.getId(), id -> {
            Optional<NatureLigneFonctionnelle> natureOpt = resolveNatureFactoring();
            if (natureOpt.isEmpty()) return Optional.empty();

            NatureLigneFonctionnelle nature = natureOpt.get();

            // 1. Ligne spécifique
            Optional<LigneFonctionnelleSpecifique> specifique =
                    ligneSpecifiqueReader.findActiveSansVerrou(
                            marche, nature);
            if (specifique.isPresent()) {
                log.debug("Marché {} — ligne spécifique FACTORING id={} taux={}",
                        id, specifique.get().getId(), specifique.get().getTaux());
                return Optional.of(specifique.get().getTaux());
            }

            // 2. Ligne générale
            Optional<LigneFonctionnelle> generale =
                    ligneFonctionnelleReader.findActiveLineForMarcheSansVerrou(id, nature.getId());
            if (generale.isPresent()) {
                log.debug("Marché {} — ligne générale FACTORING id={} taux={}",
                        id, generale.get().getId(), generale.get().getTaux());
                return Optional.of(generale.get().getTaux());
            }

            log.debug("Marché {} — aucune ligne FACTORING (ni spécifique ni générale)", id);
            return Optional.empty();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Calcul de l'encours
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Un marché ne produit des agios de factoring que s'il a au moins une opération payée : sans elle, l'encours est
     * nul à tout moment et la période calculée serait vide (0 jour, 0 MAD).
     */
    private boolean aDesOperationsPayees(Marche marche) {
        return factoringReader.findByMarche(marche).stream()
                .anyMatch(o -> o.getStatus() == Status.PAYE);
    }

    /**
     * Encours factoring au jour J pour un marché :
     * {@code Encours_j = Σ(DEBLOCAGE PAYE, dateValeur ≤ j) − Σ(REMBOURSEMENT PAYE, dateValeur ≤ j)}
     */
    private BigDecimal calculerEncours(Marche marche, LocalDate jour) {
        BigDecimal totalDeblocages    = factoringReader.sumMontantPaye(marche, Type.DEBLOCAGE,     jour);
        BigDecimal totalRemboursements = factoringReader.sumMontantPaye(marche, Type.REMBOURSEMENT, jour);

        return totalDeblocages.subtract(totalRemboursements);
    }
}
