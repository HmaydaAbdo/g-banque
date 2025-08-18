package staport.finance.gBanque.lesEtatsDesEncours.core.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.lesEtatsDesEncours.core.entities.Cheque;

@Repository
public interface ChequeRepository extends JpaRepository<Cheque, String>, JpaSpecificationExecutor<Cheque> {
}
