package staport.finance.gBanque.Agios.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.Agios.entities.Agios;

import java.time.LocalDate;

@Repository
public interface AgiosRepository extends JpaRepository<Agios, Long>, JpaSpecificationExecutor<Agios> {

    boolean existsByBanqueIdAndLibelleAndDateEmissionBetween(
            Long banqueId, String libelle, LocalDate start, LocalDate end);
}
