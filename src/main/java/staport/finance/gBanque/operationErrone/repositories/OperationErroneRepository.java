package staport.finance.gBanque.operationErrone.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.operationErrone.entities.OperationErrone;

@Repository
public interface OperationErroneRepository extends JpaRepository<OperationErrone, Long>, JpaSpecificationExecutor<OperationErrone> {
}
