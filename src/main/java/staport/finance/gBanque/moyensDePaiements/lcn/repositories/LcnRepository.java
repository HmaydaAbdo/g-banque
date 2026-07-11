package staport.finance.gBanque.moyensDePaiements.lcn.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LcnRepository extends JpaRepository<Lcn, Long>, JpaSpecificationExecutor<Lcn> {

    Optional<Lcn> findByNumero(String numero);

    /** Clé de matching exclusive de la synchro FunPaiement (cf. AbstractPaymentProcessor). */
    Optional<Lcn> findByIdPaiement(String idPaiement);

    @Modifying
    @Query("UPDATE Lcn l SET l.statut = 'REGLE' WHERE l.dateEcheance < :cutoffDate AND l.statut = 'ENCOURS'")
    int deactivateByDateEcheanceBefore(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Retourne (banqueId, count) des LCN échus (ENCOURS et dateEcheance < today),
     * groupés par banque.
     */
    @Query("""
        SELECT l.banque.id, COUNT(l)
        FROM Lcn l
        WHERE l.statut = 'ENCOURS'
          AND l.dateEcheance < :today
        GROUP BY l.banque.id
    """)
    List<Object[]> countEchusGroupedByBanque(@Param("today") LocalDate today);

    /** Nombre de LCN échus pour une banque donnée — utilisé par l'agrégateur accueil banque unique. */
    @Query("""
        SELECT COUNT(l)
        FROM Lcn l
        WHERE l.banque.id = :banqueId
          AND l.statut = 'ENCOURS'
          AND l.dateEcheance < :today
    """)
    long countEchusByBanqueId(@Param("banqueId") Long banqueId, @Param("today") LocalDate today);
}
