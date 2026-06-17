package staport.finance.gBanque.prelevements.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.prelevements.entitie.Prelevement;

import java.time.LocalDate;

@Repository
public interface PrelevementRepository extends JpaRepository<Prelevement, Long>, JpaSpecificationExecutor<Prelevement> {

    boolean existsByBanqueIdAndLibelleAndDateEmissionBetween(
            Long banqueId, String libelle, LocalDate start, LocalDate end);
}
