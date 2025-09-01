package staport.finance.gBanque.miseADisposition.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.miseADisposition.dtos.MiseADispositionResponseDto;
import staport.finance.gBanque.miseADisposition.dtos.MiseADispositionSearchDto;
import staport.finance.gBanque.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.miseADisposition.mapper.MiseADispositionMapper;
import staport.finance.gBanque.miseADisposition.repositories.MiseADispositionRepository;
import staport.finance.gBanque.miseADisposition.specs.MiseADispositionSpec;

import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class MiseADispositionService {

    private final MiseADispositionRepository miseADispositionRepository;

    public MiseADispositionService(MiseADispositionRepository miseADispositionRepository) {
        this.miseADispositionRepository = miseADispositionRepository;
    }

    /**
     * Searches, filters, and paginates MiseADisposition entities based on search criteria.
     * This method uses the Specification pattern to build dynamic queries.
     *
     * @param searchDto The DTO containing search criteria.
     * @param pageable The pagination and sorting information.
     * @return A Page of MiseADispositionResponseDto.
     */
    public Page<MiseADispositionResponseDto> searchMisesADisposition(MiseADispositionSearchDto searchDto, Pageable pageable) {
        // Build the combined specification using the static methods from MiseADispositionSpec.
        Specification<MiseADisposition> spec = MiseADispositionSpec.bySearchDto(searchDto);

        // Execute the search with the combined specification and pagination.
        Page<MiseADisposition> miseADispositionPage = miseADispositionRepository.findAll(spec, pageable);

        // Map the resulting Page<MiseADisposition> to Page<MiseADispositionResponseDto>.
        return miseADispositionPage.map(MiseADispositionMapper::toDto);
    }

    /**
     * Toggles the active status of a MiseADisposition by its numero.
     *
     * @param numero The numero of the MiseADisposition to toggle.
     * @return The updated MiseADispositionResponseDto.
     * @throws NoSuchElementException if the MiseADisposition is not found.
     */
    @Transactional
    public MiseADispositionResponseDto toggleMiseADispositionStatus(String numero) {
        MiseADisposition miseADisposition = miseADispositionRepository.findById(numero)
                .orElseThrow(() -> new NoSuchElementException("MiseADisposition with numero " + numero + " not found."));

        // Toggle the 'active' status.
        miseADisposition.setActive(!miseADisposition.isActive());

        // Save the updated entity.
        miseADisposition = miseADispositionRepository.save(miseADisposition);

        // Map and return the updated DTO.
        return MiseADispositionMapper.toDto(miseADisposition);
    }
}
