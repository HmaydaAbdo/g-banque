package staport.finance.gBanque.droitConstate.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories.NatureLigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.repositories.LigneFonctionnelleSpecifiqueRepository;
import staport.finance.gBanque.droitConstate.dtos.*;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.droitConstate.mapper.DroitConstateMapper;
import staport.finance.gBanque.droitConstate.repositories.DroitConstateRepository;
import staport.finance.gBanque.droitConstate.spec.DroitConstateSpec;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.repositories.MarcheRepository;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class DroitConstateService {

    private final DroitConstateRepository droitConstateRepository;
    private  final DroitConstateMapper droitConstateMapper;
    private final MarcheRepository marcheRepository;
    private final NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository;
    private final LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository;

    public DroitConstateService(DroitConstateRepository droitConstateRepository, DroitConstateMapper droitConstateMapper, MarcheRepository marcheRepository, NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository, LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository) {
        this.droitConstateRepository = droitConstateRepository;
        this.droitConstateMapper = droitConstateMapper;
        this.marcheRepository = marcheRepository;
        this.natureLigneFonctionnelleRepository = natureLigneFonctionnelleRepository;
        this.ligneFonctionnelleSpecifiqueRepository = ligneFonctionnelleSpecifiqueRepository;
    }
    /**
     * Creates a new DroitConstate record.
     *
     * @param requestDTO The DTO containing the data for the new record.
     * @return The DTO of the newly created record.
     */
    @Transactional
    public DroitConstateResponseDto createDroitConstate(DroitConstateRequestDto requestDTO) throws Exception {


        Marche marche = marcheRepository.findById(requestDTO.getMarcheId())
                .orElseThrow(() -> new ResourceNotFoundException("Marche not found with ID: " + requestDTO.getMarcheId()));

        // Step 1: Find the "Droit constaté" nature line by its label
        NatureLigneFonctionnelle dcNature = natureLigneFonctionnelleRepository.findByLabel("Droit constaté")
                .orElseThrow(() -> new ResourceNotFoundException("Nature de ligne fonctionnelle 'Droit constaté' non trouvée."));

        // Step 2: Check for an active 'Droit constaté' specific line for the Marche
        Optional<LigneFonctionnelleSpecifique> activeDc = ligneFonctionnelleSpecifiqueRepository.findByMarcheAndNatureLigneFonctionnelleAndActifTrue(marche, dcNature);

        // Step 3: If no active line is found, throw a business validation exception
        if (activeDc.isEmpty()) {
            throw new Exception("Impossible de créer un droit constaté. Le marché '" + marche.getNom() + "' n'est pas associé à une ligne fonctionnelle 'Droit constaté' active.");
        }
        DroitConstate droitConstate = droitConstateMapper.toEntity(requestDTO, marche);
        droitConstate = droitConstateRepository.save(droitConstate);
        return droitConstateMapper.toDto(droitConstate);
    }

    /**
     * Updates an existing DroitConstate record.
     *
     * @param id The ID of the record to update.
     * @param requestDTO The DTO containing the updated data.
     * @return The DTO of the updated record.
     */
    @Transactional
    public DroitConstateResponseDto updateDroitConstate(Long id, DroitConstateRequestDto requestDTO) {

        DroitConstate existingDroitConstate = droitConstateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Droit constaté not found with ID: " + id));

        Marche marche = marcheRepository.findById(requestDTO.getMarcheId())
                .orElseThrow(() -> new ResourceNotFoundException("Marche not found with ID: " + requestDTO.getMarcheId()));

        existingDroitConstate.setMarche(marche);
        existingDroitConstate.setDateEmission(requestDTO.getDateEmission());
        existingDroitConstate.setMontant(requestDTO.getMontant());
        existingDroitConstate.setType(requestDTO.getType());
        existingDroitConstate.setObservation("Marche: " + marche.getNumero() );
        DroitConstate updatedPrefinancement = droitConstateRepository.save(existingDroitConstate);
        return droitConstateMapper.toDto(updatedPrefinancement);
    }


    /**
     * Deletes a Droit Constaté record by its ID.
     *
     * @param id The ID of the record to delete.
     */
    @Transactional
    public void deleteDroitConstate(Long id) {

        if (!droitConstateRepository.existsById(id)) {
            throw new ResourceNotFoundException("Droit constate not found with ID: " + id);
        }
        droitConstateRepository.deleteById(id);
    }

    /**
     * Finds a Droit constaté record by its ID.
     *
     * @param id The ID of the record to find.
     * @return The DTO of the found record.
     */
    @Transactional(readOnly = true)
    public DroitConstateResponseDto findById(Long id) {

        DroitConstate droitConstate = droitConstateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Droit constaté not found with ID: " + id));

        return droitConstateMapper.toDto(droitConstate);
    }

    /**
     * Searches for DroitConstaté records based on criteria.
     *
     * @param searchDTO The DTO containing the search criteria.
     * @param pageable Pagination and sorting information.
     * @return A Page of DroitConstateResponseDto matching the criteria.
     */
    @Transactional(readOnly = true)
    public Page<DroitConstateResponseDto> findAll(DroitConstateSearchDto searchDTO, Pageable pageable) {
        Specification<DroitConstate> spec = DroitConstateSpec.bySearchCriteria(searchDTO);
        Page<DroitConstate> droitConstatePage = droitConstateRepository.findAll(spec, pageable);
        return droitConstatePage.map(droitConstateMapper::toDto);
    }


    /**
     * Changes the status of a DroitConstate record.
     *
     * @param id The ID of the record.
     * @return The DTO of the updated record.
     */
    @Transactional
    public DroitConstateResponseDto markAsPaid(Long id ) {
        DroitConstate droitConstate = droitConstateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Droit constate not found with ID: " + id));

        droitConstate.setStatus(Status.PAYE);
        DroitConstate updatedDroitConstate = droitConstateRepository.save(droitConstate);
        return droitConstateMapper.toDto(updatedDroitConstate);
    }


    @Transactional(readOnly = true)
    public EncoursDroitConstateDto getRemboursementEncoursForBanque(EncoursDroitConstateSearchDto searchDto, Long banqueId, Pageable pageable) {

        // Build the specification to filter the records
        Specification<DroitConstate> spec = DroitConstateSpec.byBanqueId(banqueId)
                .and(DroitConstateSpec.outstandingRemboursementForCurrentMonth())
                .and(DroitConstateSpec.byMarcheName(searchDto.getMarcheName()))
                .and(DroitConstateSpec.byMarcheNumero(searchDto.getMarcheNumero()));

        // Fetch the paginated list of droit constate records
        Page<DroitConstate> droitConstatePage = droitConstateRepository.findAll(spec, pageable);

        // Fetch the total sum of the montant for the same criteria
        BigDecimal totalDesEncours = droitConstateRepository.findAll(spec).stream()
                .map(DroitConstate::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Map the page of entities to a page of DTOs
        Page<DroitConstateResponseDto> pageDto = droitConstatePage.map(droitConstateMapper::toDto);

        // Create and return the final DTO
        EncoursDroitConstateDto result = new EncoursDroitConstateDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        return result;
    }

    @Transactional(readOnly = true)
    public EncoursDroitConstateDto getDeblocageEncoursForBanque(EncoursDroitConstateSearchDto searchDto,Long banqueId, Pageable pageable) {

        // Build the specification to filter the records
        Specification<DroitConstate> spec = DroitConstateSpec.byBanqueId(banqueId)
                .and(DroitConstateSpec.outstandingDeblocageForCurrentMonth())
                .and(DroitConstateSpec.byMarcheName(searchDto.getMarcheName()))
                .and(DroitConstateSpec.byMarcheNumero(searchDto.getMarcheNumero()));

        // Fetch the paginated list of droit constate records
        Page<DroitConstate> droitConstatePage = droitConstateRepository.findAll(spec, pageable);

        // Fetch the total sum of the montant for the same criteria
        BigDecimal totalDesEncours = droitConstateRepository.findAll(spec).stream()
                .map(DroitConstate::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Map the page of entities to a page of DTOs
        Page<DroitConstateResponseDto> pageDto = droitConstatePage.map(droitConstateMapper::toDto);

        // Create and return the final DTO
        EncoursDroitConstateDto result = new EncoursDroitConstateDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        return result;
    }





}
