package staport.finance.gBanque.factoring.dtos.request;

import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

/**
 * Critères de recherche des factorings. Tous les champs sont optionnels.
 */
public record FactoringSearchDto(
        Long marcheId,
        Long banqueId,
        Type type,
        Status status
) {
}
