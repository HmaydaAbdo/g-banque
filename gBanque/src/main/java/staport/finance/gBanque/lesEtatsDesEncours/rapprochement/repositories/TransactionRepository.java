package staport.finance.gBanque.lesEtatsDesEncours.rapprochement.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import staport.finance.gBanque.lesEtatsDesEncours.rapprochement.entities.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
}