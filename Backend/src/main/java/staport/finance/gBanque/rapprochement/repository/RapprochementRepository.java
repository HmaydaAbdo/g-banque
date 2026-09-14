package staport.finance.gBanque.rapprochement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
public interface RapprochementRepository extends JpaRepository<Rapprochement, Long>, JpaSpecificationExecutor<Rapprochement> {

    Optional<Rapprochement> findByBanqueIdAndStatut(Long banqueId, StatutRapprochement statut);

    Optional<Rapprochement> findTopByBanqueIdAndStatutOrderByDateRapprochementDesc(Long banqueId, StatutRapprochement statut);

    Optional<Rapprochement> findByBanqueIdAndDateRapprochement(Long banqueId, YearMonth dateRapprochement);

    List<Rapprochement> findAllByStatut(StatutRapprochement statut);

    /** Nombre de rapprochements dans un statut donné — utilisé par {@code RapprochementMetrics}. */
    long countByStatut(StatutRapprochement statut);

    /**
     * Nombre de banques distinctes ayant actuellement un rapprochement dans un statut donné.
     * Distinct de {@link #countByStatut}: sert de garde-fou de cohérence — la règle métier garantit
     * au plus un EN_COURS par banque, donc une divergence entre les deux compteurs signalerait une
     * anomalie de données. Utilisé par {@code RapprochementMetrics}.
     */
    @Query("select count(distinct r.banque.id) from rapprochement r where r.statut = :statut")
    long countDistinctBanquesByStatut(@Param("statut") StatutRapprochement statut);

}
