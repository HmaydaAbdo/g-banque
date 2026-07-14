package staport.finance.gBanque.decomptes.validation.dto;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de réponse pour la validation (rapprochement) d'un décompte.
 *
 * <p>Déplacé depuis {@code validation.dtos.decomptes} vers le module {@code decomptes} —
 * conforme à {@code architecture.md} Règle 1.
 *
 * <p>{@code decompteId} (distinct de {@code id}, qui identifie la transaction créée) porte
 * l'ID du décompte source — nécessaire pour {@code @AuditableAction(idAccessor =
 * "decompteId")} sur {@code DecompteValidationService}, l'entité auditée étant le décompte,
 * pas la transaction. Accesseur sans préfixe {@code get} car ce DTO est un record — évite
 * l'anomalie {@code idAccessor} constatée sur les 6 modules moyens de paiement (DTOs à getters).
 */
public record DecompteTransactionResponseDto(

        Long id,
        Long decompteId,
        String banqueName,
        String libelle,
        String observation,
        LocalDate dateEmission,
        LocalDate dateValeur,
        LocalDate dateOperations,
        BigDecimal debit,
        BigDecimal credit,
        TransactionSource transactionSource

) {}
