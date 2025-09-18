package staport.finance.gBanque.prefinancement.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;


@Repository
public interface PrefinancementRepository extends JpaRepository <Prefinancement, Long>, JpaSpecificationExecutor<Prefinancement> {


}
