package staport.finance.gBanque.operationErrone.dtos;

import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;

/**
 * Critères de recherche pour l'écran de recherche des opérations erronées (endpoint {@code /search}).
 */
public record OperationErroneSearchDto(

        Long banqueId,
        String libelle,
        BigDecimal montant,
        Status status,
        TransactionSource motif

) {}
