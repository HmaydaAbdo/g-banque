package staport.finance.gBanque.leasing.lesOrganismsLeasing.services.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingRequestDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingResponseDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingSearchDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.entities.OrganismeLeasing;

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

    /**
     * Exporte en PDF l'intégralité des organismes de leasing correspondant aux filtres fournis
     * (mêmes critères que {@link #search}) — sans pagination.
     */
    byte[] exportPdf(OrganismeLeasingSearchDTO criteria);

    /**
     * Retourne l'entité OrganismeLeasing ou lève ResourceNotFoundException.
     * Façade pour les domaines qui ont besoin de l'entité (ex. ContratLeasingServiceImpl)
     * sans accéder directement à OrganismeLeasingRepository (CLAUDE.md — Bounded Context).
     */
    OrganismeLeasing findOrThrow(Long id);
}