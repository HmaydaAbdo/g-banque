package staport.finance.gBanque.prefinancement.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.util.List;


@Repository
public interface PrefinancementRepository extends JpaRepository <Prefinancement, Long>, JpaSpecificationExecutor<Prefinancement> {

    List<Prefinancement> findByMarche(Marche marche);

}
