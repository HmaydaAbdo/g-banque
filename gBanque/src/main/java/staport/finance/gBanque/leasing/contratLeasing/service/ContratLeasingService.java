package staport.finance.gBanque.leasing.contratLeasing.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingRequestDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingResponseDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingSearchDTO;

/**
 * Interface for the ContratLeasingService.
 */
public interface ContratLeasingService {

    ContratLeasingResponseDTO createContratLeasing(ContratLeasingRequestDTO requestDTO) throws ValidationException, ResourceNotFoundException;

    ContratLeasingResponseDTO getContratLeasingById(Long id) throws ResourceNotFoundException;

    ContratLeasingResponseDTO updateContratLeasing(Long id, ContratLeasingRequestDTO requestDTO) throws ResourceNotFoundException, ValidationException;

    void deleteContratLeasing(Long id) throws ResourceNotFoundException;

    Page<ContratLeasingResponseDTO> searchContratsLeasing(ContratLeasingSearchDTO searchDTO, Pageable pageable);

    /**
     * ⭐ Nouvelle méthode pour rechercher uniquement les contrats de leasing actifs.
     * Un contrat est considéré comme actif s'il n'est pas marqué comme terminé.
     * @param searchDTO Les critères de recherche (facultatifs).
     * @param pageable Les informations de pagination.
     * @return Une Page de ContratLeasingResponseDTO pour les contrats actifs.
     */
    Page<ContratLeasingResponseDTO> searchOnActifOnly(ContratLeasingSearchDTO searchDTO, Pageable pageable);
}