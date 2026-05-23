package staport.finance.gBanque.moyensDePaiements.virements.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VirementRepository extends JpaRepository<Virement, Long>, JpaSpecificationExecutor<Virement> {

    boolean existsByNumero(String numero);

    Optional<Virement> findByNumero(String numero);

    @Modifying
    @Query("UPDATE Virement v SET v.statut = 'REGLE' WHERE v.dateEmission < :cutoffDate AND v.statut = 'ENCOURS'")
    int deactivateByDateEmissionBefore(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Retourne (banqueId, count) des virements échus (ENCOURS et dateEmission < today),
     * groupés par banque émettrice.
     */
    @Query("""
        SELECT v.banqueEmettrice.id, COUNT(v)
        FROM Virement v
        WHERE v.statut = 'ENCOURS'
          AND v.dateEmission < :today
          AND v.banqueEmettrice IS NOT NULL
        GROUP BY v.banqueEmettrice.id
    """)
    List<Object[]> countEchusGroupedByBanque(@Param("today") LocalDate today);
}
