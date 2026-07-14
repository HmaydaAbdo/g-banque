package staport.finance.gBanque.impayees.validation.dto;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de réponse pour la validation (rapprochement) d'un impayé.
 *
 * <p>Déplacé depuis {@code validation.dtos.impayee} vers le module {@code impayees} — conforme à
 * {@code architecture.md} Règle 1.
 *
 * <p>{@code impayeeId} porte l'ID de l'impayé source — nécessaire pour
 * {@code @AuditableAction(idAccessor = "impayeeId")}.
 */
public record ImpayeeTransactionResponseDto(

        Long id,
        Long impayeeId,
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
