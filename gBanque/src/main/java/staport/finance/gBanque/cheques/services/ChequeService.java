package staport.finance.gBanque.cheques.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.cheques.dtos.ChequeResponseDto;
import staport.finance.gBanque.cheques.dtos.ChequeSearchDto;
import staport.finance.gBanque.cheques.dtos.EncoursChequeResponseDto;
import staport.finance.gBanque.cheques.dtos.EncoursChequeSearchDto;
import staport.finance.gBanque.cheques.entities.Cheque;
import staport.finance.gBanque.cheques.mappers.ChequeMapper;
import staport.finance.gBanque.cheques.repositories.ChequeRepository;
import staport.finance.gBanque.cheques.specs.ChequeSpec;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class ChequeService {

    private final ChequeRepository chequeRepository;

    public ChequeService(ChequeRepository chequeRepository) {
        this.chequeRepository = chequeRepository;
    }

    /**
     * Searches, filters, and paginates cheques based on search criteria.
     *
     * @param searchDto The DTO containing search criteria.
     * @param pageable  The pagination and sorting information.
     * @return A Page of ChequeResponseDto.
     */
    public Page<ChequeResponseDto> searchCheques(ChequeSearchDto searchDto, Pageable pageable) {
        // Combine specifications based on non-null search criteria from the DTO
        Specification<Cheque> spec = Specification.where(ChequeSpec.hasNumero(searchDto.getNumero()))
                .and(ChequeSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(ChequeSpec.hasNomBanque(searchDto.getNomBanque()))
                .and(ChequeSpec.isActive(searchDto.getActive()));

        // Execute the search with the combined specification and pagination
        Page<Cheque> chequePage = chequeRepository.findAll(spec, pageable);

        // Map the result Page<Cheque> to Page<ChequeResponseDto>
        return chequePage.map(ChequeMapper::toDto);
    }

    /**
     * Toggles the active status of a cheque by its numero.
     *
     * @param numero The numero of the cheque to toggle.
     * @return The updated ChequeResponseDto.
     * @throws NoSuchElementException if the cheque is not found.
     */
    @Transactional
    public ChequeResponseDto toggleChequeStatus(String numero) {
        Cheque cheque = chequeRepository.findByNumero(numero)
                .orElseThrow(() -> new NoSuchElementException("Cheque with numero " + numero + " not found."));

        // Toggle the 'active' status
        cheque.setActive(!cheque.isActive());

        // Save the updated cheque
        cheque = chequeRepository.save(cheque);

        // Map and return the updated DTO
        return ChequeMapper.toDto(cheque);
    }


    /**
     * Retrieves a paginated list of active cheques for a specific bank that are due in the current month.
     *
     * @param banqueId The ID of the bank to filter by.
     * @param pageable The pagination and sorting information.
     * @return A Page of EncoursChequeResponseDto for the outstanding cheques.
     */
    @Transactional(readOnly = true)
    public EncoursChequeResponseDto getEncoursForCheques(EncoursChequeSearchDto searchDto, Long banqueId, Pageable pageable) {
        // Build the specification for active cheques for the specified bank in the current month
        Specification<Cheque> spec = ChequeSpec.activeForBankAndUntilEndOfMonth(banqueId)
                .and(ChequeSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(ChequeSpec.hasNumero(searchDto.getNumero()))
                .and(ChequeSpec.hasMontant(searchDto.getMontant()));

        // Execute the search with the specification and pagination
        Page<Cheque> page = chequeRepository.findAll(spec, pageable);
        Page<ChequeResponseDto> chequePage = page.map(ChequeMapper::toDto);
        BigDecimal total=chequeRepository.findAll(spec).stream().map(Cheque::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        // Map the result to DTOs and return
        return new EncoursChequeResponseDto(total,chequePage);
    }
}