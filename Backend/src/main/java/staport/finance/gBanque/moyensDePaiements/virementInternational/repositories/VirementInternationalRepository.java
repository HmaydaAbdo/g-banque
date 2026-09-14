package staport.finance.gBanque.moyensDePaiements.virementInternational.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.moyensDePaiements.virementInternational.entities.VirementInternational;

import java.util.List;

@Repository
public interface VirementInternationalRepository extends JpaRepository<VirementInternational, Long>, JpaSpecificationExecutor<VirementInternational> {

    @Override
    @EntityGraph(attributePaths = {"banque"})
    Page<VirementInternational> findAll(Specification<VirementInternational> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"banque"})
    List<VirementInternational> findAll(Specification<VirementInternational> spec);
}