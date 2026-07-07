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
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
     * Résout un Marche par ID pour les domaines qui ont besoin de l'entité JPA elle-même
     * (ex. relation ManyToMany côté {@code LigneFonctionnelle}) — jamais via
     * {@code MarcheRepository} directement (encapsulation des domaines, architecture.md).
     */
    @Transactional(readOnly = true)
    public Marche findOrThrow(Long id) {
        return marcheRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marché non trouvé avec l'ID: " + id));
    }

    /**
     * Vérifie l'existence d'un Marche par ID, pour les domaines qui n'ont besoin que d'une
     * validation (pas de l'entité elle-même).
     */
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return marcheRepository.existsById(id);
    }

    /**
     * Retourne tous les marchés rattachés à une banque, pour les domaines qui ont besoin de
     * parcourir les marchés de la banque (ex. calculateurs d'agios) — jamais via
     * {@code MarcheRepository} directement (encapsulation des domaines, architecture.md).
     */
    @Transactional(readOnly = true)
    public List<Marche> findByBanqueId(Long banqueId) {
        return marcheRepository.findByBanqueId(banqueId);
    }

    /**
     * Creates a new Marche.
     * @param requestDTO The DTO containing the data.
     * @return The response DTO of the created Marche.
     */
    public MarcheResponseDTO createMarche(MarcheRequestDTO requestDTO) {
        logger.info("Création d'un nouveau marché avec les données suivantes: {}", requestDTO);

        if (marcheRepository.existsByNumero(requestDTO.getNumero())) {
            logger.warn("Tentative de création d'un marché avec un numéro existant: {}", requestDTO.getNumero());
            throw new IllegalArgumentException("Un marche avec ce numero existe deja.");
        }

        if (requestDTO.getDateDebutDesTravaux() != null && requestDTO.getDateDAchievementDesTravaux() != null) {
            if (requestDTO.getDateDAchievementDesTravaux().isBefore(requestDTO.getDateDebutDesTravaux())) {
                logger.warn("Dates invalides pour le marché {}: date de fin avant la date de début", requestDTO.getNumero());
                throw new IllegalArgumentException("La date d'achèvement des travaux ne peut pas être avant la date de début.");
            }
        }

        Banque banque = banqueRepository.findById(requestDTO.getBanqueId())
                .orElseThrow(() -> {
                    logger.error("Banque non trouvée avec l'id {}", requestDTO.getBanqueId());
                    return new EntityNotFoundException("Banque non trouvée avec id " + requestDTO.getBanqueId());
                });

        Marche marche = marcheMapper.toEntity(requestDTO, banque);
        Marche savedMarche = marcheRepository.save(marche);
        logger.info("Marché créé avec succès avec l'id: {}", savedMarche.getId());
        return marcheMapper.toResponseDTO(savedMarche);
    }

    /**
     * Retrieves a single Marche contract by its ID.
     * @param id The ID of the Marche.
     * @return The response DTO of the found Marche.
     */
    @Transactional(readOnly = true)
    public MarcheResponseDTO getMarcheById(Long id) {
        logger.info("Récupération du marché avec l'id: {}", id);

        Marche marche = marcheRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Marché non trouvé avec l'id {}", id);
                    return new EntityNotFoundException("Marche not found with id " + id);
                });

        logger.debug("Marché trouvé: {}", marche.getNom());
        return marcheMapper.toResponseDTO(marche);
    }

    /**
     * Updates an existing Marche contract.
     * @param id The ID of the contract to update.
     * @param requestDTO The DTO containing the updated data.
     * @return The response DTO of the updated contract.
     */
    public MarcheResponseDTO updateMarche(Long id, MarcheRequestDTO requestDTO) {
        logger.info("Mise à jour du marché avec l'id {} , les nouvelles données : {}", id,requestDTO);

        Marche existingMarche = marcheRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Marché non trouvé avec l'id {}", id);
                    return new EntityNotFoundException("Marche not found with id " + id);
                });

        if (requestDTO.getDateDebutDesTravaux() != null && requestDTO.getDateDAchievementDesTravaux() != null) {
            if (requestDTO.getDateDAchievementDesTravaux().isBefore(requestDTO.getDateDebutDesTravaux())) {
                logger.warn("Dates invalides pour la mise à jour du marché {}: date de fin avant la date de début", id);
                throw new IllegalArgumentException("La date d'achèvement des travaux ne peut pas être avant la date de début.");
            }
        }

        existingMarche.setNumero(requestDTO.getNumero());
        existingMarche.setNom(requestDTO.getNom());
        existingMarche.setMontant(requestDTO.getMontant());
        existingMarche.setMaitreDouvrage(requestDTO.getMaitreDouvrage());
        existingMarche.setDateDebutDesTravaux(requestDTO.getDateDebutDesTravaux());
        existingMarche.setDateDAchievementDesTravaux(requestDTO.getDateDAchievementDesTravaux());
        existingMarche.setNanti(requestDTO.getIsNanti() != null ? requestDTO.getIsNanti() : false);

        if (requestDTO.getBanqueId() != null) {
            Banque banque = banqueRepository.findById(requestDTO.getBanqueId())
                    .orElseThrow(() -> {
                        logger.error("Banque non trouvée avec l'id {}", requestDTO.getBanqueId());
                        return new EntityNotFoundException("Banque non trouvée avec id " + requestDTO.getBanqueId());
                    });
            existingMarche.setBanque(banque);
        }

        Marche updatedMarche = marcheRepository.save(existingMarche);
        logger.info("Marché mis à jour avec succès avec l'id: {}", updatedMarche.getId());
        return marcheMapper.toResponseDTO(updatedMarche);
    }

    /**
     * Deletes a Marche contract by its ID.
     * @param id The ID of the contract to delete.
     */
    public void deleteMarche(Long id) {
        logger.info("Suppression du marché avec l'id: {}", id);

        if (!marcheRepository.existsById(id)) {
            logger.error("Tentative de suppression d'un marché inexistant avec l'id {}", id);
            throw new EntityNotFoundException("Marche not found with id " + id);
        }

        marcheRepository.deleteById(id);
        logger.info("Marché supprimé avec succès avec l'id: {}", id);
    }

    /**
     * Finds all Marche contracts with pagination, sorting, and dynamic searching.
     * @param searchDTO The DTO containing search criteria.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of Marche response DTOs.
     */
    @Transactional(readOnly = true)
    public Page<MarcheResponseDTO> findAllMarches(MarcheSearchDTO searchDTO, Pageable pageable) {
        logger.info("Recherche de marchés avec les critères: {}", searchDTO);

        Specification<Marche> spec = Specification.where(MarcheSpecification.hasNom(searchDTO.getNom()))
                .and(MarcheSpecification.hasNumero(searchDTO.getNumero()))
                .and(MarcheSpecification.hasMaitreDouvrage(searchDTO.getMaitreDouvrage()))
                .and(MarcheSpecification.hasBanqueId(searchDTO.getBanqueId()))
                .and(MarcheSpecification.isNanti(searchDTO.getIsNanti()));


        Page<Marche> marchePage = marcheRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));
        logger.debug("Trouvé {} marchés correspondant aux critères", marchePage.getTotalElements());

        return marchePage.map(marcheMapper::toResponseDTO);
    }
}