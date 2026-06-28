package staport.finance.gBanque.leasing.contratCadre.services;

import staport.finance.gBanque.leasing.contratCadre.dtos.ContratCadreRequestDTO;
import staport.finance.gBanque.leasing.contratCadre.dtos.ContratCadreResponseDTO;
import staport.finance.gBanque.leasing.contratCadre.entities.ContratCadre;

import java.util.List;

public interface IContratCadreService {

    /** Crée un nouveau contrat cadre. Lève une exception si l'organisme a déjà un contrat cadre ACTIF. */
    ContratCadreResponseDTO create(ContratCadreRequestDTO dto);

    /** Retourne tous les contrats cadres d'un organisme, triés par date de début décroissante. */
    List<ContratCadreResponseDTO> findByOrganisme(Long organismeId);

    /** Retourne un contrat cadre par son identifiant. */
    ContratCadreResponseDTO getById(Long id);

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
