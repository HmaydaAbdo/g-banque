package staport.finance.gBanque.recapitulatif.dtos.response;

import staport.finance.gBanque.recapitulatif.enums.PeriodeRecap;

import java.time.LocalDate;
import java.util.List;

/**
 * Plan de trésorerie prévisionnel jusqu'à l'horizon {@code periode} (fin de mois ou fin de
 * trimestre) : les colonnes du plan, la vue consolidée de toutes les banques et le détail par
 * banque. Calculé à partir du solde actuel de chaque banque et des encours non encore validés
 * (vue {@code v_recapitulatif_encours}).
 */
public record PlanTresorerieDto(
        PeriodeRecap periode,
        LocalDate dateCalcul,
        LocalDate dateHorizon,
        List<ColonnePlanDto> colonnes,
        PlanBanqueDto consolide,
        List<PlanBanqueDto> banques
) {
}
