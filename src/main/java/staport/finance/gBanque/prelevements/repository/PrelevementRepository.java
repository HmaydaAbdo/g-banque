package staport.finance.gBanque.prelevements.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.prelevements.entitie.Prelevement;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PrelevementRepository extends JpaRepository<Prelevement, Long>, JpaSpecificationExecutor<Prelevement> {

    boolean existsByBanqueIdAndLibelleAndDateEmissionBetween(
            Long banqueId, String libelle, LocalDate start, LocalDate end);

    @Override
    @EntityGraph(attributePaths = "banque")
    Page<Prelevement> findAll(Specification<Prelevement> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "banque")
    List<Prelevement> findAll(Specification<Prelevement> spec);
}
