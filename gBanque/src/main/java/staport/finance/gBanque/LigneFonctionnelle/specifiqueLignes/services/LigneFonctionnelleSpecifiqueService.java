package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories.NatureLigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.LigneFonctionnelleSpecifiqueRequest;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.LigneFonctionnelleSpecifiqueResponse;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.LigneFonctionnelleSpecifiqueSearchCriteria;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.mappers.LigneFonctionnelleSpecifiqueMapper;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.repositories.LigneFonctionnelleSpecifiqueRepository;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.specifications.LigneFonctionnelleSpecifiqueSpecification;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.repositories.MarcheRepository;

import java.time.OffsetDateTime;

@Service
public class LigneFonctionnelleSpecifiqueService {

    private static final Logger log = LoggerFactory.getLogger(LigneFonctionnelleSpecifiqueService.class);

    private final LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository;
    private final LigneFonctionnelleSpecifiqueMapper ligneFonctionnelleSpecifiqueMapper;
    private final BanqueRepository banqueRepository;
    private final NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository;
    private final MarcheRepository marcheRepository;

    public LigneFonctionnelleSpecifiqueService(
            LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository,
            LigneFonctionnelleSpecifiqueMapper ligneFonctionnelleSpecifiqueMapper,
            BanqueRepository banqueRepository,
            NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository,
            MarcheRepository marcheRepository) {
        this.ligneFonctionnelleSpecifiqueRepository = ligneFonctionnelleSpecifiqueRepository;
        this.ligneFonctionnelleSpecifiqueMapper = ligneFonctionnelleSpecifiqueMapper;
        this.banqueRepository = banqueRepository;
        this.natureLigneFonctionnelleRepository = natureLigneFonctionnelleRepository;
        this.marcheRepository = marcheRepository;
    }

    @Transactional
    public LigneFonctionnelleSpecifiqueResponse createLigneFonctionnelleSpecifique(LigneFonctionnelleSpecifiqueRequest request) {
        log.debug("Requête de création de ligne fonctionnelle spécifique: {}", request);
        validateDateRange(request.getDateDepart(), request.getDateFin());

        // Step 1: Validate and retrieve required entities
        Banque banque = banqueRepository.findById(request.getBanqueId())
                .orElseThrow(() -> new ResourceNotFoundException("Banque non trouvée avec l'ID: " + request.getBanqueId()));

        NatureLigneFonctionnelle natureLigneFonctionnelle = natureLigneFonctionnelleRepository.findById(request.getNatureLigneFonctionnelleId())
                .orElseThrow(() -> new ResourceNotFoundException("NatureLigneFonctionnelle non trouvée avec l'ID: " + request.getNatureLigneFonctionnelleId()));

        Marche marche = marcheRepository.findById(request.getMarcheId())
                .orElseThrow(() -> new ResourceNotFoundException("Marché non trouvé avec l'ID: " + request.getMarcheId()));

        // Step 2: Perform business logic validation with the refined rule
        boolean requestActif = request.getActif() == null || request.getActif();

        if (requestActif) {
            ligneFonctionnelleSpecifiqueRepository.findByMarcheAndNatureLigneFonctionnelleAndActifTrue(marche, natureLigneFonctionnelle)
                    .ifPresent(existingActiveLigne -> {
                        log.warn("Tentative de création d'un doublon actif pour la combinaison Marche/Nature : Marché '{}' (ID: {}), Nature '{}' (ID: {}).",
                                marche.getNom(), request.getMarcheId(), natureLigneFonctionnelle.getLabel(), request.getNatureLigneFonctionnelleId());
                        throw new DuplicateEntryException("Une ligne fonctionnelle ACTIVE de type '" + natureLigneFonctionnelle.getLabel() + "' existe déjà pour le marché '" + marche.getNom() + "'.");
                    });
        }

        // Step 3: Map DTO to entity and save
        LigneFonctionnelleSpecifique entity = ligneFonctionnelleSpecifiqueMapper.toEntity(request, banque, natureLigneFonctionnelle, marche);

        LigneFonctionnelleSpecifique savedEntity = ligneFonctionnelleSpecifiqueRepository.save(entity);
        log.info("Ligne fonctionnelle spécifique créée avec succès, ID: {}", savedEntity.getId());
        return ligneFonctionnelleSpecifiqueMapper.toDto(savedEntity);
    }

    @Transactional(readOnly = true)
    public LigneFonctionnelleSpecifiqueResponse getLigneFonctionnelleSpecifiqueById(Long id) {
        log.debug("Récupération de ligne fonctionnelle spécifique par ID: {}", id);
        LigneFonctionnelleSpecifique entity = ligneFonctionnelleSpecifiqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne Fonctionnelle Spécifique non trouvée avec l'ID: " + id));
        log.debug("Ligne fonctionnelle spécifique trouvée: {}", entity.getId());
        return ligneFonctionnelleSpecifiqueMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public Page<LigneFonctionnelleSpecifiqueResponse> getLignesFonctionnellesSpecifiques(
            LigneFonctionnelleSpecifiqueSearchCriteria criteria, Pageable pageable) {
        log.debug("Récupération des lignes fonctionnelles spécifiques avec critères: {} et pageable: {}", criteria, pageable);
        Specification<LigneFonctionnelleSpecifique> spec = LigneFonctionnelleSpecifiqueSpecification.searchByCriteria(criteria);
        Page<LigneFonctionnelleSpecifique> page = ligneFonctionnelleSpecifiqueRepository.findAll(spec, pageable);
        log.debug("Nombre de lignes fonctionnelles spécifiques trouvées: {}", page.getTotalElements());
        return page.map(ligneFonctionnelleSpecifiqueMapper::toDto);
    }

    @Transactional
    public LigneFonctionnelleSpecifiqueResponse updateLigneFonctionnelleSpecifique(Long id, LigneFonctionnelleSpecifiqueRequest request) {
        log.debug("--- Début de la mise à jour de LigneFonctionnelleSpécifique (ID: {}) ---", id);

        LigneFonctionnelleSpecifique existingEntity = ligneFonctionnelleSpecifiqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne Fonctionnelle Spécifique non trouvée avec l'ID: " + id));

        // Use existing entity fields for validation if request fields are null
        OffsetDateTime effectiveDateDepart = request.getDateDepart() != null ? request.getDateDepart() : existingEntity.getDateDepart();
        OffsetDateTime effectiveDateFin = request.getDateFin() != null ? request.getDateFin() : existingEntity.getDateFin();
        validateDateRange(effectiveDateDepart, effectiveDateFin);

        // Find and retrieve relationships
        Banque targetBanque = (request.getBanqueId() != null) ?
                banqueRepository.findById(request.getBanqueId()).orElseThrow(() -> new ResourceNotFoundException("Banque non trouvée avec l'ID: " + request.getBanqueId()))
                : existingEntity.getBanque();

        NatureLigneFonctionnelle targetNature = (request.getNatureLigneFonctionnelleId() != null) ?
                natureLigneFonctionnelleRepository.findById(request.getNatureLigneFonctionnelleId()).orElseThrow(() -> new ResourceNotFoundException("NatureLigneFonctionnelle non trouvée avec l'ID: " + request.getNatureLigneFonctionnelleId()))
                : existingEntity.getNatureLigneFonctionnelle();

        Marche targetMarche = (request.getMarcheId() != null) ?
                marcheRepository.findById(request.getMarcheId()).orElseThrow(() -> new ResourceNotFoundException("Marché non trouvé avec l'ID: " + request.getMarcheId()))
                : existingEntity.getMarche();

        // Check for active duplicates, excluding the current entity
        boolean requestedActif = request.getActif() != null ? request.getActif() : existingEntity.isActif();

        if (requestedActif) {
            ligneFonctionnelleSpecifiqueRepository.findByMarcheAndNatureLigneFonctionnelleAndActifTrue(targetMarche, targetNature)
                    .ifPresent(existingActiveDuplicate -> {
                        // Check if the found active duplicate is a different entity
                        if (!existingActiveDuplicate.getId().equals(id)) {
                            log.warn("Tentative de mise à jour créant un doublon actif pour la combinaison Marche/Nature : Marché '{}' (ID: {}), Nature '{}' (ID: {}). Conflit avec ID: {}",
                                    targetMarche.getNom(), targetMarche.getId(), targetNature.getLabel(), targetNature.getId(), existingActiveDuplicate.getId());
                            throw new DuplicateEntryException("Impossible de mettre à jour. Une ligne fonctionnelle ACTIVE de ce type existe déjà pour ce marché.");
                        }
                    });
        }

        // Apply partial updates only for non-null fields
        if (request.getPlafond() != null) {
            existingEntity.setPlafond(request.getPlafond());
        }
        if (request.getTaux() != null) {
            existingEntity.setTaux(request.getTaux());
        }
        if (request.getDateDepart() != null) {
            existingEntity.setDateDepart(request.getDateDepart());
        }
        if (request.getDateFin() != null) {
            existingEntity.setDateFin(request.getDateFin());
        }
        if (request.getActif() != null) {
            existingEntity.setActif(request.getActif());
        }

        // Update relationships
        existingEntity.setBanque(targetBanque);
        existingEntity.setNatureLigneFonctionnelle(targetNature);
        existingEntity.setMarche(targetMarche);

        // Save and return
        LigneFonctionnelleSpecifique updatedEntity = ligneFonctionnelleSpecifiqueRepository.save(existingEntity);
        log.info("Ligne fonctionnelle spécifique avec ID {} mise à jour avec succès.", updatedEntity.getId());
        return ligneFonctionnelleSpecifiqueMapper.toDto(updatedEntity);
    }

    /**
     * Deactivates the  LigneFonctionnelleSpecifique with the given ID.
     * Sets its 'actif' flag to false.
     * @param ligneId The ID of the functional line.
     * @throws ResourceNotFoundException if no active functional line is found for the given criteria.
     */
    @Transactional
    public void deactivateLigneFonctionnelleByNatureAndBank(Long ligneId) {
        log.debug("Tentative de désactivation de la ligne fonctionnelle spécifique  avec l ID: {} ", ligneId);
        LigneFonctionnelleSpecifique ligneToDeactivate = ligneFonctionnelleSpecifiqueRepository.findById(ligneId).orElseThrow(
                ()->new ResourceNotFoundException("Aucune Ligne Fonctionnelle spécifique avec l'ID: " + ligneId)
        );
        ligneToDeactivate.setActif(false);
        ligneFonctionnelleSpecifiqueRepository.save(ligneToDeactivate);
        log.info("Ligne fonctionnelle  spécifique avec ID {}  désactivée avec succès.", ligneToDeactivate.getId());
    }

    @Transactional
    public void deleteLigneFonctionnelleSpecifique(Long id) {
        log.debug("Requête de suppression de ligne fonctionnelle spécifique pour ID: {}", id);
        if (!ligneFonctionnelleSpecifiqueRepository.existsById(id)) {
            log.warn("Ligne Fonctionnelle Spécifique non trouvée avec l'ID: {} pour la suppression.", id);
            throw new ResourceNotFoundException("Ligne Fonctionnelle Spécifique non trouvée avec l'ID: " + id);
        }
        ligneFonctionnelleSpecifiqueRepository.deleteById(id);
        log.info("Ligne fonctionnelle spécifique avec ID {} supprimée avec succès.", id);
    }

    private void validateDateRange(OffsetDateTime dateDepart, OffsetDateTime dateFin) {
        if (dateDepart != null && dateFin != null && dateFin.isBefore(dateDepart)) {
            log.warn("Validation de date échouée: dateFin ({}) est antérieure à dateDepart ({}).", dateFin, dateDepart);
            throw new IllegalArgumentException("La date de fin (" + dateFin + ") ne peut pas être antérieure à la date de départ (" + dateDepart + ").");
        }
    }
}