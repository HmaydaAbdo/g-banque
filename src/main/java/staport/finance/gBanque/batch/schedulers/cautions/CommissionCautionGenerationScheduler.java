package staport.finance.gBanque.batch.schedulers.cautions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.cautions.commissions.service.IGenerationCommissionCautionService;
import staport.finance.gBanque.cautions.commissions.service.ResultatGenerationCommissionDto;

import java.time.YearMonth;

/**
 * Scheduler de génération mensuelle des commissions sur cautions.
 *
 * <p>S'exécute automatiquement le dernier jour de chaque mois à 23h00
 * (heure de Casablanca) pour générer les commissions du mois suivant.
 *
 * <p>Exemple : exécution le 31/03/2026 à 23h00 → génère les commissions
 * avec {@code dateEmission = 30/04/2026}.
 *
 * <p>Le cron est configurable via la propriété
 * {@code commission.caution.generation.cron} dans {@code application.properties}.
 * Valeur par défaut : {@code 0 0 23 L * *} (dernier jour du mois à 23h00).
 */
@Component
public class CommissionCautionGenerationScheduler {

    private static final Logger log = LoggerFactory.getLogger(CommissionCautionGenerationScheduler.class);

    private final IGenerationCommissionCautionService generationService;

    public CommissionCautionGenerationScheduler(IGenerationCommissionCautionService generationService) {
        this.generationService = generationService;
    }

    /**
     * Génère les commissions + TVA sur cautions pour le mois suivant.
     *
     * <p>Cron : dernier jour du mois à 23h00 — après la clôture des opérations
     * de la journée, avant le changement de mois.
     *
     * <p>Expression cron (6 champs Spring) :
     * <pre>
     *   0    0    23   L    *    *
     *   sec  min  h    dom  mois dow
     * </pre>
     * {@code L} = dernier jour du mois (supporté depuis Spring Framework 5.3).
     */
    @Scheduled(cron = "0 0 23 L * *", zone = "Africa/Casablanca")
    public void genererCommissionsMensuelles() {
        YearMonth moisReference = YearMonth.now();
        log.info("=== Déclenchement génération commissions cautions — mois référence : {} ===", moisReference);

        try {
            ResultatGenerationCommissionDto resultat = generationService.generer(moisReference);

            log.info("=== Génération terminée — {} commission(s) + TVA créée(s), {} ignorée(s) — mois cible : {} ===",
                    resultat.generees(),
                    resultat.ignorees(),
                    moisReference.plusMonths(1));

        } catch (Exception e) {
            log.error("Erreur lors de la génération automatique des commissions cautions — mois référence : {} : {}",
                    moisReference, e.getMessage(), e);
            // L'exception est capturée pour ne pas interrompre le scheduler —
            // la prochaine exécution aura lieu le mois suivant normalement.
        }
    }
}
