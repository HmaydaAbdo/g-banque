package staport.finance.gBanque.provision.validation.dto;

import staport.finance.gBanque.transactions.enums.SensOperation;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de réponse pour la validation (rapprochement) d'une provision.
 *
 * <p>{@code provisionId} (distinct de {@code id}, qui identifie la transaction créée) porte l'ID
 * de la provision source — nécessaire pour {@code @AuditableAction(idAccessor = "provisionId")}
 * sur {@code ProvisionValidationService}, l'entité auditée étant la provision, pas la transaction.
 */
public record ProvisionTransactionResponseDto(

        Long id,
        Long provisionId,
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
        TransactionSource transactionSource,
        SensOperation sens

) {}
