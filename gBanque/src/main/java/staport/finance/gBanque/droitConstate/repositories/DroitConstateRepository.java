package staport.finance.gBanque.droitConstate.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;

@Repository
public interface DroitConstateRepository extends JpaRepository<DroitConstate, Long>, JpaSpecificationExecutor<DroitConstate> {
}
