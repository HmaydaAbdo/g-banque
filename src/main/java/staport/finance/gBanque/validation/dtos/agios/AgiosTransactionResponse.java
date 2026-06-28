package staport.finance.gBanque.validation.dtos.agios;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de réponse pour la validation (rapprochement) d'un agios.
 * {@code transactionSource} est toujours {@link TransactionSource#AGIOS}.
 */
public record AgiosTransactionResponse(

        Long id,
        String banqueName,
        String libelle,
        String observation,
        LocalDate dateEmission,
        LocalDate dateValeur,
        LocalDate dateOperations,
        BigDecimal tvaDeductible,
        BigDecimal tvaCollectee,
        BigDecimal debit,
        BigDecimal credit,
        TransactionSource transactionSource

) {
    /** Construit une réponse avec la source fixée à AGIOS. */
    public AgiosTransactionResponse(
            Long id,
            String banqueName,
            String libelle,
            String observation,
            LocalDate dateEmission,
            LocalDate dateValeur,
            LocalDate dateOperations,
            BigDecimal tvaDeductible,
            BigDecimal tvaCollectee,
            BigDecimal debit,
            BigDecimal credit
    ) {
        this(id, banqueName, libelle, observation,
                dateEmission, dateValeur, dateOperations,
                tvaDeductible, tvaCollectee, debit, credit,
                TransactionSource.AGIOS);
    }
}
