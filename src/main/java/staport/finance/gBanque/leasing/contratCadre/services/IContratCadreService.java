package staport.finance.gBanque.leasing.contratCadre.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.leasing.contratCadre.dtos.ContratCadreRequestDTO;
import staport.finance.gBanque.leasing.contratCadre.dtos.ContratCadreResponseDTO;
import staport.finance.gBanque.leasing.contratCadre.dtos.ContratCadreSearchDTO;
import staport.finance.gBanque.leasing.contratCadre.dtos.ContratCadreUpdateDTO;
import staport.finance.gBanque.leasing.contratCadre.entities.ContratCadre;

import java.util.List;

public interface IContratCadreService {

    /** Crée un nouveau contrat cadre. Lève une exception si l'organisme a déjà un contrat cadre ACTIF. */
    ContratCadreResponseDTO create(ContratCadreRequestDTO dto);

    /** Recherche paginée avec filtres optionnels (organisme, statut). */
    Page<ContratCadreResponseDTO> search(ContratCadreSearchDTO criteria, Pageable pageable);

    /** Retourne tous les contrats cadres d'un organisme, triés par date de début décroissante. */
    List<ContratCadreResponseDTO> findByOrganisme(Long organismeId);

    /** Retourne un contrat cadre par son identifiant. */
    ContratCadreResponseDTO getById(Long id);

    /** Met à jour le plafond et les dates d'un contrat cadre (organisme non modifiable). */
    ContratCadreResponseDTO update(Long id, ContratCadreUpdateDTO dto);

    /**
     * Archive le contrat cadre identifié : ACTIF → ARCHIVE.
     * Seul un contrat ACTIF peut être archivé.
     */
    ContratCadreResponseDTO archiver(Long id);

    /**
     * Résout le contrat cadre ACTIF d'un organisme.
     * Si le contrat cadre est expiré (dateFin < today), il est marqué EXPIRE et une exception est levée.
     * Utilisé par ContratLeasingService lors de la création d'un contrat leasing.
     */
    ContratCadre resolveActif(Long organismeId);
}
