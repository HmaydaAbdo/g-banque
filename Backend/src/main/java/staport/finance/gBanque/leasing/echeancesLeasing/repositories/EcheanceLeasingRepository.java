package staport.finance.gBanque.leasing.echeancesLeasing.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    List<EcheanceLeasing> findByStatutPaiement(StatutEcheanceLeasing statut);

    long countByStatutPaiement(StatutEcheanceLeasing statut);

    List<EcheanceLeasing> findByContratLeasingId(Long contratId);

    /**
     * Nombre d'échéances PREVU dont la date tombe entre aujourd'hui et aujourd'hui+nbJours (inclus).
     * Utilisé par les métriques Gauge J-7 / J-30.
     */
    @Query("SELECT COUNT(e) FROM EcheanceLeasing e " +
           "WHERE e.statutPaiement = :statut " +
           "AND e.dateEcheance >= :debut " +
           "AND e.dateEcheance <= :fin")
    long countByStatutPaiementAndDateEcheanceBetween(
            @Param("statut") StatutEcheanceLeasing statut,
            @Param("debut") LocalDate debut,
            @Param("fin") LocalDate fin);

    /**
     * Total TTC de toutes les échéances d'un contrat (loyer + valeur résiduelle),
     * tous statuts confondus. Utilisé par le tableau d'amortissement.
     * Filtre sur l'ID du contrat uniquement (pas l'entité) — évite à EcheanceLeasingService
     * de dépendre du Repository du module contratLeasing.
     */
    @Query("SELECT COALESCE(SUM(e.montantTTC), 0) FROM EcheanceLeasing e WHERE e.contratLeasing.id = :contratId")
    BigDecimal sumMontantTTCByContratLeasingId(@Param("contratId") Long contratId);

    /**
     * Total TTC déjà payé (statut PAYE) des échéances d'un contrat.
     * Utilisé par le tableau d'amortissement.
     */
    @Query("SELECT COALESCE(SUM(e.montantTTC), 0) FROM EcheanceLeasing e " +
           "WHERE e.contratLeasing.id = :contratId AND e.statutPaiement = :statut")
    BigDecimal sumMontantTTCByContratLeasingIdAndStatutPaiement(
            @Param("contratId") Long contratId,
            @Param("statut") StatutEcheanceLeasing statut);
}