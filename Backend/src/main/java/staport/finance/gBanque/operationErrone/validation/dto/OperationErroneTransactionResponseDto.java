package staport.finance.gBanque.operationErrone.validation.dto;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de réponse pour la validation (rapprochement) d'une opération erronée.
 *
 * <p>Déplacé depuis {@code validation.dtos.operationErrone} vers le module {@code operationErrone}
 * — conforme à {@code architecture.md} Règle 1.
 *
 * <p>{@code operationErroneId} porte l'ID de l'opération erronée source — nécessaire pour
 * {@code @AuditableAction(idAccessor = "operationErroneId")}.
 */
public record OperationErroneTransactionResponseDto(

        Long id,
        Long operationErroneId,
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

) {}
