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
 * <p>S'exécute automatiquement le 1er jour de chaque mois à 03h00
 * (heure de Casablanca). Le mois de référence transmis au service est
 * le mois courant au moment de l'exécution.
 *
 * <p>Exemple : exécution le 01/04/2026 à 03h00 → moisReference = 2026-04.
 * Chaque stratégie banque détermine ensuite son propre mois cible :
 * <ul>
 *   <li>BMCI / CDM : M+1 ou M selon configuration</li>
 *   <li>AWB : mois de référence lui-même (M)</li>
 * </ul>
 */
@Component
public class CommissionCautionGenerationScheduler {

    private static final Logger log = LoggerFactory.getLogger(CommissionCautionGenerationScheduler.class);

    private final IGenerationCommissionCautionService generationService;

    public CommissionCautionGenerationScheduler(IGenerationCommissionCautionService generationService) {
        this.generationService = generationService;
    }

    /**
     * Génère les commissions + TVA sur cautions — exécution le 1er de chaque mois.
     *
     * <p>Expression cron (6 champs Spring) :
     * <pre>
     *   0    0    3    1    *    *
     *   sec  min  h    dom  mois dow
     * </pre>
     * Déclenchement : 1er jour du mois à 03h00 (Africa/Casablanca).
     */
    @Scheduled(cron = "0 0 3 1 * *", zone = "Africa/Casablanca")
    public void genererCommissionsMensuelles() {
        YearMonth moisReference = YearMonth.now();
        log.info("=== Déclenchement génération commissions cautions — mois référence : {} ===", moisReference);

        try {
            ResultatGenerationCommissionDto resultat = generationService.generer(moisReference);

            log.info("=== Génération terminée — {} commission(s) + TVA créée(s), {} ignorée(s) — mois référence : {} ===",
                    resultat.generees(),
                    resultat.ignorees(),
                    moisReference);

        } catch (Exception e) {
            log.error("Erreur lors de la génération automatique des commissions cautions — mois référence : {} : {}",
                    moisReference, e.getMessage(), e);
            // L'exception est capturée pour ne pas interrompre le scheduler —
            // la prochaine exécution aura lieu le mois suivant normalement.
        }
    }
}
