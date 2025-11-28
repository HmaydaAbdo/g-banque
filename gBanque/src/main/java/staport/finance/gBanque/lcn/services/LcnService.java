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
import staport.finance.gBanque.virements.enums.Type;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class LcnService {

    private final LcnRepository lcnRepository;

    public LcnService(LcnRepository lcnRepository) {
        this.lcnRepository = lcnRepository;

    }


    public Page<LcnResponseDTO> getDecaissements(LcnSearchDTO searchDto, Pageable pageable) {
        // Combine specifications based on non-null search criteria from the DTO
        Specification<Lcn> spec = Specification.where(LcnSpecification.hasNumero(searchDto.getNumero()))
                .and(LcnSpecification.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(LcnSpecification.hasBanqueNom(searchDto.getNomBanque()))
                .and(LcnSpecification.isActive(searchDto.getActive()))
                .and(LcnSpecification.hasType(Type.DECAISSEMENT));

        // Execute the search with the combined specification and pagination
        Page<Lcn> lcnPage = lcnRepository.findAll(spec, pageable);

        // Map the result Page<Lcn> to Page<LcnResponseDTO>
        return lcnPage.map(LcnMapper::toDto);
    }


    public Page<LcnResponseDTO> getEncaissements(LcnSearchDTO searchDto, Pageable pageable) {
        // Combine specifications based on non-null search criteria from the DTO
        Specification<Lcn> spec = Specification.where(LcnSpecification.hasNumero(searchDto.getNumero()))
                .and(LcnSpecification.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(LcnSpecification.hasBanqueNom(searchDto.getNomBanque()))
                .and(LcnSpecification.isActive(searchDto.getActive()))
                .and(LcnSpecification.hasType(Type.ENCAISSEMENT));

        // Execute the search with the combined specification and pagination
        Page<Lcn> lcnPage = lcnRepository.findAll(spec, pageable);

        // Map the result Page<Lcn> to Page<LcnResponseDTO>
        return lcnPage.map(LcnMapper::toDto);
    }


    @Transactional
    public LcnResponseDTO toggleLcnStatus(Long  id) {
        Lcn lcn = lcnRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Lcn with id " + id + " not found."));

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