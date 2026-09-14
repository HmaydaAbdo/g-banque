package staport.finance.gBanque.commision.validation.dto;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de réponse pour la validation (rapprochement) d'une commission.
 *
 * <p>Déplacé depuis {@code validation.dtos.commision} vers le module {@code commision} —
 * conforme à {@code architecture.md} Règle 1.
 *
 * <p>{@code commissionId} (distinct de {@code id}, qui identifie la transaction créée) porte
 * l'ID de la commission source — nécessaire pour {@code @AuditableAction(idAccessor =
 * "commissionId")} sur {@code CommissionValidationService}, l'entité auditée étant la commission,
 * pas la transaction. Accesseur sans préfixe {@code get} car ce DTO est un record — évite
 * l'anomalie {@code idAccessor} constatée sur les 6 modules moyens de paiement (DTOs à getters).
 */
public record CommissionTransactionResponseDto(

        Long id,
        Long commissionId,
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
