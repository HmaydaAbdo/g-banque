package staport.finance.gBanque.recapitulatif.services.interfaces;

import staport.finance.gBanque.recapitulatif.dtos.response.PlanTresorerieDto;
import staport.finance.gBanque.recapitulatif.enums.PeriodeRecap;

/**
 * Façade publique de la Trésorerie prévisionnelle : plan de trésorerie par banque et consolidé,
 * du solde actuel jusqu'à la fin du mois ou la fin du trimestre.
 */
public interface ITresoreriePrevisionnelleService {

    /**
     * Plan de toutes les banques (détail par banque + un total par devise) jusqu'à l'horizon demandé.
     *
     * @param certainesSeulement vrai pour ne compter que les opérations certaines, sans les estimations
     */
    PlanTresorerieDto getPlan(PeriodeRecap periode, boolean certainesSeulement);

    /**
     * Export PDF du plan : la banque demandée si {@code banqueId} est renseigné, sinon le total de la
     * {@code devise} demandée (le dirham, ou à défaut la première devise présente, si elle est absente).
     *
     * @throws staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException si la
     *         banque ou la devise n'existe pas
     */
    byte[] exportPdf(PeriodeRecap periode, boolean certainesSeulement, Long banqueId, String devise);
}
