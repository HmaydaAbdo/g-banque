package staport.finance.gBanque.leasing.echeancesLeasing.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository pour l'entité EcheanceLeasing.
 * Fournit des opérations CRUD, de recherche et la capacité d'exécuter des Specifications.
 */
@Repository
public interface EcheanceLeasingRepository extends JpaRepository<EcheanceLeasing, Long>, JpaSpecificationExecutor<EcheanceLeasing> {

    /**
     * Trouve toutes les échéances associées à un contrat de leasing donné.
     * @param contratLeasing Le contrat de leasing dont on veut récupérer les échéances.
     * @return Une liste d'échéances pour le contrat spécifié.
     */
    List<EcheanceLeasing> findByContratLeasing(ContratLeasing contratLeasing);


    Optional<EcheanceLeasing> findByIdAndContratLeasingId(Long id, Long contratLeasingId);

    /**
     * Supprime toutes les échéances associées à un contrat de leasing donné.
     * @param contratLeasing Le contrat de leasing dont les échéances doivent être supprimées.
     */
    void deleteByContratLeasing(ContratLeasing contratLeasing);

    // méthode pour vérifier l'existence d'au moins une échéance payée.
    boolean existsByContratLeasingIdAndStatutPaiement(Long contratLeasingId, StatutEcheanceLeasing statutPaiement);
}