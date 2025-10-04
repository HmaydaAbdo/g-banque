package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories.NatureLigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.*;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.mappers.LigneFonctionnelleSpecifiqueMapper;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.repositories.LigneFonctionnelleSpecifiqueRepository;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.specifications.LigneFonctionnelleSpecifiqueSpecification;
import staport.finance.gBanque.droitConstate.dtos.AgregatedSuivieDcLigneResponseDto;
import staport.finance.gBanque.droitConstate.dtos.SuivieDcLigneResponseDto;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.droitConstate.mapper.DroitConstateMapper;
import staport.finance.gBanque.droitConstate.repositories.DroitConstateRepository;
import staport.finance.gBanque.droitConstate.spec.DroitConstateSpec;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.repositories.MarcheRepository;
import staport.finance.gBanque.prefinancement.dtos.AgregatedSuiviePrefiLigneResponseDto;
import staport.finance.gBanque.prefinancement.dtos.SuiviePrefiLigneResponseDto;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;
import staport.finance.gBanque.prefinancement.mapper.PrefinancementMapper;
import staport.finance.gBanque.prefinancement.repository.PrefinancementRepository;
import staport.finance.gBanque.prefinancement.spec.PrefinancementSpecification;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class LigneFonctionnelleSpecifiqueService {

    private static final Logger log = LoggerFactory.getLogger(LigneFonctionnelleSpecifiqueService.class);

    private final LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository;
    private final LigneFonctionnelleSpecifiqueMapper ligneFonctionnelleSpecifiqueMapper;
    private final BanqueRepository banqueRepository;
    private final NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository;
    private final MarcheRepository marcheRepository;

    private final PrefinancementRepository prefinancementRepository;
    private final PrefinancementMapper prefinancementMapper;

    private final DroitConstateRepository droitConstateRepository;
    private final DroitConstateMapper droitConstateMapper;


    public LigneFonctionnelleSpecifiqueService(
            LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository,
            LigneFonctionnelleSpecifiqueMapper ligneFonctionnelleSpecifiqueMapper,
            BanqueRepository banqueRepository,
            NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository,
            MarcheRepository marcheRepository, PrefinancementRepository prefinancementRepository, PrefinancementMapper prefinancementMapper, DroitConstateRepository droitConstateRepository, DroitConstateMapper droitConstateMapper) {
        this.ligneFonctionnelleSpecifiqueRepository = ligneFonctionnelleSpecifiqueRepository;
        this.ligneFonctionnelleSpecifiqueMapper = ligneFonctionnelleSpecifiqueMapper;
        this.banqueRepository = banqueRepository;
        this.natureLigneFonctionnelleRepository = natureLigneFonctionnelleRepository;
        this.marcheRepository = marcheRepository;
        this.prefinancementRepository = prefinancementRepository;
        this.prefinancementMapper = prefinancementMapper;
        this.droitConstateRepository = droitConstateRepository;
        this.droitConstateMapper = droitConstateMapper;
    }

    @Transactional
    public LigneFonctionnelleSpecifiqueResponse createLigneFonctionnelleSpecifique(LigneFonctionnelleSpecifiqueRequest request) {

        log.debug("Requête de création de ligne fonctionnelle spécifique: {}", request);

        validateDateRange(request.getDateDepart(), request.getDateFin());


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
        LigneFonctionnelleSpecifique entity = ligneFonctionnelleSpecifiqueMapper.toEntity(request, natureLigneFonctionnelle, marche);

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

    @Transactional(readOnly = true)
    public Page<LigneFonctionnelleSpecifiqueResponse> getLignesFonctionnellesSpecifiquesByBanqueId(
            Long banqueId, LigneFonctionnelleSpecifiqueSearchCriteria criteria, Pageable pageable) {

        log.debug("Récupération paginée et filtrée des lignes fonctionnelles spécifiques pour Banque ID: {} avec critères: {} et pageable: {}", banqueId, criteria, pageable);

        // 1. Validate that the bank exists
        if (!banqueRepository.existsById(banqueId)) {
            log.warn("Banque non trouvée avec l'ID: {}", banqueId);
            throw new ResourceNotFoundException("Banque non trouvée avec l'ID: " + banqueId + ". Impossible de récupérer les lignes fonctionnelles spécifiques.");
        }

        // 2. Set the banqueId in the criteria to use the existing specification logic
        criteria.setBanqueId(banqueId);

        // 3. Use the existing Specification to find the entities
        Specification<LigneFonctionnelleSpecifique> spec = LigneFonctionnelleSpecifiqueSpecification.searchByCriteria(criteria);
        Page<LigneFonctionnelleSpecifique> page = ligneFonctionnelleSpecifiqueRepository.findAll(spec, pageable);

        log.debug("Nombre de lignes fonctionnelles spécifiques trouvées pour la banque {} avec filtres: {}", banqueId, page.getTotalElements());

        // 4. Map the entities to DTOs and return
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
     *
     * @param ligneId The ID of the functional line.
     * @throws ResourceNotFoundException if no active functional line is found for the given criteria.
     */
    @Transactional
    public void deactivateLigneFonctionnelleById(Long ligneId) {
        log.debug("Tentative de désactivation de la ligne fonctionnelle spécifique  avec l ID: {} ", ligneId);
        LigneFonctionnelleSpecifique ligneToDeactivate = ligneFonctionnelleSpecifiqueRepository.findById(ligneId).orElseThrow(
                () -> new ResourceNotFoundException("Aucune Ligne Fonctionnelle spécifique avec l'ID: " + ligneId)
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


    /**
     * Retrieves a page of SpecificLigneResposneDto with pagination, sorting, and search support.
     *
     * @param searchCriteria The DTO containing search criteria.
     * @param pageable       The pagination and sorting information.
     * @return A Page of SpecificLigneResposneDto DTOs.
     * @throws ResourceNotFoundException if no NatureLigneFonctionnelle is found with the given label.
     */
    @Transactional(readOnly = true)
    public Page<SpecificLigneResposneDto> getPrefiLines(SpecifiqueLineSearchDto searchCriteria, Pageable pageable) {

        log.debug("Récupération paginée des lignes de préfinancement avec critères de recherche: {}", searchCriteria);

        // Find the NatureLigneFonctionnelle by its label
        NatureLigneFonctionnelle prefiNature = natureLigneFonctionnelleRepository.findByLabel("Préfinancement")
                .orElseThrow(() -> new ResourceNotFoundException("Aucune nature de ligne fonctionnelle trouvée avec le label : Préfinancement"));

        // Build the combined specification
        Specification<LigneFonctionnelleSpecifique> natureSpec = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("natureLigneFonctionnelle"), prefiNature);

        Specification<LigneFonctionnelleSpecifique> searchSpec = LigneFonctionnelleSpecifiqueSpecification.searchForPrefi(searchCriteria);

        Specification<LigneFonctionnelleSpecifique> finalSpec = Specification.where(natureSpec).and(searchSpec);

        // Use the repository with the combined specification and pageable
        Page<LigneFonctionnelleSpecifique> page = ligneFonctionnelleSpecifiqueRepository.findAll(finalSpec, pageable);

        log.info("Page de {} lignes de préfinancement trouvée. Total: {}", page.getNumberOfElements(), page.getTotalElements());

        // Map the page of entities to a page of DTOs and return
        return page.map(ligneFonctionnelleSpecifiqueMapper::toPrefinancementResponseDto);
    }


    /**
     * Retrieves a page of SpecificLigneResposneDto with pagination, sorting, and search support.
     *
     * @param searchCriteria The DTO containing search criteria.
     * @param pageable       The pagination and sorting information.
     * @return A Page of SpecificLigneResposneDto DTOs.
     * @throws ResourceNotFoundException if no NatureLigneFonctionnelle is found with the given label.
     */
    @Transactional(readOnly = true)
    public Page<SpecificLigneResposneDto> getDCLines(SpecifiqueLineSearchDto searchCriteria, Pageable pageable) {
        log.debug("Récupération paginée des lignes de droit constatées avec critères de recherche: {}", searchCriteria);

        // Find the NatureLigneFonctionnelle by its label
        NatureLigneFonctionnelle dcLine = natureLigneFonctionnelleRepository.findByLabel("Droit constaté")
                .orElseThrow(() -> new ResourceNotFoundException("Aucune nature de ligne fonctionnelle trouvée avec le label : Droit constaté"));

        // Build the combined specification
        Specification<LigneFonctionnelleSpecifique> natureSpec = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("natureLigneFonctionnelle"), dcLine);

        Specification<LigneFonctionnelleSpecifique> searchSpec = LigneFonctionnelleSpecifiqueSpecification.searchForPrefi(searchCriteria);

        Specification<LigneFonctionnelleSpecifique> finalSpec = Specification.where(natureSpec).and(searchSpec);

        // Use the repository with the combined specification and pageable
        Page<LigneFonctionnelleSpecifique> page = ligneFonctionnelleSpecifiqueRepository.findAll(finalSpec, pageable);

        log.info("Page de {} lignes de préfinancement trouvée. Total: {}", page.getNumberOfElements(), page.getTotalElements());

        // Map the page of entities to a page of DTOs and return
        return page.map(ligneFonctionnelleSpecifiqueMapper::toDCResponseDto);
    }

    /**
     * Récupère un DTO agrégé contenant les totaux (débit, crédit, reste) et une page d'enregistrements
     * de suivi de préfinancement payés pour une ligne fonctionnelle spécifique.
     *
     * @param lineId   L'ID de la ligne fonctionnelle spécifique.
     * @param pageable Les informations de pagination et de tri.
     * @return Un AgregatedSuiviePrefiLigneResponseDto contenant les informations agrégées et paginées.
     * @throws ResourceNotFoundException si la ligne fonctionnelle spécifique n'est pas trouvée.
     */
    @Transactional(readOnly = true)
    public AgregatedSuiviePrefiLigneResponseDto getSuiviePrefiLigneResponse(Long lineId, Pageable pageable) {
        log.debug("Préparation de l'agrégation et de la pagination pour la ligne ID: {}", lineId);

        // 1. Récupérer l'entité LigneFonctionnelleSpecifique pour les détails contextuels
        LigneFonctionnelleSpecifique specificLine = ligneFonctionnelleSpecifiqueRepository.findById(lineId)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne Fonctionnelle Spécifique non trouvée avec l'ID: " + lineId));

        Marche marche = specificLine.getMarche();
        if (marche == null) {
            log.warn("La ligne fonctionnelle ID {} n'est liée à aucun marché.", lineId);
            throw new ResourceNotFoundException("Aucun marché associé à la ligne fonctionnelle avec l'ID: " + lineId);
        }

        // 2. Calculer les totaux globaux (tous les enregistrements payés)
        List<Prefinancement> allPaidPrefinancements = prefinancementRepository.findByMarcheAndStatus(marche, Status.PAYE);
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (Prefinancement prefi : allPaidPrefinancements) {
            if (prefi.getType() == Type.REMBOURSEMENT) {
                totalDebit = totalDebit.add(prefi.getMontant());
            } else if (prefi.getType() == Type.DEBLOCAGE) {
                totalCredit = totalCredit.add(prefi.getMontant());
            }
        }
        BigDecimal reste = totalCredit.subtract(totalDebit);

        // 3. Récupérer la page d'enregistrements payés
        Specification<Prefinancement> paidPrefiSpec = Specification.where(PrefinancementSpecification.byMarcheId(marche.getId()))
                .and(PrefinancementSpecification.byStatus(Status.PAYE));

        Page<Prefinancement> prefinancementPage = prefinancementRepository.findAll(paidPrefiSpec, pageable);

        // 4. Mapper la page d'entités en page de DTOs de suivi
        Page<SuiviePrefiLigneResponseDto> pageOfDtos = prefinancementPage.map(prefinancementMapper::toSuiviePrefiLigneResponseDto);

        // 5. Construire et retourner le DTO agrégé
        AgregatedSuiviePrefiLigneResponseDto aggregatedDto = new AgregatedSuiviePrefiLigneResponseDto();
        aggregatedDto.setBanqueName(specificLine.getMarche().getBanque().getNom());
        aggregatedDto.setMarcheName(marche.getNom());
        aggregatedDto.setMarcheNumero(marche.getNumero());
        aggregatedDto.setTotalDebit(totalDebit);
        aggregatedDto.setTotalCredit(totalCredit);
        aggregatedDto.setReste(reste);
        aggregatedDto.setPage(pageOfDtos);

        return aggregatedDto;
    }

    /**
     * Récupère un DTO agrégé contenant les totaux (débit, crédit, reste) et une page d'enregistrements
     * de suivi de Droit constaté payés pour une ligne fonctionnelle spécifique.
     *
     * @param lineId   L'ID de la ligne fonctionnelle spécifique.
     * @param pageable Les informations de pagination et de tri.
     * @return Un AgregatedSuivieDcLigneResponseDto contenant les informations agrégées et paginées.
     * @throws ResourceNotFoundException si la ligne fonctionnelle spécifique n'est pas trouvée.
     */
    @Transactional(readOnly = true)
    public AgregatedSuivieDcLigneResponseDto getSuivieDcLigneResponse(Long lineId, Pageable pageable) {
        log.debug("Préparation de l'agrégation et de la pagination pour la ligne ID: {}", lineId);

        // 1. Récupérer l'entité LigneFonctionnelleSpecifique pour les détails contextuels
        LigneFonctionnelleSpecifique specificLine = ligneFonctionnelleSpecifiqueRepository.findById(lineId)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne Fonctionnelle Spécifique non trouvée avec l'ID: " + lineId));

        Marche marche = specificLine.getMarche();
        if (marche == null) {
            log.warn("La ligne fonctionnelle ID {} n'est liée à aucun marché.", lineId);
            throw new ResourceNotFoundException("Aucun marché associé à la ligne fonctionnelle avec l'ID: " + lineId);
        }

        // 2. Calculer les totaux globaux (tous les enregistrements payés)
        List<DroitConstate> allPaidDc = droitConstateRepository.findByMarcheAndStatus(marche, Status.PAYE);
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (DroitConstate dc : allPaidDc) {
            if (dc.getType() == Type.REMBOURSEMENT) {
                totalDebit = totalDebit.add(dc.getMontant());
            } else if (dc.getType() == Type.DEBLOCAGE) {
                totalCredit = totalCredit.add(dc.getMontant());
            }
        }

        BigDecimal reste = totalCredit.subtract(totalDebit);

        // 3. Récupérer la page d'enregistrements payés
        Specification<DroitConstate> paidDcSpec = Specification.where(DroitConstateSpec.byMarcheId(marche.getId()))
                .and(DroitConstateSpec.byStatus(Status.PAYE));

        Page<DroitConstate> dcPage = droitConstateRepository.findAll(paidDcSpec, pageable);

        // 4. Mapper la page d'entités en page de DTOs de suivi
        Page<SuivieDcLigneResponseDto> pageOfDtos = dcPage.map(droitConstateMapper::toSuivieDcLigneResponseDto);

        // 5. Construire et retourner le DTO agrégé
        AgregatedSuivieDcLigneResponseDto aggregatedDto = new AgregatedSuivieDcLigneResponseDto();
        aggregatedDto.setBanqueName(specificLine.getMarche().getBanque().getNom());
        aggregatedDto.setMarcheName(marche.getNom());
        aggregatedDto.setMarcheNumero(marche.getNumero());
        aggregatedDto.setTotalDebit(totalDebit);
        aggregatedDto.setTotalCredit(totalCredit);
        aggregatedDto.setReste(reste);
        aggregatedDto.setPage(pageOfDtos);

        return aggregatedDto;
    }
}