package staport.finance.gBanque.recapitulatif.dtos.response;

import java.time.LocalDate;

/**
 * Colonne du plan de trésorerie prévisionnel.
 *
 * <p>La première colonne ({@code code = "ECHU"}) regroupe les encours non validés dont la date
 * de référence est aujourd'hui ou avant ({@code du} est alors {@code null}). Les suivantes
 * découpent la période du lendemain jusqu'à l'horizon : par semaine (lundi-dimanche) pour la fin
 * de mois, par mois civil pour la fin de trimestre — la première et la dernière peuvent être
 * incomplètes.</p>
 */
public record ColonnePlanDto(
        String code,
        String libelle,
        LocalDate du,
        LocalDate au
) {
}
