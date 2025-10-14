package staport.finance.gBanque.interets.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.interets.entities.Interet;

@Repository
public interface InteretRepository extends JpaRepository<Interet, Long> , JpaSpecificationExecutor<Interet> {
}
