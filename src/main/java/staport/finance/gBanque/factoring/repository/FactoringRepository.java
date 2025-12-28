package staport.finance.gBanque.factoring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.util.List;

@Repository
public interface FactoringRepository extends JpaRepository<Factoring,Long> , JpaSpecificationExecutor<Factoring> {

    List<Factoring> findByMarche(Marche marche);

}
