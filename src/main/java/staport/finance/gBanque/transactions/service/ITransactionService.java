package staport.finance.gBanque.transactions.service;

import java.math.BigDecimal;

public interface ITransactionService {

    BigDecimal computeBalance(Long banqueId);

    long countPendingTransactions(Long banqueId);
}