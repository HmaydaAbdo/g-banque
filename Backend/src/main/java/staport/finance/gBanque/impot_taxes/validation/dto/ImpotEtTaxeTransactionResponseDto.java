package staport.finance.gBanque.impot_taxes.validation.dto;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de réponse pour la validation (rapprochement) d'un impôt/taxe.
 *
 * <p>Déplacé depuis {@code validation.dtos.impotsEtTaxes} vers le module {@code impot_taxes} —
 * conforme à {@code architecture.md} Règle 1.
 *
 * <p>{@code impotEtTaxeId} (distinct de {@code id}, qui identifie la transaction créée) porte
 * l'ID de l'impôt/taxe source — nécessaire pour {@code @AuditableAction(idAccessor =
 * "impotEtTaxeId")} sur {@code ImpotEtTaxeValidationService}, l'entité auditée étant l'impôt/taxe,
 * pas la transaction. Accesseur sans préfixe {@code get} car ce DTO est un record — évite
 * l'anomalie {@code idAccessor} constatée sur les 6 modules moyens de paiement (DTOs à getters).
 */
public record ImpotEtTaxeTransactionResponseDto(

        Long id,
        Long impotEtTaxeId,
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
