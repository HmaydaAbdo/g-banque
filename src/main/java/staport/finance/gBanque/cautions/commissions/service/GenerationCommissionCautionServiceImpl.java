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
import staport.finance.gBanque.transactions.enums.TransactionSource;

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
 * <p>Idempotence :
 * <ul>
 *   <li>Avant persistance, vérification de l'existence par le triptyque
 *       {@code (CAUTION, dateEmission, libelle)} — la génération peut être relancée
 *       sans créer de doublons.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class GenerationCommissionCautionServiceImpl implements IGenerationCommissionCautionService {

    private static final Logger logger =
            LoggerFactory.getLogger(GenerationCommissionCautionServiceImpl.class);

    private final CautionRepository          cautionRepository;
    private final CommisionRepository        commissionRepository;
    private final CommissionCautionStrategieFactory strategieFactory;

    public GenerationCommissionCautionServiceImpl(
            CautionRepository cautionRepository,
            CommisionRepository commissionRepository,
            CommissionCautionStrategieFactory strategieFactory) {
        this.cautionRepository   = cautionRepository;
        this.commissionRepository = commissionRepository;
        this.strategieFactory    = strategieFactory;
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

            // 3. Calcul de la commission (délégué à la stratégie)
            Commission commission = strategieOpt.get().calculer(caution, moisReference);

            // 4. Vérification d'idempotence — on n'insère pas deux fois la même commission
            if (commissionRepository.existsByOperationAndDateEmissionAndLibelle(
                    TransactionSource.CAUTION,
                    commission.getDateEmission(),
                    commission.getLibelle())) {
                logger.warn("Commission déjà présente — caution {} / mois cible {} — ignorée",
                        caution.getNumeroCaution(), moisReference.plusMonths(1));
                ignorees++;
                continue;
            }

            aCreer.add(commission);
        }

        // 5. Persistance en lot (un seul INSERT batch)
        commissionRepository.saveAll(aCreer);

        logger.info("Génération terminée — {} commission(s) créée(s), {} ignorée(s) — mois cible : {}",
                aCreer.size(), ignorees, moisReference.plusMonths(1));

        return new ResultatGenerationCommissionDto(aCreer.size(), ignorees, moisReference);
    }
}
