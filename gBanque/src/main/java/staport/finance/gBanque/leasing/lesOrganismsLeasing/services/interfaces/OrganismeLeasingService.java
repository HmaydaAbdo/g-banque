package staport.finance.gBanque.leasing.lesOrganismsLeasing.services.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingRequestDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingResponseDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingSearchDTO;

/**
 * Interface for OrganismeLeasing service operations.
 * Defines the public contract for the leasing organism management.
 */
public interface OrganismeLeasingService {
    OrganismeLeasingResponseDTO create(OrganismeLeasingRequestDTO dto);
    OrganismeLeasingResponseDTO update(Long id, OrganismeLeasingRequestDTO dto);
    OrganismeLeasingResponseDTO getById(Long id);
    void delete(Long id);
    Page<OrganismeLeasingResponseDTO> search(OrganismeLeasingSearchDTO criteria, Pageable pageable);
}