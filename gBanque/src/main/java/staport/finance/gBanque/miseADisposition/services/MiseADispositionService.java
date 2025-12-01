package staport.finance.gBanque.miseADisposition.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.miseADisposition.dtos.EncoursMiseAdisposition;
import staport.finance.gBanque.miseADisposition.dtos.EncoursMiseAdispositionSearchDto;
import staport.finance.gBanque.miseADisposition.dtos.MiseADispositionResponseDto;
import staport.finance.gBanque.miseADisposition.dtos.MiseADispositionSearchDto;
import staport.finance.gBanque.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.miseADisposition.mapper.MiseADispositionMapper;
import staport.finance.gBanque.miseADisposition.repositories.MiseADispositionRepository;
import staport.finance.gBanque.miseADisposition.specs.MiseADispositionSpec;
import staport.finance.gBanque.virements.enums.Type;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

@Service
public class MiseADispositionService {

    private final MiseADispositionRepository miseADispositionRepository;

    public MiseADispositionService(MiseADispositionRepository miseADispositionRepository) {
        this.miseADispositionRepository = miseADispositionRepository;
    }


    public Page<MiseADispositionResponseDto> getDecaissementForMiseAdisposition(MiseADispositionSearchDto searchDto, Pageable pageable) {
        // Build the combined specification using the static methods from MiseADispositionSpec.
        Specification<MiseADisposition> spec = MiseADispositionSpec.bySearchDto(searchDto)
                .and(MiseADispositionSpec.hasType(Type.DECAISSEMENT));

        // Execute the search with the combined specification and pagination.
        Page<MiseADisposition> miseADispositionPage = miseADispositionRepository.findAll(spec, pageable);

        // Map the resulting Page<MiseADisposition> to Page<MiseADispositionResponseDto>.
        return miseADispositionPage.map(MiseADispositionMapper::toDto);
    }

    public Page<MiseADispositionResponseDto> getEncaissementForMiseAdisposition(MiseADispositionSearchDto searchDto, Pageable pageable) {
        // Build the combined specification using the static methods from MiseADispositionSpec.
        Specification<MiseADisposition> spec = MiseADispositionSpec.bySearchDto(searchDto)
                .and(MiseADispositionSpec.hasType(Type.ENCAISSEMENT));

        // Execute the search with the combined specification and pagination.
        Page<MiseADisposition> miseADispositionPage = miseADispositionRepository.findAll(spec, pageable);

        // Map the resulting Page<MiseADisposition> to Page<MiseADispositionResponseDto>.
        return miseADispositionPage.map(MiseADispositionMapper::toDto);
    }


    @Transactional
    public MiseADispositionResponseDto toggleMiseADispositionStatus(Long id) {
        MiseADisposition miseADisposition = miseADispositionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("MiseADisposition with id " + id + " not found."));

        // Toggle the 'active' status.
        miseADisposition.setActive(!miseADisposition.isActive());

        // Save the updated entity.
        miseADisposition = miseADispositionRepository.save(miseADisposition);

        // Map and return the updated DTO.
        return MiseADispositionMapper.toDto(miseADisposition);
    }


    /**
     * Retrieves a paginated list of active mise à disposition for a specific bank that are due in the current month.
     *
     * @param banqueId The ID of the bank to filter by.
     * @param pageable The pagination and sorting information.
     * @return A Page of MiseADispositionResponseDto for the outstanding virements.
     */
    @Transactional(readOnly = true)
    public EncoursMiseAdisposition getEncoursDecaissementsForMiseADispositionByBank(EncoursMiseAdispositionSearchDto searchDto, Long banqueId, Pageable pageable) {
        // Build the specification for active mise à disposition for the specified bank in the current month
        Specification<MiseADisposition> spec = MiseADispositionSpec.activeForBankAndUntilEndOfMonth(banqueId)
                .and(MiseADispositionSpec.hasNumero(searchDto.getNumero()))
                .and(MiseADispositionSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(MiseADispositionSpec.hasMontant(searchDto.getMontant()))
                .and(MiseADispositionSpec.hasType(Type.DECAISSEMENT));

        // Execute the search with the specification and pagination
        Page<MiseADisposition> miseADispositionPage = miseADispositionRepository.findAll(spec, pageable);
        BigDecimal totalDesEncours = miseADispositionRepository.findAll(spec).stream().map(MiseADisposition::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        Page<MiseADispositionResponseDto> result = miseADispositionPage.map(MiseADispositionMapper::toDto);
        // Map the result to DTOs and return
        return new EncoursMiseAdisposition(totalDesEncours,result);
    }

    @Transactional(readOnly = true)
    public EncoursMiseAdisposition getEncoursEncaissementsForMiseADispositionByBank(EncoursMiseAdispositionSearchDto searchDto, Long banqueId, Pageable pageable) {
        // Build the specification for active mise à disposition for the specified bank in the current month
        Specification<MiseADisposition> spec = MiseADispositionSpec.activeForBankAndUntilEndOfMonth(banqueId)
                .and(MiseADispositionSpec.hasNumero(searchDto.getNumero()))
                .and(MiseADispositionSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(MiseADispositionSpec.hasMontant(searchDto.getMontant()))
                .and(MiseADispositionSpec.hasType(Type.ENCAISSEMENT));

        // Execute the search with the specification and pagination
        Page<MiseADisposition> miseADispositionPage = miseADispositionRepository.findAll(spec, pageable);
        BigDecimal totalDesEncours = miseADispositionRepository.findAll(spec).stream().map(MiseADisposition::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        Page<MiseADispositionResponseDto> result = miseADispositionPage.map(MiseADispositionMapper::toDto);
        // Map the result to DTOs and return
        return new EncoursMiseAdisposition(totalDesEncours,result);
    }
}
