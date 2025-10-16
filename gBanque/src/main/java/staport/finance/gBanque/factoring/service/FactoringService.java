package staport.finance.gBanque.factoring.service;

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
import staport.finance.gBanque.factoring.dtos.*;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.factoring.mapper.FactoringMapper;
import staport.finance.gBanque.factoring.repository.FactoringRepository;
import staport.finance.gBanque.factoring.spec.FactoringSpec;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.repositories.MarcheRepository;
import staport.finance.gBanque.prefinancement.enums.Status;
import java.math.BigDecimal;
import java.util.Optional;

@Service
public class FactoringService {

    private final FactoringRepository factoringRepository;
    private final MarcheRepository marcheRepository;
    private final FactoringMapper factoringMapper;
    private final LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository;
    private final NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository;

    public FactoringService(FactoringRepository factoringRepository, MarcheRepository marcheRepository, FactoringMapper factoringMapper, LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository, NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository) {
        this.factoringRepository = factoringRepository;
        this.marcheRepository = marcheRepository;
        this.factoringMapper = factoringMapper;
        this.ligneFonctionnelleSpecifiqueRepository = ligneFonctionnelleSpecifiqueRepository;
        this.natureLigneFonctionnelleRepository = natureLigneFonctionnelleRepository;
    }


    /**
     * Creates a new Factoring record.
     *
     * @param requestDTO The DTO containing the data for the new record.
     * @return The DTO of the newly created record.
     */
    @Transactional
    public FactoringResponseDto createFactoring(FactoringRequestDto requestDTO) throws Exception {


        Marche marche = marcheRepository.findById(requestDTO.getMarcheId())
                .orElseThrow(() -> new ResourceNotFoundException("Marche not found with ID: " + requestDTO.getMarcheId()));

        // vérifiez si le marché a une ligne spécifique actif de nature Factoring

        // Step 1: Find the "Factoring" nature line by its label
        NatureLigneFonctionnelle factoringNature = natureLigneFonctionnelleRepository.findByLabel("Factoring")
                .orElseThrow(() -> new ResourceNotFoundException("Nature de ligne fonctionnelle 'Factoring' non trouvée."));

        // Step 2: Check for an active 'Factoring' specific line for the Marche
        Optional<LigneFonctionnelleSpecifique> activeFactoringLine = ligneFonctionnelleSpecifiqueRepository.findByMarcheAndNatureLigneFonctionnelleAndActifTrue(marche, factoringNature);

        // Step 3: If no active line is found, throw a business validation exception
        if (activeFactoringLine.isEmpty()) {
            throw new Exception("Impossible de créer un factoring. Le marché '" + marche.getNom() + "' n'est pas associé à une ligne fonctionnelle 'Factoring' active.");
        }

        Factoring factoring = factoringMapper.toEntity(requestDTO, marche);
        factoring = factoringRepository.save(factoring);
        return factoringMapper.toDto(factoring);
    }

    /**
     * Updates an existing Factoring record.
     *
     * @param id The ID of the record to update.
     * @param requestDTO The DTO containing the updated data.
     * @return The DTO of the updated record.
     */
    @Transactional
    public FactoringResponseDto updateFactoring(Long id, FactoringRequestDto requestDTO) {

        Factoring existingFactoring = factoringRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Factoring not found with ID: " + id));

        Marche marche = marcheRepository.findById(requestDTO.getMarcheId())
                .orElseThrow(() -> new ResourceNotFoundException("Marche not found with ID: " + requestDTO.getMarcheId()));

        existingFactoring.setMarche(marche);
        existingFactoring.setDateEmission(requestDTO.getDateEmission());
        existingFactoring.setMontant(requestDTO.getMontant());
        existingFactoring.setType(requestDTO.getType());
        existingFactoring.setObservation("Marche: " + marche.getNumero() );
        Factoring updatedFactoring = factoringRepository.save(existingFactoring);
        return factoringMapper.toDto(updatedFactoring);
    }

    /**
     * Deletes a Factoring record by its ID.
     *
     * @param id The ID of the record to delete.
     */
    @Transactional
    public void deleteFactoring(Long id) {

        if (!factoringRepository.existsById(id)) {
            throw new ResourceNotFoundException("Factoring not found with ID: " + id);
        }
        factoringRepository.deleteById(id);
    }

    /**
     * Finds a Factoring record by its ID.
     *
     * @param id The ID of the record to find.
     * @return The DTO of the found record.
     */
    @Transactional(readOnly = true)
    public FactoringResponseDto findById(Long id) {

        Factoring factoring = factoringRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Factoring not found with ID: " + id));

        return factoringMapper.toDto(factoring);
    }



    /**
     * Searches for Factoring records based on criteria.
     *
     * @param searchDTO The DTO containing the search criteria.
     * @param pageable Pagination and sorting information.
     * @return A Page of FactoringResponseDto matching the criteria.
     */
    @Transactional(readOnly = true)
    public Page<FactoringResponseDto> findAll(FactoringSearchDto searchDTO, Pageable pageable) {
        Specification<Factoring> spec = FactoringSpec.bySearchCriteria(searchDTO);
        Page<Factoring> factoringPage = factoringRepository.findAll(spec, pageable);
        return factoringPage.map(factoringMapper::toDto);
    }

    /**
     * Changes the status of a Factoring record.
     *
     * @param id The ID of the record.
     * @return The DTO of the updated record.
     */
    @Transactional
    public FactoringResponseDto toggleStatus(Long id ) {
        Factoring factoring = factoringRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Factoring not found with ID: " + id));

        Status existingStatus = factoring.getStatus();
        factoring.setStatus(existingStatus==Status.PAYE ? Status.PREVU: Status.PAYE);
        Factoring updatedFactoring = factoringRepository.save(factoring);
        return factoringMapper.toDto(updatedFactoring);
    }

    @Transactional(readOnly = true)
    public EncoursFactoringDto getRemboursementEncoursForBanque(EncoursFactoringSearchDto searchDto, Long banqueId, Pageable pageable) {

        // Build the specification to filter the records
        Specification<Factoring> spec = FactoringSpec.byBanqueId(banqueId)
                .and(FactoringSpec.outstandingRemboursementForCurrentMonth())
                .and(FactoringSpec.byMarcheName(searchDto.getMarcheName()))
                .and(FactoringSpec.byMarcheNumero(searchDto.getMarcheNumero()));

        // Fetch the paginated list of factoring records
        Page<Factoring> factoringPage = factoringRepository.findAll(spec, pageable);

        // Fetch the total sum of the montant for the same criteria
        BigDecimal totalDesEncours = factoringRepository.findAll(spec).stream()
                .map(Factoring::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Map the page of entities to a page of DTOs
        Page<FactoringResponseDto> pageDto = factoringPage.map(factoringMapper::toDto);

        // Create and return the final DTO
        EncoursFactoringDto result = new EncoursFactoringDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        return result;
    }

    @Transactional(readOnly = true)
    public EncoursFactoringDto getDeblocageEncoursForBanque(EncoursFactoringSearchDto searchDto,Long banqueId, Pageable pageable) {

        // Build the specification to filter the records
        Specification<Factoring> spec = FactoringSpec.byBanqueId(banqueId)
                .and(FactoringSpec.outstandingDeblocageForCurrentMonth())
                .and(FactoringSpec.byMarcheName(searchDto.getMarcheName()))
                .and(FactoringSpec.byMarcheNumero(searchDto.getMarcheNumero()));

        // Fetch the paginated list of factoring records
        Page<Factoring> factoringPage = factoringRepository.findAll(spec, pageable);

        // Fetch the total sum of the montant for the same criteria
        BigDecimal totalDesEncours = factoringRepository.findAll(spec).stream()
                .map(Factoring::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Map the page of entities to a page of DTOs
        Page<FactoringResponseDto> pageDto = factoringPage.map(factoringMapper::toDto);

        // Create and return the final DTO
        EncoursFactoringDto result = new EncoursFactoringDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        return result;
    }

}
