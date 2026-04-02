package staport.finance.gBanque.transactions.service;

import staport.finance.gBanque.transactions.dtos.TransactionResponseDto;
import staport.finance.gBanque.transactions.dtos.TransactionUpdateRequestDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ITransactionService {

    TransactionResponseDto updateTransactionById(Long id, TransactionUpdateRequestDto updateRequestDto);

    BigDecimal computeBalance(Long banqueId);

    long countPendingTransactions(Long banqueId);

    Map<Long, BigDecimal> computeBalancesForBanks(List<Long> bankIds);

}