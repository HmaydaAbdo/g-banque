package staport.finance.gBanque.lesEtatsDesEncours.core.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.lesEtatsDesEncours.core.entities.Lcn;

@Repository
public interface LcnRepository extends JpaRepository<Lcn, String> , JpaSpecificationExecutor<Lcn> {
}
