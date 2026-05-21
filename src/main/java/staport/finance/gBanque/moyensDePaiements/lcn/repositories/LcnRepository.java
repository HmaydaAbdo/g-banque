package staport.finance.gBanque.moyensDePaiements.lcn.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface LcnRepository extends JpaRepository<Lcn, Long>, JpaSpecificationExecutor<Lcn> {

    /**
     * Finds an Lcn by its unique numero.
     * @param numero The unique identifier of the Lcn.
     * @return An Optional containing the found Lcn, or an empty Optional if none is found.
     */
    Optional<Lcn> findByNumero(String numero);

    /**
     * Règle tous les LCN dont la date d'échéance est strictement antérieure à la date donnée.
     * @param cutoffDate La date limite (exclusive)
     * @return Le nombre de LCN réglés
     */
    @Modifying
    @Query("UPDATE Lcn l SET l.statut = 'REGLE' WHERE l.dateEcheance < :cutoffDate AND l.statut = 'ENCOURS'")
    int deactivateByDateEcheanceBefore(@Param("cutoffDate") LocalDate cutoffDate);
}
