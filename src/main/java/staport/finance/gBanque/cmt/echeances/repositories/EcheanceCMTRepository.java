package staport.finance.gBanque.cmt.echeances.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository Spring Data JPA pour l'entité EcheanceCMT.
 * Gère les opérations de persistance pour les échéances de crédit.
 * Hérite de JpaSpecificationExecutor pour la recherche dynamique via Specification.
 */
@Repository
public interface EcheanceCMTRepository extends JpaRepository<EcheanceCMT, Long>, JpaSpecificationExecutor<EcheanceCMT> {

    /**
     * Récupère toutes les échéances associées à un crédit donné.
     * Cette méthode est utilisée pour valider l'absence d'échéances payées avant une régénération.
     *
     * @param credit L'entité CreditMoyenTerme.
     * @return Une liste des échéances du crédit.
     */
    List<EcheanceCMT> findByCreditMoyenTerme(CreditMoyenTerme credit);

    /**
     * Supprime toutes les échéances associées à un crédit donné.
     * Cette méthode est utilisée pour nettoyer les anciennes échéances lors d'une régénération.
     *
     * @param credit L'entité CreditMoyenTerme.
     */
    void deleteByCreditMoyenTerme(CreditMoyenTerme credit);



    Optional<EcheanceCMT> findByIdAndCreditMoyenTermeId(Long id,Long creditId);

    /**
     * Utilisé pour vérifier qu'aucune échéance n'est déjà payée, sans avoir à charger
     * l'entité {@code CreditMoyenTerme} (évite une dépendance croisée vers son Repository —
     * architecture.md, "Encapsulation des domaines").
     */
    boolean existsByCreditMoyenTermeIdAndStatutPaiement(Long creditMoyenTermeId, StatutEcheanceCMT statut);

    /**
     * Utilisé par les métriques Gauge PREVU / PAYE (metrics.md).
     */
    long countByStatutPaiement(StatutEcheanceCMT statut);

    /**
     * Utilisé par les métriques Gauge J-7 / J-30 (metrics.md).
     */
    long countByStatutPaiementAndDateEcheanceBetween(StatutEcheanceCMT statut, LocalDate from, LocalDate to);
}