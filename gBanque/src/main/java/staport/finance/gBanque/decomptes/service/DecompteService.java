package staport.finance.gBanque.decomptes.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.decomptes.dto.*;
import staport.finance.gBanque.decomptes.entities.Decompte;
import staport.finance.gBanque.decomptes.mapper.DecompteMapper;
import staport.finance.gBanque.decomptes.repositories.DecompteRepository;
import staport.finance.gBanque.decomptes.spec.DecompteSpec;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.repositories.MarcheRepository;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;

@Service
public class DecompteService {

    private final DecompteRepository decompteRepository;
    private final DecompteMapper decompteMapper;
    private final MarcheRepository marcheRepository;
    private final BanqueRepository banqueRepository;

    public DecompteService(DecompteRepository decompteRepository, DecompteMapper decompteMapper, MarcheRepository marcheRepository, BanqueRepository banqueRepository) {
        this.decompteRepository = decompteRepository;
        this.decompteMapper = decompteMapper;
        this.marcheRepository = marcheRepository;
        this.banqueRepository = banqueRepository;
    }

    /**
     * Creates a new Décompte record.
     *
     * @param requestDTO The DTO containing the data for the new record.
     * @return The DTO of the newly created record.
     */
    @Transactional
    public DecompteResponseDto createDecompte(DecompteRequestDto requestDTO) {

        Marche marche = marcheRepository.findById(requestDTO.getMarcheId())
                .orElseThrow(() -> new ResourceNotFoundException("Marche not found with ID: " + requestDTO.getMarcheId()));

        Decompte decompte = decompteMapper.toEntity(requestDTO, marche);

        decompte = decompteRepository.save(decompte);

        return decompteMapper.toDto(decompte);
    }

    /**
     * Updates an existing Décompte record.
     *
     * @param id The ID of the record to update.
     * @param requestDTO The DTO containing the updated data.
     * @return The DTO of the updated record.
     */
    @Transactional
    public DecompteResponseDto updateDecompte(Long id, DecompteRequestDto requestDTO) {

        Decompte existingDecompte = decompteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("décompte not found with ID: " + id));

        Marche marche = marcheRepository.findById(requestDTO.getMarcheId())
                .orElseThrow(() -> new ResourceNotFoundException("Marche not found with ID: " + requestDTO.getMarcheId()));

        existingDecompte.setMarche(marche);
        existingDecompte.setDateEmission(requestDTO.getDateEmission());
        existingDecompte.setMontant(requestDTO.getMontant());
        existingDecompte.setObservation("Marche: " + marche.getNumero() );
        Decompte updatedDecompte = decompteRepository.save(existingDecompte);

        return decompteMapper.toDto(updatedDecompte);
    }


    /**
     * Deletes a Décompte record by its ID.
     *
     * @param id The ID of the record to delete.
     */
    @Transactional
    public void deleteDecompte(Long id) {

        if (!decompteRepository.existsById(id)) {
            throw new ResourceNotFoundException("décompte not found with ID: " + id);
        }
        decompteRepository.deleteById(id);
    }

    /**
     * Finds a Décompte record by its ID.
     *
     * @param id The ID of the record to find.
     * @return The DTO of the found record.
     */
    @Transactional(readOnly = true)
    public DecompteResponseDto findById(Long id) {

        Decompte decompte = decompteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Décompte not found with ID: " + id));

        return decompteMapper.toDto(decompte);
    }


    /**
     * Searches for décomptes records based on criteria.
     *
     * @param searchDTO The DTO containing the search criteria.
     * @param pageable Pagination and sorting information.
     * @return A Page of DecompteResponseDto matching the criteria.
     */
    @Transactional(readOnly = true)
    public Page<DecompteResponseDto> findAll(DecompteSearchDto searchDTO, Pageable pageable) {
        Specification<Decompte> spec = DecompteSpec.bySearchCriteria(searchDTO);
        Page<Decompte> decomptes = decompteRepository.findAll(spec, pageable);
        return decomptes.map(decompteMapper::toDto);
    }


    /**
     * Changes the status of a Décompte record.
     *
     * @param id The ID of the record.
     * @return The DTO of the updated record.
     */
    @Transactional
    public DecompteResponseDto toglleStatus(Long id ) {

        Decompte decompte = decompteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("décompte not found with ID: " + id));

        Status existingStatus = decompte.getStatus();
        decompte.setStatus(existingStatus==Status.PAYE ? Status.PREVU : Status.PAYE);
        Decompte updatedDecompte = decompteRepository.save(decompte);
        return decompteMapper.toDto(updatedDecompte);
    }


    @Transactional(readOnly = true)
    public EncoursDecompteDto getDecomptesEncoursForBanque(EncoursDecompteSearchDto searchDto, Long banqueId, Pageable pageable) {

        // Build the specification to filter the records
        Specification<Decompte> spec = DecompteSpec.byBanqueId(banqueId)
                .and(DecompteSpec.outstandingDecomptesForCurrentMonth())
                .and(DecompteSpec.byMarcheName(searchDto.getMarcheName()))
                .and(DecompteSpec.byMarcheNumero(searchDto.getMarcheNumero()));

        // Fetch the paginated list of decomptes  records
        Page<Decompte> decomptePage = decompteRepository.findAll(spec, pageable);

        // Fetch the total sum of the montant for the same criteria
        BigDecimal totalDesEncours = decompteRepository.findAll(spec).stream()
                .map(Decompte::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Map the page of entities to a page of DTOs
        Page<DecompteResponseDto> pageDto = decomptePage.map(decompteMapper::toDto);

        // Create and return the final DTO
        EncoursDecompteDto result = new EncoursDecompteDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        return result;
    }



}
