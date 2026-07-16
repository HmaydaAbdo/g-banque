package staport.finance.gBanque.transactions.dtos;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * Agrégats (débit/crédit/TVA) et page de transactions pour une banque donnée.
 */
public record AgreratedTransactionResponseDto(
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        BigDecimal totalTvaDeductible,
        BigDecimal totalTvaCollectee,
        BigDecimal tvaDue,
        Page<TransactionResponseDto> page
) {
}
