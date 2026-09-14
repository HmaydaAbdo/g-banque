package staport.finance.gBanque.fraisBancaires.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.fraisBancaires.entities.FraisBancaire;

import java.time.LocalDate;

@Repository
public interface FraisBancaireRepository extends JpaRepository <FraisBancaire, Long>, JpaSpecificationExecutor<FraisBancaire> {

    /**
     * Vérifie si un frais bancaire avec le même libellé a déjà été généré pour
     * cette banque dans la fenêtre de dates donnée. Utilisé pour l'idempotence
     * du scheduler des charges fixes.
     */
    boolean existsByBanqueIdAndLibelleAndDateEmissionBetween(
            Long banqueId, String libelle, LocalDate start, LocalDate end);
}
