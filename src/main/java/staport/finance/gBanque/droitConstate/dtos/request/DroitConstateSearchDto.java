package staport.finance.gBanque.droitConstate.dtos.request;

import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

/**
 * Critères de recherche des droits constatés. Tous les champs sont optionnels.
 */
public record DroitConstateSearchDto(
        Long marcheId,
        Long banqueId,
        Type type,
        Status status
) {
}
