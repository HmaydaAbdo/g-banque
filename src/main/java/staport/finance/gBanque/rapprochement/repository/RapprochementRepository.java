package staport.finance.gBanque.rapprochement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;

@Repository
public interface RapprochementRepository extends JpaRepository<Rapprochement, Long>, JpaSpecificationExecutor<Rapprochement> {
}
