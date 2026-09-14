package staport.finance.gBanque.restitution.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.restitution.entities.Restitution;

@Repository
public interface RestitutionRepository extends JpaRepository<Restitution, Long>, JpaSpecificationExecutor<Restitution> {
}