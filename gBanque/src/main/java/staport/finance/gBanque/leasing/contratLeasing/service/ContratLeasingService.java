package staport.finance.gBanque.leasing.contratLeasing.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingRequestDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingResponseDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingSearchDTO;

/**
 * Interface pour le service de gestion des contrats de leasing.
 */
public interface ContratLeasingService {

    ContratLeasingResponseDTO createContratLeasing(ContratLeasingRequestDTO requestDTO) throws ValidationException, ResourceNotFoundException;

    ContratLeasingResponseDTO getContratLeasingById(Long id) throws ResourceNotFoundException;

    ContratLeasingResponseDTO updateContratLeasing(Long id, ContratLeasingRequestDTO requestDTO) throws ResourceNotFoundException, ValidationException;

    void deleteContratLeasing(Long id) throws ResourceNotFoundException;

    Page<ContratLeasingResponseDTO> getActiveContrats(ContratLeasingSearchDTO searchDTO, Pageable pageable);

    Page<ContratLeasingResponseDTO> getPendingLienReleaseContrats(ContratLeasingSearchDTO searchDTO, Pageable pageable);

    Page<ContratLeasingResponseDTO> getInactiveContrats(ContratLeasingSearchDTO searchDTO, Pageable pageable);

    /**
     * Marque un contrat comme inactif et terminé.
     * Cette action n'est possible que pour un contrat "en attente de main levée".
     * @param id L'ID du contrat à marquer comme inactif.
     * @return Le DTO du contrat mis à jour.
     * @throws ResourceNotFoundException Si le contrat n'existe pas.
     * @throws ValidationException Si le contrat n'est pas dans l'état "en attente de main levée".
     */
    ContratLeasingResponseDTO markAsInactive(Long id) throws ResourceNotFoundException, ValidationException;
}