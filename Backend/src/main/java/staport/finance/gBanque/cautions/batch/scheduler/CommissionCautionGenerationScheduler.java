package staport.finance.gBanque.cautions.batch.scheduler;

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
 * <p>Deux déclenchements distincts selon la banque :
 * <ul>
 *   <li><b>Fin de mois (dernier jour à 20h00)</b> — AWB, BMCI : stratégies dont
 *       {@code executerEnDebutMois() = false}</li>
 *   <li><b>Début de mois (1er jour à 03h00)</b> — CDM : stratégies dont
 *       {@code executerEnDebutMois() = true}</li>
 * </ul>
 *
 * <p>Exemple fin de mois : exécution le 30/04/2026 à 20h00 → moisReference = 2026-04
 * → AWB émet le 30/04, BMCI émet le 30/04 pour le mois M+1.
 *
 * <p>Exemple début de mois : exécution le 01/04/2026 à 03h00 → moisReference = 2026-04
 * → CDM émet le 01/04.
 */
@Component
public class CommissionCautionGenerationScheduler {

    private static final Logger log = LoggerFactory.getLogger(CommissionCautionGenerationScheduler.class);

    private final IGenerationCommissionCautionService generationService;

    public CommissionCautionGenerationScheduler(IGenerationCommissionCautionService generationService) {
        this.generationService = generationService;
    }

    /**
     * Génère les commissions de fin de mois — AWB et BMCI.
     *
     * <p>Déclenchement : dernier jour du mois à 20h00 (Africa/Casablanca).
     * <pre>
     *   0    0    20   L    *    ?
     *   sec  min  h    dom  mois dow
     * </pre>
     */
    @Scheduled(cron = "0 0 20 L * ?", zone = "Africa/Casablanca")
    public void genererCommissionsFinDeMois() {
        YearMonth moisReference = YearMonth.now();
        log.info("=== [FIN DE MOIS] Déclenchement génération commissions cautions (AWB, BMCI) — mois référence : {} ===",
                moisReference);

        try {
            ResultatGenerationCommissionDto resultat = generationService.generer(moisReference, false);

            log.info("=== [FIN DE MOIS] Génération terminée — {} commission(s) + TVA créée(s), {} ignorée(s) ===",
                    resultat.generees(), resultat.ignorees());

        } catch (Exception e) {
            log.error("[FIN DE MOIS] Erreur lors de la génération automatique des commissions cautions — mois référence : {} : {}",
                    moisReference, e.getMessage(), e);
        }
    }

    /**
     * Génère les commissions de début de mois — CDM.
     *
     * <p>Déclenchement : 1er jour du mois à 03h00 (Africa/Casablanca).
     * <pre>
     *   0    0    3    1    *    ?
     *   sec  min  h    dom  mois dow
     * </pre>
     */
    @Scheduled(cron = "0 0 3 1 * ?", zone = "Africa/Casablanca")
    public void genererCommissionsDebutDeMois() {
        YearMonth moisReference = YearMonth.now();
        log.info("=== [DÉBUT DE MOIS] Déclenchement génération commissions cautions (CDM) — mois référence : {} ===",
                moisReference);

        try {
            ResultatGenerationCommissionDto resultat = generationService.generer(moisReference, true);

            log.info("=== [DÉBUT DE MOIS] Génération terminée — {} commission(s) créée(s), {} ignorée(s) ===",
                    resultat.generees(), resultat.ignorees());

        } catch (Exception e) {
            log.error("[DÉBUT DE MOIS] Erreur lors de la génération automatique des commissions cautions — mois référence : {} : {}",
                    moisReference, e.getMessage(), e);
        }
    }
}
