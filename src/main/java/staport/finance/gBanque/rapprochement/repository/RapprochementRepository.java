package staport.finance.gBanque.rapprochement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;

import java.time.YearMonth;
import java.util.Optional;

@Repository
public interface RapprochementRepository extends JpaRepository<Rapprochement, Long>, JpaSpecificationExecutor<Rapprochement> {

    Optional<Rapprochement> findByBanqueIdAndStatut(Long banqueId, StatutRapprochement statut);

    Optional<Rapprochement> findTopByBanqueIdAndStatutOrderByDateRapprochementDesc(Long banqueId, StatutRapprochement statut);

    Optional<Rapprochement> findByBanqueIdAndDateRapprochement(Long banqueId, YearMonth dateRapprochement);

}
