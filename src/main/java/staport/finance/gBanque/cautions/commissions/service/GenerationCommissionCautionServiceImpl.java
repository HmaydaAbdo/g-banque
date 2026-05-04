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
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.commision.repository.CommisionRepository;
import staport.finance.gBanque.impot_taxes.entities.ImpotEtTaxe;
import staport.finance.gBanque.impot_taxes.enums.Motif;
import staport.finance.gBanque.impot_taxes.repositories.ImpotEtTaxeRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation du service de génération mensuelle des commissions sur cautions.
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
 *
 */
@Service
@Transactional(readOnly = true)
public class GenerationCommissionCautionServiceImpl implements IGenerationCommissionCautionService {

    private static final Logger logger =
            LoggerFactory.getLogger(GenerationCommissionCautionServiceImpl.class);

    /** Taux TVA légal applicable aux commissions sur cautions bancaires (10%). */
    private static final BigDecimal TAUX_TVA_COMMISSION_CAUTION = new BigDecimal("0.10");

    private final CautionRepository                  cautionRepository;
    private final CommisionRepository                commissionRepository;
    private final ImpotEtTaxeRepository              impotEtTaxeRepository;
    private final CommissionCautionStrategieFactory  strategieFactory;

    public GenerationCommissionCautionServiceImpl(
            CautionRepository cautionRepository,
            CommisionRepository commissionRepository,
            ImpotEtTaxeRepository impotEtTaxeRepository,
            CommissionCautionStrategieFactory strategieFactory) {
        this.cautionRepository    = cautionRepository;
        this.commissionRepository = commissionRepository;
        this.impotEtTaxeRepository = impotEtTaxeRepository;
        this.strategieFactory     = strategieFactory;
    }

    // ── Génération ────────────────────────────────────────────────────────────

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ResultatGenerationCommissionDto generer(YearMonth moisReference) {
        logger.info("Début génération commissions cautions — mois référence : {}", moisReference);

        // 1. Chargement en une seule requête (EntityGraph banque + nature + marche)
        List<Caution> cautions = cautionRepository.findActivesAvecBanqueEtNature();
        logger.debug("{} caution(s) active(s) chargée(s)", cautions.size());

        List<Commission> aCreer = new ArrayList<>();
        int ignorees = 0;

        for (Caution caution : cautions) {
            String nomBanque = caution.getBanque().getNom();

            // 2. Résolution de la stratégie par nom de banque
            Optional<CommissionCautionStrategie> strategieOpt = strategieFactory.trouver(nomBanque);
            if (strategieOpt.isEmpty()) {
                logger.warn("Aucune stratégie de commission enregistrée pour la banque '{}'"
                        + " — caution {} ignorée", nomBanque, caution.getNumeroCaution());
                ignorees++;
                continue;
            }

            // 3. Calcul de la commission (délégué à la stratégie) + liaison caution
            Commission commission = strategieOpt.get().calculer(caution, moisReference);
            commission.setCaution(caution);
            aCreer.add(commission);
        }

        // 4. Persistance des commissions en lot — les IDs sont générés après ce saveAll
        List<Commission> commissionsCreees = commissionRepository.saveAll(aCreer);

        // 5. Génération des TVA — uniquement pour les banques dont la stratégie l'autorise.
        //    CDM (et toute banque future avec genererTva()=false) est exclue :
        //    son montant de commission inclut déjà la TVA.
        List<ImpotEtTaxe> tvasACreer = new ArrayList<>();
        for (Commission commission : commissionsCreees) {
            String nomBanque = commission.getBanque().getNom();
            boolean doitGenererTva = strategieFactory.trouver(nomBanque)
                    .map(CommissionCautionStrategie::genererTva)
                    .orElse(true); // par sécurité : générer si stratégie inconnue

            if (!doitGenererTva) {
                logger.debug("TVA non générée pour la commission id={} (banque={} : montant TTC inclus)",
                        commission.getId(), nomBanque);
                continue;
            }

            BigDecimal montantTva = commission.getMontant()
                    .multiply(TAUX_TVA_COMMISSION_CAUTION)
                    .setScale(2, RoundingMode.HALF_UP);

            ImpotEtTaxe tva = new ImpotEtTaxe();
            tva.setBanque(commission.getBanque());
            tva.setMontant(montantTva);
            tva.setMotif(Motif.TVA_COMMISSION_CAUTION);
            tva.setDateEmission(commission.getDateEmission());
            tva.setLibelle("TVA " + commission.getLibelle());
            tva.setObservation("TVA/commission/caution");
            tva.setCommission(commission);
            tva.setCaution(commission.getCaution());

            tvasACreer.add(tva);
            logger.debug("TVA préparée — commission id={}, montant TVA={}", commission.getId(), montantTva);
        }

        impotEtTaxeRepository.saveAll(tvasACreer);

        logger.info("Génération terminée — {} commission(s) + {} TVA créée(s), {} ignorée(s) — mois cible : {}",
                commissionsCreees.size(), tvasACreer.size(), ignorees, moisReference.plusMonths(1));

        return new ResultatGenerationCommissionDto(commissionsCreees.size(), ignorees, moisReference);
    }
}
