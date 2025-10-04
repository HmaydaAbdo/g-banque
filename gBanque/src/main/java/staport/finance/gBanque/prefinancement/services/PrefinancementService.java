package staport.finance.gBanque.prefinancement.services;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories.NatureLigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.repositories.LigneFonctionnelleSpecifiqueRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.repositories.MarcheRepository;
import staport.finance.gBanque.prefinancement.dtos.*;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.mapper.PrefinancementMapper;
import staport.finance.gBanque.prefinancement.repository.PrefinancementRepository;
import staport.finance.gBanque.prefinancement.spec.PrefinancementSpecification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service class for handling all business logic related to Prefinancement entities.
 * This class uses the DTOs, mappers, and specifications to provide a clean and robust API.
 */
@Service
public class PrefinancementService {

    private final PrefinancementRepository prefinancementRepository;
    private final MarcheRepository marcheRepository;
    private final PrefinancementMapper prefinancementMapper;
    private final LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository;
    private final NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository;

    public PrefinancementService(
            PrefinancementRepository prefinancementRepository,
            MarcheRepository marcheRepository,
            PrefinancementMapper prefinancementMapper, LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository, NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository) {
        this.prefinancementRepository = prefinancementRepository;
        this.marcheRepository = marcheRepository;
        this.prefinancementMapper = prefinancementMapper;
        this.ligneFonctionnelleSpecifiqueRepository = ligneFonctionnelleSpecifiqueRepository;
        this.natureLigneFonctionnelleRepository = natureLigneFonctionnelleRepository;
    }

    /**
     * Creates a new Prefinancement record.
     *
     * @param requestDTO The DTO containing the data for the new record.
     * @return The DTO of the newly created record.
     */
    @Transactional
    public PrefinancementResponseDTO createPrefinancement(PrefinancementRequestDTO requestDTO) throws Exception {


        Marche marche = marcheRepository.findById(requestDTO.getMarcheId())
                .orElseThrow(() -> new ResourceNotFoundException("Marche not found with ID: " + requestDTO.getMarcheId()));

        // vérifiez si le marché a une ligne spécifique actif de nature prefinancement

        // Step 1: Find the "Préfinancement" nature line by its label
        NatureLigneFonctionnelle prefiNature = natureLigneFonctionnelleRepository.findByLabel("Préfinancement")
                .orElseThrow(() -> new ResourceNotFoundException("Nature de ligne fonctionnelle 'Préfinancement' non trouvée."));

        // Step 2: Check for an active 'Préfinancement' specific line for the Marche
        Optional<LigneFonctionnelleSpecifique> activePrefiLine = ligneFonctionnelleSpecifiqueRepository.findByMarcheAndNatureLigneFonctionnelleAndActifTrue(marche, prefiNature);

        // Step 3: If no active line is found, throw a business validation exception
        if (activePrefiLine.isEmpty()) {
            throw new Exception("Impossible de créer un préfinancement. Le marché '" + marche.getNom() + "' n'est pas associé à une ligne fonctionnelle 'Préfinancement' active.");
        }

        Prefinancement prefinancement = prefinancementMapper.toEntity(requestDTO, marche);
        prefinancement = prefinancementRepository.save(prefinancement);
        return prefinancementMapper.toDto(prefinancement);
    }

    /**
     * Updates an existing Prefinancement record.
     *
     * @param id The ID of the record to update.
     * @param requestDTO The DTO containing the updated data.
     * @return The DTO of the updated record.
     */
    @Transactional
    public PrefinancementResponseDTO updatePrefinancement(Long id, PrefinancementRequestDTO requestDTO) {
        Prefinancement existingPrefinancement = prefinancementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prefinancement not found with ID: " + id));

        Marche marche = marcheRepository.findById(requestDTO.getMarcheId())
                .orElseThrow(() -> new ResourceNotFoundException("Marche not found with ID: " + requestDTO.getMarcheId()));

        existingPrefinancement.setMarche(marche);
        existingPrefinancement.setDateEmission(requestDTO.getDateEmission());
        existingPrefinancement.setMontant(requestDTO.getMontant());
        existingPrefinancement.setType(requestDTO.getType());
        existingPrefinancement.setObservation("Marche: " + marche.getNumero() );
        Prefinancement updatedPrefinancement = prefinancementRepository.save(existingPrefinancement);
        return prefinancementMapper.toDto(updatedPrefinancement);
    }

    /**
     * Deletes a Prefinancement record by its ID.
     *
     * @param id The ID of the record to delete.
     */
    @Transactional
    public void deletePrefinancement(Long id) {

        if (!prefinancementRepository.existsById(id)) {
            throw new ResourceNotFoundException("Prefinancement not found with ID: " + id);
        }
        prefinancementRepository.deleteById(id);
    }

    /**
     * Finds a Prefinancement record by its ID.
     *
     * @param id The ID of the record to find.
     * @return The DTO of the found record.
     */
    @Transactional(readOnly = true)
    public PrefinancementResponseDTO findById(Long id) {

        Prefinancement prefinancement = prefinancementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prefinancement not found with ID: " + id));

        return prefinancementMapper.toDto(prefinancement);
    }



    /**
     * Searches for Prefinancement records based on criteria.
     *
     * @param searchDTO The DTO containing the search criteria.
     * @param pageable Pagination and sorting information.
     * @return A Page of PrefinancementResponseDTOs matching the criteria.
     */
    @Transactional(readOnly = true)
    public Page<PrefinancementResponseDTO> findAll(PrefinancementSearchDTO searchDTO, Pageable pageable) {
        Specification<Prefinancement> spec = PrefinancementSpecification.bySearchCriteria(searchDTO);
        Page<Prefinancement> prefinancementPage = prefinancementRepository.findAll(spec, pageable);
        return prefinancementPage.map(prefinancementMapper::toDto);
    }

    /**
     * Changes the status of a Prefinancement record.
     *
     * @param id The ID of the record.
     * @return The DTO of the updated record.
     */
    @Transactional
    public PrefinancementResponseDTO toggleStatus(Long id ) {
        Prefinancement prefinancement = prefinancementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prefinancement not found with ID: " + id));

        Status existingStatus = prefinancement.getStatus();
        prefinancement.setStatus(existingStatus==Status.PAYE ? Status.PREVU: Status.PAYE);
        Prefinancement updatedPrefinancement = prefinancementRepository.save(prefinancement);
        return prefinancementMapper.toDto(updatedPrefinancement);
    }

    @Transactional(readOnly = true)
    public EncoursPrefinancementDto getRemboursementEncoursForBanque(EncoursPrefinancementSearchDto searchDto, Long banqueId, Pageable pageable) {
        // Build the specification to filter the records
        Specification<Prefinancement> spec = PrefinancementSpecification.byBanqueId(banqueId)
                .and(PrefinancementSpecification.outstandingRemboursementForCurrentMonth())
                .and(PrefinancementSpecification.byMarcheName(searchDto.getMarcheName()))
                .and(PrefinancementSpecification.byMarcheNumero(searchDto.getMarcheNumero()));

        // Fetch the paginated list of prefinancement records
        Page<Prefinancement> prefinancementPage = prefinancementRepository.findAll(spec, pageable);

        // Fetch the total sum of the montant for the same criteria
        BigDecimal totalDesEncours = prefinancementRepository.findAll(spec).stream()
                .map(Prefinancement::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Map the page of entities to a page of DTOs
        Page<PrefinancementResponseDTO> pageDto = prefinancementPage.map(prefinancementMapper::toDto);

        // Create and return the final DTO
        EncoursPrefinancementDto result = new EncoursPrefinancementDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        return result;
    }

    @Transactional(readOnly = true)
    public EncoursPrefinancementDto getDeblocageEncoursForBanque(EncoursPrefinancementSearchDto searchDto,Long banqueId, Pageable pageable) {
        // Build the specification to filter the records
        Specification<Prefinancement> spec = PrefinancementSpecification.byBanqueId(banqueId)
                .and(PrefinancementSpecification.outstandingDeblocageForCurrentMonth())
                .and(PrefinancementSpecification.byMarcheName(searchDto.getMarcheName()))
                .and(PrefinancementSpecification.byMarcheNumero(searchDto.getMarcheNumero()));

        // Fetch the paginated list of prefinancement records
        Page<Prefinancement> prefinancementPage = prefinancementRepository.findAll(spec, pageable);

        // Fetch the total sum of the montant for the same criteria
        BigDecimal totalDesEncours = prefinancementRepository.findAll(spec).stream()
                .map(Prefinancement::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Map the page of entities to a page of DTOs
        Page<PrefinancementResponseDTO> pageDto = prefinancementPage.map(prefinancementMapper::toDto);

        // Create and return the final DTO
        EncoursPrefinancementDto result = new EncoursPrefinancementDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        return result;
    }
}
