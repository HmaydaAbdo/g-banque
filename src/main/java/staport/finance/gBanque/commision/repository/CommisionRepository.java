package staport.finance.gBanque.commision.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.commision.entities.Commission;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface CommisionRepository extends JpaRepository<Commission, Long> , JpaSpecificationExecutor<Commission> {
    List<Commission> findByLibelleLikeAndDateEmission(String libellePattern, OffsetDateTime dateEmission);
    List<Commission> findByLibelleLike(String libellePattern);

}
