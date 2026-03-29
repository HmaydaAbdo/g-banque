package staport.finance.gBanque.transactions.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ITransactionService {

    BigDecimal computeBalance(Long banqueId);

    long countPendingTransactions(Long banqueId);

    Map<Long, BigDecimal> computeBalancesForBanks(List<Long> bankIds);
}