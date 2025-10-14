package staport.finance.gBanque.Agios.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.Agios.entities.Agios;

@Repository
public interface AgiosRepository extends JpaRepository<Agios, Long>, JpaSpecificationExecutor<Agios> {
}
