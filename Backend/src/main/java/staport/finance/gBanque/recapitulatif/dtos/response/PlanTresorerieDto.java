package staport.finance.gBanque.recapitulatif.dtos.response;

import staport.finance.gBanque.recapitulatif.enums.PeriodeRecap;

import java.time.LocalDate;
import java.util.List;

/**
 * Plan de trésorerie prévisionnel jusqu'à l'horizon {@code periode} (fin de mois ou fin de
 * trimestre) : les colonnes du plan, un total par devise et le détail par banque. Calculé à partir du
 * solde actuel de chaque banque et des encours non encore validés (vue
 * {@code v_recapitulatif_encours}).
 *
 * <p>On n'additionne jamais des devises différentes : {@code groupes} contient un total par devise
 * présente (le dirham en premier). {@code certainesSeulement} indique que le plan ne compte que les
 * opérations certaines (voir {@code CategoriesTresorerie}).</p>
 */
public record PlanTresorerieDto(
        PeriodeRecap periode,
        LocalDate dateCalcul,
        LocalDate dateHorizon,
        boolean certainesSeulement,
        List<ColonnePlanDto> colonnes,
        List<PlanBanqueDto> groupes,
        List<PlanBanqueDto> banques
) {
}
