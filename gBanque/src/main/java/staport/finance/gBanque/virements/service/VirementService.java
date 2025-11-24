package staport.finance.gBanque.virements.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.virements.dtos.EncoursVirementResponseDto;
import staport.finance.gBanque.virements.dtos.EncoursVirementsSearchDto;
import staport.finance.gBanque.virements.dtos.VirementResponseDto;
import staport.finance.gBanque.virements.dtos.VirementSearchDto;
import staport.finance.gBanque.virements.entities.Virement;
import staport.finance.gBanque.virements.mapper.VirementMapper;
import staport.finance.gBanque.virements.repositories.VirementRepository;
import staport.finance.gBanque.virements.specs.VirementSpec;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class VirementService {

    private final VirementRepository virementRepository;
    private final BanqueRepository banqueRepository;

    public VirementService(VirementRepository virementRepository, BanqueRepository banqueRepository) {
        this.virementRepository = virementRepository;
        this.banqueRepository = banqueRepository;
    }

    /**
     * Searches, filters, and paginates Virements based on search criteria.
     *
     * @param searchDto The DTO containing search criteria.
     * @param pageable  The pagination and sorting information.
     * @return A Page of VirementResponseDto.
     */
    public Page<VirementResponseDto> searchVirements(VirementSearchDto searchDto, Pageable pageable) {
        // Build the combined specification using the static methods from VirementSpec
        Specification<Virement> spec = VirementSpec.bySearchDto(searchDto);

        // Execute the search with the combined specification and pagination
        Page<Virement> virementPage = virementRepository.findAll(spec, pageable);

        // Map the result Page<Virement> to Page<VirementResponseDto>
        return virementPage.map(VirementMapper::toDto);
    }

    /**
     * Toggles the active status of a Virement by its numero.
     *
     * @param numero The numero of the Virement to toggle.
     * @return The updated VirementResponseDto.
     * @throws NoSuchElementException if the Virement is not found.
     */
    @Transactional
    public VirementResponseDto toggleVirementStatus(String numero) {
        Virement virement = virementRepository.findById(numero)
                .orElseThrow(() -> new NoSuchElementException("Virement with numero " + numero + " not found."));

        // Toggle the 'active' status
        virement.setActive(!virement.isActive());

        // Save the updated virement
        virement = virementRepository.save(virement);

        // Map and return the updated DTO
        return VirementMapper.toDto(virement);
    }



    /**
     * Retrieves a paginated list of active virements for a specific bank that are due in the current month.
     *
     * @param banqueId The ID of the bank to filter by.
     * @param pageable The pagination and sorting information.
     * @return A Page of VirementResponseDto for the outstanding virements.
     */
    @Transactional(readOnly = true)
    public EncoursVirementResponseDto getEncoursDecaissementForVirements(EncoursVirementsSearchDto searchDto, Long banqueId, Pageable pageable) {
        // Build the specification for active virements for the specified bank in the current month
        Specification<Virement> spec = VirementSpec.encoursDecaissement(banqueId)
                .and(VirementSpec.hasNumero(searchDto.getNumero()))
                .and(VirementSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(VirementSpec.hasMontant(searchDto.getMontant()));

        // Execute the search with the specification and pagination
        Page<Virement> virementPage = virementRepository.findAll(spec, pageable);

        BigDecimal totalDesEncours = virementRepository.findAll(spec).stream().map(Virement::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        Page<VirementResponseDto> result = virementPage.map(VirementMapper::toDto);
        // Map the result to DTOs and return
        return new EncoursVirementResponseDto(totalDesEncours,result);
    }



    @Transactional(readOnly = true)
    public EncoursVirementResponseDto getEncoursEncaissementForVirements(EncoursVirementsSearchDto searchDto, Long banqueId, Pageable pageable) {

        Banque banqueBeneficiaire = banqueRepository.findById(banqueId).orElseThrow(() -> new NoSuchElementException("Bank with id " + banqueId + " not found."));

        // Build the specification for active virements for the specified bank in the current month
        Specification<Virement> spec = VirementSpec.encoursEncaissement(banqueBeneficiaire.getNom())
                .and(VirementSpec.hasNumero(searchDto.getNumero()))
                .and(VirementSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(VirementSpec.hasMontant(searchDto.getMontant()));

        // Execute the search with the specification and pagination
        Page<Virement> virementPage = virementRepository.findAll(spec, pageable);

        BigDecimal totalDesEncours = virementRepository.findAll(spec).stream().map(Virement::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        Page<VirementResponseDto> result = virementPage.map(VirementMapper::toDto);
        // Map the result to DTOs and return
        return new EncoursVirementResponseDto(totalDesEncours,result);
    }
}