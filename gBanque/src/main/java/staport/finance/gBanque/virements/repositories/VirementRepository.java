package staport.finance.gBanque.virements.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.virements.entities.Virement;

@Repository
public interface VirementRepository extends JpaRepository <Virement, Long> , JpaSpecificationExecutor<Virement> {
    boolean existsByNumero(String numero);
}
