package staport.finance.gBanque.moyensDePaiements.virementInternational.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.moyensDePaiements.virementInternational.entities.VirementInternational;

@Repository
public interface VirementInternationalRepository extends JpaRepository<VirementInternational, Long>, JpaSpecificationExecutor<VirementInternational> {

}