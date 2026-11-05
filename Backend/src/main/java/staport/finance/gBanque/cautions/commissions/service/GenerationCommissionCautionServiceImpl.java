package staport.finance.gBanque.cautions.commissions.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.cautions.commissions.factory.CommissionCautionStrategieFactory;
import staport.finance.gBanque.cautions.commissions.strategy.CommissionCautionStrategie;
import staport.finance.gBanque.cautions.entities.Caution;
import staport.finance.gBanque.cautions.repository.CautionRepository;
import staport.finance.gBanque.commision.dtos.CommissionResponseDto;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.commision.enums.TypeCommission;
import staport.finance.gBanque.commision.service.ICommissionService;
import staport.finance.gBanque.impot_taxes.enums.Motif;
import staport.finance.gBanque.impot_taxes.service.IImpotEtTaxeService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Implémentation du service de génération mensuelle des commissions sur cautions.
 *
 * <p>Encapsulation des domaines (architecture.md) : toute création de commission ou de TVA
 * passe par la façade publique du module concerné ({@link ICommissionService},
 * {@link IImpotEtTaxeService}) — jamais par injection directe de leurs {@code Repository}.
 * Conséquence assumée : les commissions et TVA du batch sont persistées une à une (et non
 * via {@code saveAll} en lot) — le volume mensuel de cautions actives reste modeste et ce
 * traitement s'exécute une fois par mois, hors period de forte charge.
 *
 * <p>Stratégie transactionnelle :
 * <ul>
 *   <li>Classe annotée {@code @Transactional(readOnly = true)} — protection par défaut.</li>
 *   <li>{@code generer} annotée {@code @Transactional(isolation = READ_COMMITTED)} —
 *       standard bancaire : évite les dirty reads sans bloquer les lectures concurrentes.</li>
 * </ul>
 *
 * <p>Anti-N+1 :
 * <ul>
 *   <li>Une seule requête JPQL avec {@code EntityGraph} charge toutes les cautions actives
 *       avec leur {@code banque} et leur {@code nature} — aucune requête lazy supplémentaire
 *       lors du parcours de la liste.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class GenerationCommissionCautionServiceImpl implements IGenerationCommissionCautionService {

    private static final Logger log =
            LoggerFactory.getLogger(GenerationCommissionCautionServiceImpl.class);

    /** Taux de TVA légal applicable aux commissions sur cautions bancaires (statutaire, non configurable). */
    private static final BigDecimal TAUX_TVA_COMMISSION_CAUTION = new BigDecimal("0.10");

    private final CautionRepository                  cautionRepository;
    private final ICommissionService                 commissionService;
    private final IImpotEtTaxeService                impotEtTaxeService;
    private final CommissionCautionStrategieFactory  strategieFactory;

    public GenerationCommissionCautionServiceImpl(
            CautionRepository cautionRepository,
            ICommissionService commissionService,
            IImpotEtTaxeService impotEtTaxeService,
            CommissionCautionStrategieFactory strategieFactory) {
        this.cautionRepository    = cautionRepository;
        this.commissionService    = commissionService;
        this.impotEtTaxeService   = impotEtTaxeService;
        this.strategieFactory     = strategieFactory;
    }

    // ── Génération ────────────────────────────────────────────────────────────

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ResultatGenerationCommissionDto generer(YearMonth moisReference, boolean debutMois) {
        log.info("Début génération commissions cautions — mois référence : {}, débutMois : {}",
                moisReference, debutMois);

        // 1. Chargement en une seule requête (EntityGraph banque + nature + marche)
        List<Caution> cautions = cautionRepository.findActivesAvecBanqueEtNature();
        log.debug("{} caution(s) active(s) chargée(s)", cautions.size());

        List<CommissionResponseDto> commissionsCreees = new ArrayList<>();
        int ignorees = 0;

        for (Caution caution : cautions) {
            String nomBanque = caution.getBanque().getNom();

            // 2. Résolution de la stratégie par nom de banque
            Optional<CommissionCautionStrategie> strategieOpt = strategieFactory.trouver(nomBanque);
            if (strategieOpt.isEmpty()) {
                log.warn("Aucune stratégie de commission enregistrée pour la banque '{}'"
                        + " — caution {} ignorée", nomBanque, caution.getNumeroCaution());
                ignorees++;
                continue;
            }

            // 3. Filtrer selon le moment d'exécution (début ou fin de mois)
            CommissionCautionStrategie strategie = strategieOpt.get();
            if (strategie.executerEnDebutMois() != debutMois) {
                log.debug("Stratégie {} ignorée — moment d'exécution incompatible (attendu debutMois={})",
                        nomBanque, debutMois);
                ignorees++;
                continue;
            }

            // 3b. Facturation périodique : une banque trimestrielle n'est traitée qu'en fin de trimestre.
            if (moisReference.getMonthValue() % strategie.periodiciteMois() != 0) {
                log.debug("Stratégie {} ignorée — {} n'est pas un mois de facturation ({} mois)",
                        nomBanque, moisReference, strategie.periodiciteMois());
                ignorees++;
                continue;
            }

            // 3c. Prorata d'entrée des cautions prises pendant la période qui se clôt : généré ici (et non à la
            //     création), daté du jour de prise ; ignoré si une commission existe déjà à cette date.
            if (strategie.supporteProrataEntree()
                    && caution.getDateDePrise() != null
                    && estDansPeriode(caution.getDateDePrise(), periodeProrata(strategie, moisReference, debutMois))) {
                genererProrataEntree(caution, strategie, commissionsCreees);
            }

            // 4a. Garde prorata : pour les banques période courante (AWB, CDM, BP — facturerMoisSuivant=false),
            //     si un prorata d'entrée existe dans la période de facturation (dateEmission=dateDePrise),
            //     on skip pour éviter de facturer deux fois les mêmes jours.
            //     Pour les banques M+1 (BMCI, SAHAM BANK) cette garde n'est pas appliquée :
            //     prorata dateEmission=dateDePrise ≠ dateEmission batch=31/M → pas de conflit.
            LocalDate[] periodeCourante = periodeFacturation(strategie, moisReference);
            if (!strategie.facturerMoisSuivant()
                    && commissionService.existsByCautionAndDateEmissionBetween(
                            caution.getId(), periodeCourante[0], periodeCourante[1])) {
                log.debug("Prorata d'entrée déjà présent pour la caution {} (banque={}) "
                        + "dans la période {} → {} — période courante skippée",
                        caution.getNumeroCaution(), nomBanque, periodeCourante[0], periodeCourante[1]);
                ignorees++;
                continue;
            }

            // 4b. Calcul de la commission (délégué à la stratégie) — objet transitoire, jamais
            //     persisté directement (encapsulation du module commission).
            Commission commissionCalculee = strategie.calculer(caution, moisReference);

            // 4c. Garde idempotence exacte : une seule commission par caution par date d'émission
            if (commissionService.existsByCautionAndDateEmission(
                    caution.getId(), commissionCalculee.getDateEmission())) {
                log.warn("Commission déjà existante pour la caution {} (banque={}) à la date {} — ignorée",
                        caution.getNumeroCaution(), nomBanque, commissionCalculee.getDateEmission());
                ignorees++;
                continue;
            }

            CommissionResponseDto commissionPersistee = commissionService.creerCommissionCaution(
                    caution.getId(), caution.getBanque().getId(), commissionCalculee.getMontant(),
                    commissionCalculee.getDateEmission(), commissionCalculee.getLibelle(),
                    commissionCalculee.getObservation(), strategie.typeCommissionPeriodique());
            commissionsCreees.add(commissionPersistee);

            // 4d. TVA — uniquement pour les banques dont la stratégie l'autorise. CDM (et toute
            //     banque future avec genererTva()=false) est exclue : son montant de commission
            //     inclut déjà la TVA.
            if (strategie.genererTva()) {
                BigDecimal montantTva = commissionPersistee.montant()
                        .multiply(TAUX_TVA_COMMISSION_CAUTION)
                        .setScale(2, RoundingMode.HALF_UP);

                impotEtTaxeService.creerTvaSurCommission(
                        caution.getBanque().getId(), montantTva, Motif.TVA_COMMISSION_CAUTION,
                        commissionPersistee.dateEmission(), "TVA " + commissionPersistee.libelle(),
                        "TVA/commission/caution", commissionPersistee.id(), caution.getId());
                log.debug("TVA créée — commission id={}, montant TVA={}",
                        commissionPersistee.id(), montantTva);
            } else {
                log.debug("TVA non générée pour la commission id={} (banque={} : montant TTC inclus)",
                        commissionPersistee.id(), nomBanque);
            }
        }

        log.info("Génération terminée — {} commission(s) créée(s), {} ignorée(s) — mois cible : {}",
                commissionsCreees.size(), ignorees, moisReference.plusMonths(1));

        return new ResultatGenerationCommissionDto(commissionsCreees.size(), ignorees, moisReference);
    }

    @Override
    @Transactional(readOnly = true)
    public RattrapageCommissionCautionResultDto apercuRattrapage(RattrapageCommissionCautionRequest request) {
        return analyserRattrapage(request, false);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public RattrapageCommissionCautionResultDto rattraper(RattrapageCommissionCautionRequest request) {
        if (request.lignesSelectionnees() == null || request.lignesSelectionnees().isEmpty()) {
            throw new staport.finance.gBanque.exception.customExceptions.ValidationException(
                    "Sélectionnez au moins une caution à rattraper depuis l'aperçu.");
        }
        YearMonth cycle = YearMonth.of(request.annee(), request.mois());
        ZoneId zone = ZoneId.of("Africa/Casablanca");
        LocalDateTime maintenant = LocalDateTime.now(zone);
        YearMonth moisCourant = YearMonth.from(maintenant);
        if (cycle.isAfter(moisCourant)) {
            throw new staport.finance.gBanque.exception.customExceptions.ValidationException(
                    "Un cycle futur ne peut pas être rattrapé.");
        }
        if (cycle.equals(moisCourant)) {
            LocalDateTime echeance = request.debutMois()
                    ? cycle.atDay(1).atTime(3, 0)
                    : cycle.atEndOfMonth().atTime(20, 0);
            if (maintenant.isBefore(echeance)) {
                throw new staport.finance.gBanque.exception.customExceptions.ValidationException(
                        "L'exécution automatique de ce lot n'est pas encore échue.");
            }
        }
        return analyserRattrapage(request, true);
    }

    private RattrapageCommissionCautionResultDto analyserRattrapage(
            RattrapageCommissionCautionRequest request, boolean enregistrer) {
        YearMonth moisReference = YearMonth.of(request.annee(), request.mois());
        Set<Long> banquesSelectionnees = request.banqueIds() == null || request.banqueIds().isEmpty()
                ? null : Set.copyOf(request.banqueIds());
        Set<String> lignesSelectionnees = request.lignesSelectionnees() == null
                ? Set.of() : new java.util.HashSet<>(request.lignesSelectionnees());
        Set<String> lignesEligiblesSelectionnees = new java.util.HashSet<>();
        List<RattrapageCommissionCautionLineDto> lignes = new ArrayList<>();
        List<Caution> cautions = cautionRepository.findActivesAvecBanqueEtNature();
        int generees = 0;
        int commissionsSansProvenance = 0;

        for (Caution caution : cautions) {
            Long banqueId = caution.getBanque().getId();
            if (banquesSelectionnees != null && !banquesSelectionnees.contains(banqueId)) continue;

            Optional<CommissionCautionStrategie> strategieOpt = strategieFactory.trouver(caution.getBanque().getNom());
            if (strategieOpt.isEmpty()) continue;
            CommissionCautionStrategie strategie = strategieOpt.get();
            if (strategie.executerEnDebutMois() != request.debutMois()) continue;
            if (moisReference.getMonthValue() % strategie.periodiciteMois() != 0) continue;

            boolean prorataAVenir = false;
            if (strategie.supporteProrataEntree() && caution.getDateDePrise() != null
                    && estDansPeriode(caution.getDateDePrise(), periodeProrata(strategie, moisReference,
                    request.debutMois()))) {
                Commission prorata = strategie.calculerProrataEntree(caution);
                boolean origineInconnue = commissionService.existsCautionCommissionSansProvenance(
                        banqueId, prorata.getDateEmission(), prorata.getDateEmission());
                boolean existe = commissionService.existsByCautionAndDateEmission(
                        caution.getId(), prorata.getDateEmission());
                String statut = origineInconnue ? "ORIGINE_HISTORIQUE_A_CONTROLER"
                        : existe ? "DEJA_PRESENTE" : "A_GENERER";
                prorataAVenir = !origineInconnue && !existe;
                String cleProrata = cleLigne(caution.getId(), "PRORATA_ENTREE");
                if ("A_GENERER".equals(statut) && lignesSelectionnees.contains(cleProrata)) {
                    lignesEligiblesSelectionnees.add(cleProrata);
                }
                if (origineInconnue) commissionsSansProvenance++;
                if (enregistrer && "A_GENERER".equals(statut) && lignesSelectionnees.contains(cleProrata)) {
                    genererProrataEntree(caution, strategie, new ArrayList<>());
                    generees++;
                    statut = "GENEREE";
                }
                lignes.add(ligne(caution, prorata, strategie, moisReference, "PRORATA_ENTREE", statut,
                        origineInconnue ? "Une commission CAUTION sans rattachement à sa caution existe pour cette date. Vérification requise."
                                : null));
            }

            Commission commission = strategie.calculer(caution, moisReference);
            LocalDate[] periodeCourante = periodeFacturation(strategie, moisReference);
            boolean prorataCourantPresent = !strategie.facturerMoisSuivant()
                    && commissionService.existsByCautionAndDateEmissionBetween(
                    caution.getId(), periodeCourante[0], periodeCourante[1]);
            prorataCourantPresent |= prorataAVenir && !strategie.facturerMoisSuivant();
            LocalDate[] plageEmission = strategie.executerEnDebutMois()
                    ? new LocalDate[]{moisReference.atDay(1), moisReference.atEndOfMonth()}
                    : new LocalDate[]{moisReference.atDay(1), moisReference.atEndOfMonth()};
            boolean origineInconnue = commissionService.existsCautionCommissionSansProvenance(
                    banqueId, plageEmission[0], plageEmission[1]);
            boolean existe = commissionService.existsByCautionAndDateEmission(
                    caution.getId(), commission.getDateEmission());
            String statut = origineInconnue ? "ORIGINE_HISTORIQUE_A_CONTROLER"
                    : (existe || prorataCourantPresent) ? "DEJA_PRESENTE" : "A_GENERER";
            String detail = origineInconnue
                    ? "Une commission CAUTION sans rattachement à sa caution existe pour cette banque et cette période. Vérification requise."
                    : prorataCourantPresent ? "Un prorata existe déjà dans la période de facturation."
                    : null;
            if (origineInconnue) commissionsSansProvenance++;
            String cleCommission = cleLigne(caution.getId(), strategie.typeCommissionPeriodique().name());
            if ("A_GENERER".equals(statut) && lignesSelectionnees.contains(cleCommission)) {
                lignesEligiblesSelectionnees.add(cleCommission);
            }
            if (enregistrer && "A_GENERER".equals(statut) && lignesSelectionnees.contains(cleCommission)) {
                creerCommissionEtTva(caution, strategie, commission);
                generees++;
                statut = "GENEREE";
            }
            lignes.add(ligne(caution, commission, strategie, moisReference,
                    strategie.typeCommissionPeriodique().name(), statut, detail));
        }

        if (enregistrer && !lignesEligiblesSelectionnees.containsAll(lignesSelectionnees)) {
            throw new staport.finance.gBanque.exception.customExceptions.ValidationException(
                    "Une ou plusieurs lignes sélectionnées ne sont plus éligibles. Actualisez l'aperçu.");
        }

        int candidates = (int) lignes.stream().filter(l -> "A_GENERER".equals(l.statut()) || "GENEREE".equals(l.statut())).count();
        int presentes = (int) lignes.stream().filter(l -> "DEJA_PRESENTE".equals(l.statut())).count();
        return new RattrapageCommissionCautionResultDto(moisReference, request.debutMois(), candidates,
                generees, presentes, commissionsSansProvenance, cautionRepository.countByActif(false), List.copyOf(lignes));
    }

    private static String cleLigne(Long cautionId, String typeCommission) {
        return cautionId + ":" + typeCommission;
    }

    private void creerCommissionEtTva(Caution caution, CommissionCautionStrategie strategie,
                                      Commission commission) {
        CommissionResponseDto persistee = commissionService.creerCommissionCaution(
                caution.getId(), caution.getBanque().getId(), commission.getMontant(),
                commission.getDateEmission(), commission.getLibelle(), commission.getObservation(),
                strategie.typeCommissionPeriodique());
        if (strategie.genererTva()) {
            BigDecimal tva = persistee.montant().multiply(TAUX_TVA_COMMISSION_CAUTION)
                    .setScale(2, RoundingMode.HALF_UP);
            impotEtTaxeService.creerTvaSurCommission(caution.getBanque().getId(), tva,
                    Motif.TVA_COMMISSION_CAUTION, persistee.dateEmission(), "TVA " + persistee.libelle(),
                    "TVA/commission/caution", persistee.id(), caution.getId());
        }
    }

    private RattrapageCommissionCautionLineDto ligne(Caution caution, Commission commission,
                                                     CommissionCautionStrategie strategie, YearMonth reference,
                                                     String type, String statut, String detail) {
        YearMonth debut;
        YearMonth fin;
        if (strategie.periodiciteMois() == 3) {
            fin = reference;
            debut = reference.minusMonths(2);
        } else if (strategie.facturerMoisSuivant()) {
            debut = reference.plusMonths(1);
            fin = debut;
        } else {
            debut = reference;
            fin = reference;
        }
        String periode = debut.equals(fin) ? debut.toString() : debut + " – " + fin;
        return new RattrapageCommissionCautionLineDto(caution.getId(), caution.getNumeroCaution(),
                caution.getBanque().getId(), caution.getBanque().getNom(), type,
                commission.getDateEmission(), periode, commission.getMontant(), statut, detail);
    }

    // ── Périodes et prorata d'entrée ──────────────────────────────────────────

    /** Période de facturation courante : le mois de référence, ou le trimestre civil qu'il termine. */
    private static LocalDate[] periodeFacturation(CommissionCautionStrategie strategie, YearMonth moisReference) {
        return new LocalDate[] {
                moisReference.minusMonths(strategie.periodiciteMois() - 1L).atDay(1),
                moisReference.atEndOfMonth()
        };
    }

    /**
     * Période dont les prises de caution reçoivent leur prorata d'entrée à cette exécution : la période de
     * facturation courante, sauf pour une banque exécutée en début de mois (CDM) où le batch du 1er traite
     * le mois qui vient de s'achever.
     */
    private static LocalDate[] periodeProrata(CommissionCautionStrategie strategie, YearMonth moisReference,
                                              boolean debutMois) {
        if (debutMois) {
            YearMonth moisPrecedent = moisReference.minusMonths(1);
            return new LocalDate[] { moisPrecedent.atDay(1), moisPrecedent.atEndOfMonth() };
        }
        return periodeFacturation(strategie, moisReference);
    }

    private static boolean estDansPeriode(LocalDate date, LocalDate[] periode) {
        return !date.isBefore(periode[0]) && !date.isAfter(periode[1]);
    }

    /** Crée le prorata d'entrée (+ TVA si la banque l'exige) sauf s'il existe déjà une commission à la date de prise. */
    private void genererProrataEntree(Caution caution, CommissionCautionStrategie strategie,
                                      List<CommissionResponseDto> commissionsCreees) {
        Commission prorataCalcule = strategie.calculerProrataEntree(caution);

        if (commissionService.existsByCautionAndDateEmission(caution.getId(), prorataCalcule.getDateEmission())) {
            log.debug("Prorata d'entrée ignoré — commission déjà existante pour la caution {} à la date {}",
                    caution.getNumeroCaution(), prorataCalcule.getDateEmission());
            return;
        }

        CommissionResponseDto prorataPersiste = commissionService.creerCommissionCaution(
                caution.getId(), caution.getBanque().getId(), prorataCalcule.getMontant(),
                prorataCalcule.getDateEmission(), prorataCalcule.getLibelle(),
                prorataCalcule.getObservation(), TypeCommission.PRORATA_ENTREE);
        commissionsCreees.add(prorataPersiste);

        if (strategie.genererTva()) {
            BigDecimal montantTva = prorataPersiste.montant()
                    .multiply(TAUX_TVA_COMMISSION_CAUTION)
                    .setScale(2, RoundingMode.HALF_UP);

            impotEtTaxeService.creerTvaSurCommission(
                    caution.getBanque().getId(), montantTva, Motif.TVA_COMMISSION_CAUTION,
                    prorataPersiste.dateEmission(), "TVA " + prorataPersiste.libelle(),
                    "TVA/prorata/caution/creation", prorataPersiste.id(), caution.getId());
        }
    }
}
