package staport.finance.gBanque.commision.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.commision.entities.Commission;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CommisionRepository extends JpaRepository<Commission, Long> , JpaSpecificationExecutor<Commission> {
    List<Commission> findByLibelleLikeAndDateEmission(String libellePattern, LocalDate dateEmission);
    List<Commission> findByLibelleLike(String libellePattern);

}
