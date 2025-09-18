package staport.finance.gBanque.marches.service;

import staport.finance.gBanque.marches.dtos.MarcheRequestDTO;
import staport.finance.gBanque.marches.dtos.MarcheResponseDTO;
import staport.finance.gBanque.marches.dtos.MarcheSearchDTO;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.mapper.MarcheMapper;
import staport.finance.gBanque.marches.repositories.MarcheRepository;
import staport.finance.gBanque.marches.specs.MarcheSpecification;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



@Service
@Transactional
public class MarcheService {

    private final MarcheRepository marcheRepository;
    private final MarcheMapper marcheMapper;

    public MarcheService(MarcheRepository marcheRepository, MarcheMapper marcheMapper) {
        this.marcheRepository = marcheRepository;
        this.marcheMapper = marcheMapper;
    }

    /**
     * Creates a new Marche.
     * @param requestDTO The DTO containing the data.
     * @return The response DTO of the created Marche.
     */
    public MarcheResponseDTO createMarche(MarcheRequestDTO requestDTO) {
        // Business logic: check for a unique numero
        if (marcheRepository.existsByNumero(requestDTO.getNumero())) {
            throw new IllegalArgumentException("Un marche avec ce numero existe deja.");
        }



        // Business logic: achievement date must be after the start date
        if (requestDTO.getDateDAchievementDesTravaux().isBefore(requestDTO.getDateDebutDesTravaux())) {
            throw new IllegalArgumentException("La date d'achevement des travaux ne peut pas etre avant la date de debut.");
        }

        Marche marche = marcheMapper.toEntity(requestDTO);
        Marche savedMarche = marcheRepository.save(marche);
        return marcheMapper.toResponseDTO(savedMarche);
    }

    /**
     * Retrieves a single Marche contract by its ID.
     * @param id The ID of the Marche.
     * @return The response DTO of the found Marche.
     */
    @Transactional(readOnly = true)
    public MarcheResponseDTO getMarcheById(Long id) {
        Marche marche = marcheRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Marche not found with id " + id));
        return marcheMapper.toResponseDTO(marche);
    }

    /**
     * Updates an existing Marche contract.
     * @param id The ID of the contract to update.
     * @param requestDTO The DTO containing the updated data.
     * @return The response DTO of the updated contract.
     */
    public MarcheResponseDTO updateMarche(Long id, MarcheRequestDTO requestDTO) {
        Marche existingMarche = marcheRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Marche not found with id " + id));

        // Business logic: achievement date must be after the start date for updates
        if (requestDTO.getDateDAchievementDesTravaux().isBefore(requestDTO.getDateDebutDesTravaux())) {
            throw new IllegalArgumentException("La date d'achevement des travaux ne peut pas etre avant la date de debut.");
        }

        // Update fields from DTO
        existingMarche.setNumero(requestDTO.getNumero());
        existingMarche.setNom(requestDTO.getNom());
        existingMarche.setMontant(requestDTO.getMontant());
        existingMarche.setMaitreDouvrage(requestDTO.getMaitreDouvrage());
        existingMarche.setDateDebutDesTravaux(requestDTO.getDateDebutDesTravaux());
        existingMarche.setDateDAchievementDesTravaux(requestDTO.getDateDAchievementDesTravaux());

        Marche updatedMarche = marcheRepository.save(existingMarche);
        return marcheMapper.toResponseDTO(updatedMarche);
    }

    /**
     * Deletes a Marche contract by its ID.
     * @param id The ID of the contract to delete.
     */
    public void deleteMarche(Long id) {
        if (!marcheRepository.existsById(id)) {
            throw new EntityNotFoundException("Marche not found with id " + id);
        }
        marcheRepository.deleteById(id);
    }

    /**
     * Finds all Marche contracts with pagination, sorting, and dynamic searching.
     * @param searchDTO The DTO containing search criteria.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of Marche response DTOs.
     */
    @Transactional(readOnly = true)
    public Page<MarcheResponseDTO> findAllMarches(MarcheSearchDTO searchDTO, Pageable pageable) {
        // Build the dynamic specification based on search criteria
        Specification<Marche> spec = Specification.where(MarcheSpecification.hasNom(searchDTO.getNom()))
                .and(MarcheSpecification.hasNumero(searchDTO.getNumero()))
                .and(MarcheSpecification.hasMaitreDouvrage(searchDTO.getMaitreDouvrage()));



        // Execute the query with the specification and pageable info
        Page<Marche> marchePage = marcheRepository.findAll(spec, pageable);

        // Map the paginated result to a paginated DTO
        return marchePage.map(marcheMapper::toResponseDTO);
    }
}