package staport.finance.gBanque.leasing.contratCadre.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import staport.finance.gBanque.leasing.contratCadre.entities.ContratCadre;
import staport.finance.gBanque.leasing.contratCadre.enums.StatutContratCadre;

import java.util.List;
import java.util.Optional;

public interface ContratCadreRepository extends JpaRepository<ContratCadre, Long>,
        JpaSpecificationExecutor<ContratCadre> {

    /** Retourne le contrat cadre actif d'un organisme, s'il existe. */
    Optional<ContratCadre> findByOrganisme_IdAndStatut(Long organismeId, StatutContratCadre statut);

    /** Vérifie l'existence d'un contrat cadre actif pour un organisme. */
    boolean existsByOrganisme_IdAndStatut(Long organismeId, StatutContratCadre statut);

    /** Tous les contrats cadres d'un organisme, triés par date de début décroissante. */
    List<ContratCadre> findByOrganisme_IdOrderByDateDebutDesc(Long organismeId);
}
