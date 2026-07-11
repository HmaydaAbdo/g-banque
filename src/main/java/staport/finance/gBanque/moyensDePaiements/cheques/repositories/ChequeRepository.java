package staport.finance.gBanque.moyensDePaiements.cheques.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ChequeRepository extends JpaRepository<Cheque, Long>, JpaSpecificationExecutor<Cheque> {

    /**
     * Finds a Cheque by its unique numero.
     * @param numero The unique identifier of the cheque.
     * @return An Optional containing the found Cheque, or an empty Optional if none is found.
     */
    Optional<Cheque> findByNumero(String numero);

    /** Clé de matching exclusive de la synchro FunPaiement (cf. AbstractPaymentProcessor). */
    Optional<Cheque> findByIdPaiement(String idPaiement);

    /**
     * Règle tous les chèques dont la date d'émission est strictement antérieure à la date donnée.
     * @param cutoffDate La date limite (exclusive)
     * @return Le nombre de chèques réglés
     */
    @Modifying
    @Query("UPDATE Cheque c SET c.statut = 'REGLE' WHERE c.dateEmission < :cutoffDate AND c.statut = 'ENCOURS'")
    int deactivateByDateEmissionBefore(@Param("cutoffDate") LocalDate cutoffDate);
}
