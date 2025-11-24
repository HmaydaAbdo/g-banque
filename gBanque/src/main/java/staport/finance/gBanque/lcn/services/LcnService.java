package staport.finance.gBanque.lcn.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.lcn.dtos.EncoursLcnResponse;
import staport.finance.gBanque.lcn.dtos.EncoursLcnSearchDto;
import staport.finance.gBanque.lcn.dtos.LcnResponseDTO;
import staport.finance.gBanque.lcn.dtos.LcnSearchDTO;
import staport.finance.gBanque.lcn.entities.Lcn;
import staport.finance.gBanque.lcn.mapper.LcnMapper;
import staport.finance.gBanque.lcn.repositories.LcnRepository;
import staport.finance.gBanque.lcn.specs.LcnSpecification;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class LcnService {

    private final LcnRepository lcnRepository;

    public LcnService(LcnRepository lcnRepository) {
        this.lcnRepository = lcnRepository;

    }

    /**
     * Searches, filters, and paginates LCNs based on search criteria.
     *
     * @param searchDto The DTO containing search criteria.
     * @param pageable  The pagination and sorting information.
     * @return A Page of LcnResponseDTO.
     */
    public Page<LcnResponseDTO> searchLcns(LcnSearchDTO searchDto, Pageable pageable) {
        // Combine specifications based on non-null search criteria from the DTO
        Specification<Lcn> spec = Specification.where(LcnSpecification.hasNumero(searchDto.getNumero()))
                .and(LcnSpecification.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(LcnSpecification.hasBanqueNom(searchDto.getNomBanque()))
                .and(LcnSpecification.isActive(searchDto.getActive()));

        // Execute the search with the combined specification and pagination
        Page<Lcn> lcnPage = lcnRepository.findAll(spec, pageable);

        // Map the result Page<Lcn> to Page<LcnResponseDTO>
        return lcnPage.map(LcnMapper::toDto);
    }

    /**
     * Toggles the active status of an LCN by its numero.
     *
     * @param numero The numero of the LCN to toggle.
     * @return The updated LcnResponseDTO.
     * @throws NoSuchElementException if the LCN is not found.
     */
    @Transactional
    public LcnResponseDTO toggleLcnStatus(String numero) {
        Lcn lcn = lcnRepository.findByNumero(numero)
                .orElseThrow(() -> new NoSuchElementException("Lcn with numero " + numero + " not found."));

        // Toggle the 'active' status
        lcn.setActive(!lcn.isActive());

        // Save the updated lcn
        lcn = lcnRepository.save(lcn);

        // Map and return the updated DTO
        return LcnMapper.toDto(lcn);
    }

    /**
     * Retrieves a paginated list of active LCN for a specific bank that are due in the current month.
     *
     * @param banqueId The ID of the bank to filter by.
     * @param pageable The pagination and sorting information.
     * @return A Page of LcnResponseDTO for the outstanding LCN.
     */
    @Transactional(readOnly = true)
    public EncoursLcnResponse getEncoursForLcn(EncoursLcnSearchDto searchDto, Long banqueId, Pageable pageable) {
        // Build the specification for active LCN for the specified bank in the current month
        Specification<Lcn> spec = LcnSpecification.activeForBankAndUntilEndOfMonth(banqueId)
                .and(LcnSpecification.hasNumero(searchDto.getNumero()))
                .and(LcnSpecification.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(LcnSpecification.hasMontant(searchDto.getMontant()));

        // Execute the search with the specification and pagination
        Page<Lcn> lcnPage = lcnRepository.findAll(spec, pageable);
        BigDecimal totalDesEncours = lcnRepository.findAll(spec).stream().map(Lcn::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        Page<LcnResponseDTO> result = lcnPage.map(LcnMapper::toDto);
        // Map the result to DTOs and return
        return new EncoursLcnResponse(totalDesEncours,result);
    }
}