package staport.finance.gBanque.recapitulatif.dtos.response;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;

/**
 * Indicateurs d'une source dans le récapitulatif : nombre d'encours, totaux débit (décaissements)
 * et crédit (encaissements), et nombre d'encours dont la date de référence est atteinte à ce jour
 * ({@code dateReference <= aujourd'hui}, même définition que {@code PeriodeRecap.A_CE_JOUR}).
 */
public record SourceRecapDto(
        TransactionSource source,
        long operationsCount,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        long echuesCount
) {
}