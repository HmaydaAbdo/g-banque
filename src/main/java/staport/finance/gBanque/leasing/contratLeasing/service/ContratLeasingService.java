package staport.finance.gBanque.leasing.contratLeasing.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingRequestDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingResponseDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingSearchDTO;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;

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

    ContratLeasingResponseDTO markAsInactive(Long id) throws ResourceNotFoundException, ValidationException;

    /**
     * Retourne l'entité ContratLeasing ou lève ResourceNotFoundException.
     * Façade pour les domaines qui ont besoin de l'entité (ex. LeasingValidationService)
     * sans accéder directement à ContratLeasingRepository (CLAUDE.md — Bounded Context).
     */
    ContratLeasing findOrThrow(Long id);
}
