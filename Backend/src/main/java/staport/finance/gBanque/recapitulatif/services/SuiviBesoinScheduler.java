package staport.finance.gBanque.recapitulatif.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.recapitulatif.dtos.response.PlanTresorerieDto;
import staport.finance.gBanque.recapitulatif.enums.PeriodeRecap;
import staport.finance.gBanque.recapitulatif.services.interfaces.ITresoreriePrevisionnelleService;

import java.time.LocalDate;
import java.time.ZoneId;

/** Enregistre chaque nuit, à 03h00 (Casablanca), le besoin de financement de chaque banque pour les deux horizons. */
@Component
public class SuiviBesoinScheduler {

    private static final Logger log = LoggerFactory.getLogger(SuiviBesoinScheduler.class);
    private static final ZoneId ZONE_MAROC = ZoneId.of("Africa/Casablanca");

    private final ITresoreriePrevisionnelleService tresorerieService;
    private final SuiviBesoinService suiviBesoinService;

    public SuiviBesoinScheduler(ITresoreriePrevisionnelleService tresorerieService, SuiviBesoinService suiviBesoinService) {
        this.tresorerieService = tresorerieService;
        this.suiviBesoinService = suiviBesoinService;
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Africa/Casablanca")
    public void enregistrerBesoinQuotidien() {
        LocalDate jour = LocalDate.now(ZONE_MAROC);
        for (PeriodeRecap periode : new PeriodeRecap[]{PeriodeRecap.FIN_DE_MOIS, PeriodeRecap.FIN_DE_TRIMESTRE}) {
            PlanTresorerieDto plan = tresorerieService.getPlan(periode);
            suiviBesoinService.enregistrer(jour, periode, plan.banques());
            log.info("Suivi du besoin — {} — {} banque(s) enregistrée(s)", periode, plan.banques().size());
        }
    }
}
