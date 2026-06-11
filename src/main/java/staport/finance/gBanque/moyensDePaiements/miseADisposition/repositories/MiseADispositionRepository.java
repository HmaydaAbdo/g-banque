package staport.finance.gBanque.moyensDePaiements.miseADisposition.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface MiseADispositionRepository extends JpaRepository<MiseADisposition, Long>, JpaSpecificationExecutor<MiseADisposition> {

    Optional<MiseADisposition> findByNumero(String numero);

    /**
     * Recherche par numéro ET banque. Le numéro FunPaiement n'est unique que par banque
     * (annulation puis réémission sur une autre banque = même numéro).
     */
    Optional<MiseADisposition> findByNumeroAndBanqueId(String numero, Long banqueId);

    /**
     * Règle toutes les mises à disposition dont la date d'émission est strictement antérieure à la date donnée.
     * @param cutoffDate La date limite (exclusive)
     * @return Le nombre de mises à disposition réglées
     */
    @Modifying
    @Query("UPDATE MiseADisposition m SET m.statut = 'REGLE' WHERE m.dateEmission < :cutoffDate AND m.statut = 'ENCOURS'")
    int deactivateByDateEmissionBefore(@Param("cutoffDate") LocalDate cutoffDate);
}
