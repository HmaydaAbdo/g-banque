package staport.finance.gBanque.chargesFixes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

/** Génération automatique des charges fixes le dernier jour de chaque mois à 07h00. */
@Component
public class ChargesFixesScheduler {

    private static final Logger log = LoggerFactory.getLogger(ChargesFixesScheduler.class);
    private final GenerationChargeFixeService generationService;

    public ChargesFixesScheduler(GenerationChargeFixeService generationService) {
        this.generationService = generationService;
    }

    @Scheduled(cron = "0 0 7 L * *")
    @Transactional
    public void genererChargesFixesDuMois() {
        YearMonth mois = YearMonth.now();
        log.info("[ChargesFixes] Génération du mois {} démarrée", mois);
        generationService.genererToutesChargesDuMois(mois);
        log.info("[ChargesFixes] Génération terminée pour {}", mois);
    }
}
