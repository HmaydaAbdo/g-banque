package staport.finance.gBanque.provision.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.provision.entities.Provision;

@Repository
public interface ProvisionRepository extends JpaRepository<Provision, Long>, JpaSpecificationExecutor<Provision> {
}
