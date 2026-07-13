package staport.finance.gBanque.prefinancement.dtos.request;

import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

/**
 * Critères de recherche des préfinancements. Tous les champs sont optionnels.
 */
public record PrefinancementSearchDTO(
        Long marcheId,
        Long banqueId,
        Type type,
        Status status
) {
}
