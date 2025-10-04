package staport.finance.gBanque.droitConstate.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.util.List;

@Repository
public interface DroitConstateRepository extends JpaRepository<DroitConstate, Long>, JpaSpecificationExecutor<DroitConstate> {


    List<DroitConstate> findByMarcheAndStatus(Marche marche, Status status);
}
