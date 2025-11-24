package staport.finance.gBanque.marches.service;

import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.marches.dtos.MarcheRequestDTO;
import staport.finance.gBanque.marches.dtos.MarcheResponseDTO;
import staport.finance.gBanque.marches.dtos.MarcheSearchDTO;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.mapper.MarcheMapper;
import staport.finance.gBanque.marches.repositories.MarcheRepository;
import staport.finance.gBanque.marches.specs.MarcheSpecification;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MarcheService {

    private static final Logger logger = LoggerFactory.getLogger(MarcheService.class);

    private final MarcheRepository marcheRepository;
    private final BanqueRepository banqueRepository;
    private final MarcheMapper marcheMapper;

    public MarcheService(MarcheRepository marcheRepository, BanqueRepository banqueRepository, MarcheMapper marcheMapper) {
        this.marcheRepository = marcheRepository;
        this.banqueRepository = banqueRepository;
        this.marcheMapper = marcheMapper;
    }

    /**
     * Creates a new Marche.
     * @param requestDTO The DTO containing the data.
     * @return The response DTO of the created Marche.
     */
    public MarcheResponseDTO createMarche(MarcheRequestDTO requestDTO) {
        logger.info("Creating new Marche with numero: {}", requestDTO.getNumero());

        if (marcheRepository.existsByNumero(requestDTO.getNumero())) {
            logger.warn("Attempted to create a Marche with existing numero: {}", requestDTO.getNumero());
            throw new IllegalArgumentException("Un marche avec ce numero existe deja.");
        }

        if (requestDTO.getDateDebutDesTravaux() != null && requestDTO.getDateDAchievementDesTravaux() != null) {
            if (requestDTO.getDateDAchievementDesTravaux().isBefore(requestDTO.getDateDebutDesTravaux())) {
                logger.warn("Invalid dates for Marche {}: end date before start date", requestDTO.getNumero());
                throw new IllegalArgumentException("La date d'achèvement des travaux ne peut pas être avant la date de début.");
            }
        }

        Banque banque = banqueRepository.findById(requestDTO.getBanqueId())
                .orElseThrow(() -> {
                    logger.error("Banque not found with id {}", requestDTO.getBanqueId());
                    return new EntityNotFoundException("Banque non trouvée avec id " + requestDTO.getBanqueId());
                });

        Marche marche = marcheMapper.toEntity(requestDTO, banque);
        Marche savedMarche = marcheRepository.save(marche);
        logger.info("Successfully created Marche with id: {}", savedMarche.getId());
        return marcheMapper.toResponseDTO(savedMarche);
    }

    /**
     * Retrieves a single Marche contract by its ID.
     * @param id The ID of the Marche.
     * @return The response DTO of the found Marche.
     */
    @Transactional(readOnly = true)
    public MarcheResponseDTO getMarcheById(Long id) {
        logger.info("Fetching Marche with id: {}", id);

        Marche marche = marcheRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Marche not found with id {}", id);
                    return new EntityNotFoundException("Marche not found with id " + id);
                });

        logger.debug("Found Marche: {}", marche.getNom());
        return marcheMapper.toResponseDTO(marche);
    }

    /**
     * Updates an existing Marche contract.
     * @param id The ID of the contract to update.
     * @param requestDTO The DTO containing the updated data.
     * @return The response DTO of the updated contract.
     */
    public MarcheResponseDTO updateMarche(Long id, MarcheRequestDTO requestDTO) {
        logger.info("Updating Marche with id: {}", id);

        Marche existingMarche = marcheRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Marche not found with id {}", id);
                    return new EntityNotFoundException("Marche not found with id " + id);
                });

        if (requestDTO.getDateDebutDesTravaux() != null && requestDTO.getDateDAchievementDesTravaux() != null) {
            if (requestDTO.getDateDAchievementDesTravaux().isBefore(requestDTO.getDateDebutDesTravaux())) {
                logger.warn("Invalid dates for update of Marche {}: end date before start date", id);
                throw new IllegalArgumentException("La date d'achèvement des travaux ne peut pas être avant la date de début.");
            }
        }

        existingMarche.setNumero(requestDTO.getNumero());
        existingMarche.setNom(requestDTO.getNom());
        existingMarche.setMontant(requestDTO.getMontant());
        existingMarche.setMaitreDouvrage(requestDTO.getMaitreDouvrage());
        existingMarche.setDateDebutDesTravaux(requestDTO.getDateDebutDesTravaux());
        existingMarche.setDateDAchievementDesTravaux(requestDTO.getDateDAchievementDesTravaux());

        if (requestDTO.getBanqueId() != null) {
            Banque banque = banqueRepository.findById(requestDTO.getBanqueId())
                    .orElseThrow(() -> {
                        logger.error("Banque not found with id {}", requestDTO.getBanqueId());
                        return new EntityNotFoundException("Banque non trouvée avec id " + requestDTO.getBanqueId());
                    });
            existingMarche.setBanque(banque);
        }

        Marche updatedMarche = marcheRepository.save(existingMarche);
        logger.info("Successfully updated Marche with id: {}", updatedMarche.getId());
        return marcheMapper.toResponseDTO(updatedMarche);
    }

    /**
     * Deletes a Marche contract by its ID.
     * @param id The ID of the contract to delete.
     */
    public void deleteMarche(Long id) {
        logger.info("Deleting Marche with id: {}", id);

        if (!marcheRepository.existsById(id)) {
            logger.error("Attempted to delete non-existent Marche with id {}", id);
            throw new EntityNotFoundException("Marche not found with id " + id);
        }

        marcheRepository.deleteById(id);
        logger.info("Successfully deleted Marche with id: {}", id);
    }

    /**
     * Finds all Marche contracts with pagination, sorting, and dynamic searching.
     * @param searchDTO The DTO containing search criteria.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of Marche response DTOs.
     */
    @Transactional(readOnly = true)
    public Page<MarcheResponseDTO> findAllMarches(MarcheSearchDTO searchDTO, Pageable pageable) {
        logger.info("Searching Marches with criteria: {}", searchDTO);

        Specification<Marche> spec = Specification.where(MarcheSpecification.hasNom(searchDTO.getNom()))
                .and(MarcheSpecification.hasNumero(searchDTO.getNumero()))
                .and(MarcheSpecification.hasMaitreDouvrage(searchDTO.getMaitreDouvrage()))
                .and(MarcheSpecification.hasBanqueId(searchDTO.getBanqueId()));

        Page<Marche> marchePage = marcheRepository.findAll(spec, pageable);
        logger.debug("Found {} Marches matching criteria", marchePage.getTotalElements());

        return marchePage.map(marcheMapper::toResponseDTO);
    }
}
