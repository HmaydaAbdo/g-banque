package staport.finance.gBanque.fraisBancaires.validation.dto;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de réponse pour la validation (rapprochement) d'un frais bancaire.
 *
 * <p>Déplacé depuis {@code validation.dtos.fraisBancaire} vers le module {@code fraisBancaires} —
 * conforme à {@code architecture.md} Règle 1.
 *
 * <p>{@code fraisBancaireId} (distinct de {@code id}, qui identifie la transaction créée) porte
 * l'ID du frais bancaire source — nécessaire pour {@code @AuditableAction(idAccessor =
 * "fraisBancaireId")} sur {@code FraisBancaireValidationService}, l'entité auditée étant le frais
 * bancaire, pas la transaction. Accesseur sans préfixe {@code get} car ce DTO est un record —
 * évite l'anomalie {@code idAccessor} constatée sur les 6 modules moyens de paiement (DTOs à
 * getters).
 */
public record FraisBancaireTransactionResponseDto(

        Long id,
        Long fraisBancaireId,
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
