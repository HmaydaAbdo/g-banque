package staport.finance.gBanque.recapitulatif.services.interfaces;

import staport.finance.gBanque.recapitulatif.dtos.response.PlanTresorerieDto;
import staport.finance.gBanque.recapitulatif.enums.PeriodeRecap;

/**
 * Façade publique de la Trésorerie prévisionnelle : plan de trésorerie par banque et consolidé,
 * du solde actuel jusqu'à la fin du mois ou la fin du trimestre.
 */
public interface ITresoreriePrevisionnelleService {

    /** Plan de toutes les banques (détail par banque + vue consolidée) jusqu'à l'horizon demandé. */
    PlanTresorerieDto getPlan(PeriodeRecap periode);

    /**
     * Export PDF du plan : vue consolidée si {@code banqueId} est {@code null}, sinon la banque
     * demandée.
     *
     * @throws staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException si la
     *         banque n'existe pas
     */
    byte[] exportPdf(PeriodeRecap periode, Long banqueId);
}
