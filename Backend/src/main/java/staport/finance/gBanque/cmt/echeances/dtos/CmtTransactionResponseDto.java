package staport.finance.gBanque.cmt.echeances.dtos;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de réponse après validation (rapprochement) d'une échéance CMT.
 * Représente la transaction bancaire créée.
 */
public record CmtTransactionResponseDto(

        Long id,
        String banqueName,
        String libelle,
        String observation,

        /** ID de l'échéance source — correspond à {@code Transaction.referent}. */
        Long echeanceId,

        LocalDate dateEmission,
        LocalDate dateValeur,
        LocalDate dateOperations,
        BigDecimal tvaDeductible,
        BigDecimal tvaCollectee,
        BigDecimal debit,
        BigDecimal credit,
        TransactionSource transactionSource

) {}
