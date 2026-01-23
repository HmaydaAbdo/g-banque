package staport.finance.gBanque.moyensDePaiements.virements.virementErrone.repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.moyensDePaiements.virements.virementErrone.entities.VirementErrone;

@Repository
public interface VirementErroneRepository extends JpaRepository<VirementErrone, Long>, JpaSpecificationExecutor<VirementErrone> {
}