package staport.finance.gBanque.restitution.validation.dto;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de réponse pour la validation (rapprochement) d'une restitution.
 *
 * <p>Déplacé depuis {@code validation.dtos.restitutions} vers le module {@code restitution} —
 * conforme à {@code architecture.md} Règle 1.
 *
 * <p>{@code restitutionId} porte l'ID de la restitution source — nécessaire pour
 * {@code @AuditableAction(idAccessor = "restitutionId")}.
 */
public record RestitutionTransactionResponseDto(

        Long id,
        Long restitutionId,
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
