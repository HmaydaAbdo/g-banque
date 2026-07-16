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
import staport.finance.gBanque.commision.service.ICommissionService;
import staport.finance.gBanque.impot_taxes.enums.Motif;
import staport.finance.gBanque.impot_taxes.service.IImpotEtTaxeService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

            // 4a. Garde prorata : pour les banques mois courant (AWB, CDM — facturerMoisSuivant=false),
            //     si un prorata d'entrée a été généré à la création dans le mois de référence
            //     (dateEmission=dateDePrise ∈ moisReference), on skip pour éviter de facturer
            //     deux fois le même mois.
            //     Pour les banques M+1 (BMCI, SAHAM BANK) cette garde n'est pas appliquée :
            //     prorata dateEmission=dateDePrise ≠ dateEmission batch=31/M → pas de conflit.
            if (!strategie.facturerMoisSuivant()
                    && commissionService.existsByCautionAndDateEmissionBetween(
                            caution.getId(),
                            moisReference.atDay(1),
                            moisReference.atEndOfMonth())) {
                log.debug("Prorata d'entrée déjà présent pour la caution {} (banque={}) "
                        + "dans le mois {} — mois courant skippé",
                        caution.getNumeroCaution(), nomBanque, moisReference);
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
                    commissionCalculee.getObservation());
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
}
