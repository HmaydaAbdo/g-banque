package staport.finance.gBanque.LigneFonctionnelle.services;

import staport.finance.gBanque.LigneFonctionnelle.dtos.*;
import staport.finance.gBanque.LigneFonctionnelle.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.mappers.LigneFonctionnelleMapper;
import staport.finance.gBanque.LigneFonctionnelle.repositories.LigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.specifications.LigneFonctionnelleSpecification;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories.NatureLigneFonctionnelleRepository;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class LigneFonctionnelleService {

    private static final Logger logger = LoggerFactory.getLogger(LigneFonctionnelleService.class);
    // Constante de Règle Métier : Seule cette nature peut être une Ligne Mère
    private static final String CAUTION_ADMINISTRATIVE_LABEL = "Caution Administrative";

    private final LigneFonctionnelleRepository ligneFonctionnelleRepository;
    private final LigneFonctionnelleMapper ligneFonctionnelleMapper;
    private final BanqueRepository banqueRepository;
    private final NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository;

    @Autowired
    public LigneFonctionnelleService(LigneFonctionnelleRepository ligneFonctionnelleRepository,
                                     LigneFonctionnelleMapper ligneFonctionnelleMapper,
                                     BanqueRepository banqueRepository,
                                     NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository) {
        this.ligneFonctionnelleRepository = ligneFonctionnelleRepository;
        this.ligneFonctionnelleMapper = ligneFonctionnelleMapper;
        this.banqueRepository = banqueRepository;
        this.natureLigneFonctionnelleRepository = natureLigneFonctionnelleRepository;
    }


    public LigneFonctionnelleResponse createLigneFonctionnelle(LigneFonctionnelleRequest request) {

        logger.debug("Creating ligne fonctionnelle , Full creation request: {}", request);

        validateDateRange(request.getDateDepart(), request.getDateFin());

        Banque banque = banqueRepository.findById(request.getBanqueId())
                .orElseThrow(() -> {
                    logger.error("Banque not found with ID: {}", request.getBanqueId());
                    return new ResourceNotFoundException("Banque non trouvée avec l'ID: " + request.getBanqueId());
                });

        logger.debug("Found banque: {} (ID: {})", banque.getNom(), banque.getId());

        NatureLigneFonctionnelle natureLigneFonctionnelle = natureLigneFonctionnelleRepository.findById(request.getNatureLigneFonctionnelleId())
                .orElseThrow(() -> {
                    logger.error("NatureLigneFonctionnelle not found with ID: {}", request.getNatureLigneFonctionnelleId());
                    return new ResourceNotFoundException("NatureLigneFonctionnelle non trouvée avec l'ID: " + request.getNatureLigneFonctionnelleId());
                });

        logger.debug("Found nature: {} (ID: {})", natureLigneFonctionnelle.getLabel(), natureLigneFonctionnelle.getId());

        avoidDuplicateActifLines(banque, natureLigneFonctionnelle);

        LigneFonctionnelle ligneFonctionnelle = ligneFonctionnelleMapper.toEntity(request, banque, natureLigneFonctionnelle);

        if (request.getSousLignes() != null && !request.getSousLignes().isEmpty()) {
            logger.info("Processing {} sous-ligne(s) for creation", request.getSousLignes().size());

            validateNatureForMere(natureLigneFonctionnelle);

            List<LigneFonctionnelle> sousLignesEntities = new ArrayList<>();

            for (SousLigneRequestDto sousLigneRequest : request.getSousLignes()) {
                logger.debug("Processing sous-ligne - Nature ID: {}, Plafond: {}", sousLigneRequest.getNatureLigneFonctionnelleId(), sousLigneRequest.getPlafond());

                if (sousLigneRequest.getId() != null) {
                    logger.error("Sous-ligne ID must be NULL during cascade creation, but got: {}", sousLigneRequest.getId());
                    throw new IllegalArgumentException("L'ID de la sous-ligne doit être NULL lors de la création en cascade.");
                }

                NatureLigneFonctionnelle natureSousLigne = natureLigneFonctionnelleRepository.findById(sousLigneRequest.getNatureLigneFonctionnelleId())
                        .orElseThrow(() -> {
                            logger.error("NatureLigneFonctionnelle for sous-ligne not found with ID: {}", sousLigneRequest.getNatureLigneFonctionnelleId());
                            return new ResourceNotFoundException("NatureLigneFonctionnelle pour Sous-ligne non trouvée avec l'ID: " + sousLigneRequest.getNatureLigneFonctionnelleId());
                        });

                LigneFonctionnelle sousLigneEntity = ligneFonctionnelleMapper.toSousLigneEntity(
                        sousLigneRequest,
                        ligneFonctionnelle,
                        natureSousLigne
                );

                sousLignesEntities.add(sousLigneEntity);
            }

            ligneFonctionnelle.setSousLignes(sousLignesEntities);
            logger.debug("Successfully prepared {} sous-ligne(s) for cascade creation", sousLignesEntities.size());
        }

        LigneFonctionnelle savedLigneFonctionnelle = ligneFonctionnelleRepository.save(ligneFonctionnelle);
        logger.info("Successfully created ligne fonctionnelle with ID: {}, Has {} sous-ligne(s)",
                savedLigneFonctionnelle.getId(),
                savedLigneFonctionnelle.getSousLignes() != null ? savedLigneFonctionnelle.getSousLignes().size() : 0);

        return ligneFonctionnelleMapper.toDto(savedLigneFonctionnelle);
    }

    public LigneFonctionnelleResponse getLigneFonctionnelleById(Long id) {
        logger.info("Fetching ligne fonctionnelle by ID: {}", id);

        LigneFonctionnelle ligneFonctionnelle = ligneFonctionnelleRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Ligne Fonctionnelle not found with ID: {}", id);
                    return new ResourceNotFoundException("Ligne Fonctionnelle non trouvée avec l'ID: " + id);
                });

        logger.debug("Successfully retrieved ligne fonctionnelle ID: {}, Nature: {}, Plafond: {}",
                ligneFonctionnelle.getId(),
                ligneFonctionnelle.getNatureLigneFonctionnelle().getLabel(),
                ligneFonctionnelle.getPlafond());

        return ligneFonctionnelleMapper.toDto(ligneFonctionnelle);
    }

    // =========================================================================================
    // 3. READ (PAGINATION FILTRÉE - MÈRES/INDÉPENDANTES UNIQUEMENT)
    // =========================================================================================

    public Page<LigneFonctionnelleResponse> getPaginatedLigneFonctionnelles(LigneFonctionnelleSearchCriteria criteria, Pageable pageable) {
        logger.info("Fetching paginated lignes fonctionnelles (Mère/Indépendante only) - Page: {}, Size: {}", pageable.getPageNumber(), pageable.getPageSize());
        logger.debug("Search criteria: {}", criteria);

        Specification<LigneFonctionnelle> spec = LigneFonctionnelleSpecification.searchByCriteria(criteria);
        Specification<LigneFonctionnelle> onlyParentsSpec = LigneFonctionnelleSpecification.isLigneMereOrIndependante();
        spec = spec.and(onlyParentsSpec);

        Page<LigneFonctionnelle> ligneFonctionnellesPage = ligneFonctionnelleRepository.findAll(spec, pageable);

        logger.info("Retrieved {} ligne(s) fonctionnelle(s) Mère/Indépendante out of {} total (Page {}/{})",
                ligneFonctionnellesPage.getNumberOfElements(),
                ligneFonctionnellesPage.getTotalElements(),
                ligneFonctionnellesPage.getNumber() + 1,
                ligneFonctionnellesPage.getTotalPages());

        return ligneFonctionnellesPage.map(ligneFonctionnelleMapper::toDto);
    }
    // =========================================================================================
    // 4. READ (PAGINATION FILTRÉE PAR BANQUE - MÈRES/INDÉPENDANTES UNIQUEMENT)
    // =========================================================================================

    @Transactional(readOnly = true)
    public Page<LigneFonctionnelleResponse> getPaginatedLignesFonctionnellesByBanqueId(Long banqueId, LigneFonctionnelleSearchCriteria criteria, Pageable pageable) {
        logger.info("Fetching paginated lignes fonctionnelles for banque ID: {} - Page: {}, Size: {}",
                banqueId, pageable.getPageNumber(), pageable.getPageSize());
        logger.debug("Search criteria for banque {}: {}", banqueId, criteria);

        if (!banqueRepository.existsById(banqueId)) {
            logger.error("Banque not found with ID: {} while fetching lignes fonctionnelles", banqueId);
            throw new ResourceNotFoundException("Banque non trouvée avec l'ID: " + banqueId + ". Impossible de récupérer les lignes fonctionnelles.");
        }

        criteria.setBanqueId(banqueId);

        Specification<LigneFonctionnelle> spec = LigneFonctionnelleSpecification.searchByCriteria(criteria);
        Specification<LigneFonctionnelle> onlyParentsSpec = LigneFonctionnelleSpecification.isLigneMereOrIndependante();
        spec = spec.and(onlyParentsSpec);

        Page<LigneFonctionnelle> ligneFonctionnellesPage = ligneFonctionnelleRepository.findAll(spec, pageable);

        logger.info("Retrieved {} ligne(s) Mère/Indépendante for banque ID: {} out of {} total (Page {}/{})",
                ligneFonctionnellesPage.getNumberOfElements(),
                banqueId,
                ligneFonctionnellesPage.getTotalElements(),
                ligneFonctionnellesPage.getNumber() + 1,
                ligneFonctionnellesPage.getTotalPages());

        return ligneFonctionnellesPage.map(ligneFonctionnelleMapper::toDto);
    }

    // =========================================================================================
    // 5. UPDATE (LIGNE MÈRE/INDÉPENDANTE)
    // =========================================================================================
    @Transactional
    public LigneFonctionnelleResponse updateLigneFonctionnelle(Long id, LigneFonctionnelleRequest request) {

        logger.info("Updating ligne fonctionnelle ID: {} - New plafond: {}, New taux: {}", id, request.getPlafond(), request.getTaux());
        logger.debug("Full update request for ID {}: {}", id, request);

        LigneFonctionnelle existingLigneFonctionnelle = ligneFonctionnelleRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Ligne Fonctionnelle not found with ID: {} for update", id);
                    return new ResourceNotFoundException("Ligne Fonctionnelle non trouvée avec l'ID: " + id);
                });

        logger.debug("Found existing ligne fonctionnelle: ID {}, Current plafond: {}, Has {} sous-ligne(s)",
                existingLigneFonctionnelle.getId(),
                existingLigneFonctionnelle.getPlafond(),
                existingLigneFonctionnelle.getSousLignes() != null ? existingLigneFonctionnelle.getSousLignes().size() : 0);

        validateDateRange(request.getDateDepart(), request.getDateFin());

        Banque targetBanque = banqueRepository.findById(request.getBanqueId())
                .orElseThrow(() -> {
                    logger.error("Target banque not found with ID: {}", request.getBanqueId());
                    return new ResourceNotFoundException("Banque non trouvée avec l'ID: " + request.getBanqueId());
                });

        NatureLigneFonctionnelle targetNature = natureLigneFonctionnelleRepository.findById(request.getNatureLigneFonctionnelleId())
                .orElseThrow(() -> {
                    logger.error("Target nature not found with ID: {}", request.getNatureLigneFonctionnelleId());
                    return new ResourceNotFoundException("NatureLigneFonctionnelle non trouvée avec l'ID: " + request.getNatureLigneFonctionnelleId());
                });

        if (existingLigneFonctionnelle.getSousLignes() != null && !existingLigneFonctionnelle.getSousLignes().isEmpty()) {
            logger.debug("Ligne mère has {} sous-ligne(s), validating plafond and nature", existingLigneFonctionnelle.getSousLignes().size());

            validateNatureForMere(targetNature);

        }

        if (existingLigneFonctionnelle.isActif() && (
                !existingLigneFonctionnelle.getBanque().getId().equals(targetBanque.getId()) ||
                        !existingLigneFonctionnelle.getNatureLigneFonctionnelle().getId().equals(targetNature.getId()))
        ) {
            logger.debug("Key changed (banque or nature), checking for duplicate active lines");
            avoidDuplicateActifLines(targetBanque, targetNature);
        }

        ligneFonctionnelleMapper.updateLigneFonctionnelleFromDto(request, existingLigneFonctionnelle, targetBanque, targetNature);

        if (existingLigneFonctionnelle.getSousLignes() != null) {
            logger.debug("Propagating inherited properties to {} sous-ligne(s)", existingLigneFonctionnelle.getSousLignes().size());
            for (LigneFonctionnelle sousLigne : existingLigneFonctionnelle.getSousLignes()) {
                sousLigne.setTaux(existingLigneFonctionnelle.getTaux());
                sousLigne.setBanque(existingLigneFonctionnelle.getBanque());
                sousLigne.setDateDepart(existingLigneFonctionnelle.getDateDepart());
                sousLigne.setDateFin(existingLigneFonctionnelle.getDateFin());
            }
        }

        LigneFonctionnelle updatedLigneFonctionnelle = ligneFonctionnelleRepository.save(existingLigneFonctionnelle);
        logger.info("Successfully updated ligne fonctionnelle ID: {}", id);

        return ligneFonctionnelleMapper.toDto(updatedLigneFonctionnelle);
    }


    // =========================================================================================
    // 6. MANAGE SOUS-LIGNES (CRUD sur la collection d'enfants)
    // =========================================================================================

    @Transactional
    public LigneFonctionnelleResponse manageSousLignes(Long ligneMereId, List<SousLigneRequestDto> sousLignesRequests) {
        logger.info("Managing sous-lignes for ligne mère ID: {} - {} sous-ligne(s) in request",
                ligneMereId, sousLignesRequests.size());
        logger.debug("Sous-lignes requests: {}", sousLignesRequests);

        LigneFonctionnelle ligneMere = ligneFonctionnelleRepository.findById(ligneMereId)
                .orElseThrow(() -> {
                    logger.error("Ligne Mère not found with ID: {}", ligneMereId);
                    return new ResourceNotFoundException("Ligne Mère non trouvée avec l'ID: " + ligneMereId);
                });

        if (ligneMere.getLigneMere() != null) {
            logger.error("ID {} corresponds to a sous-ligne, cannot manage children", ligneMereId);
            throw new IllegalArgumentException("L'ID fourni correspond à une Sous-ligne. Seule une Ligne Mère peut gérer ses enfants.");
        }

        validateNatureForMere(ligneMere.getNatureLigneFonctionnelle());

        Map<Long, LigneFonctionnelle> existingSousLignesMap = ligneMere.getSousLignes().stream()
                .collect(Collectors.toMap(LigneFonctionnelle::getId, Function.identity()));

        logger.debug("Existing sous-lignes count: {}", existingSousLignesMap.size());

        List<LigneFonctionnelle> updatedSousLignes = new ArrayList<>();

        Banque mereBanque = ligneMere.getBanque();
        BigDecimal mereTaux = ligneMere.getTaux();
        OffsetDateTime mereDateDepart = ligneMere.getDateDepart();
        OffsetDateTime mereDateFin = ligneMere.getDateFin();

        int updateCount = 0;
        int createCount = 0;

        for (SousLigneRequestDto sousLigneRequest : sousLignesRequests) {
            LigneFonctionnelle currentSousLigne;

            NatureLigneFonctionnelle natureSousLigne = natureLigneFonctionnelleRepository.findById(sousLigneRequest.getNatureLigneFonctionnelleId())
                    .orElseThrow(() -> {
                        logger.error("Nature for sous-ligne not found with ID: {}", sousLigneRequest.getNatureLigneFonctionnelleId());
                        return new ResourceNotFoundException("NatureLigneFonctionnelle pour Sous-ligne non trouvée avec l'ID: " + sousLigneRequest.getNatureLigneFonctionnelleId());
                    });

            if (sousLigneRequest.getId() != null) {
                logger.debug("Updating existing sous-ligne ID: {}", sousLigneRequest.getId());
                currentSousLigne = existingSousLignesMap.get(sousLigneRequest.getId());
                if (currentSousLigne == null) {
                    logger.error("Sous-ligne ID {} not found or doesn't belong to ligne mère {}", sousLigneRequest.getId(), ligneMereId);
                    throw new ResourceNotFoundException("Sous-ligne non trouvée ou n'appartient pas à la ligne Mère: " + sousLigneRequest.getId());
                }

                updateSousLigneProperties(
                        currentSousLigne, sousLigneRequest, natureSousLigne,
                        mereBanque, mereTaux, mereDateDepart, mereDateFin
                );
                updateCount++;
            } else {
                logger.debug("Creating new sous-ligne with nature ID: {}, plafond: {}",
                        sousLigneRequest.getNatureLigneFonctionnelleId(), sousLigneRequest.getPlafond());
                currentSousLigne = ligneFonctionnelleMapper.toSousLigneEntity(
                        sousLigneRequest,
                        ligneMere,
                        natureSousLigne
                );
                createCount++;
            }

            updatedSousLignes.add(currentSousLigne);
        }

        int deleteCount = existingSousLignesMap.size() - updateCount;
        logger.debug("Sous-lignes management summary - Creating: {}, Updating: {}, Deleting: {}",
                createCount, updateCount, deleteCount);

        ligneMere.getSousLignes().clear();
        ligneMere.getSousLignes().addAll(updatedSousLignes);

        LigneFonctionnelle updatedLigneMere = ligneFonctionnelleRepository.save(ligneMere);
        logger.info("Successfully managed sous-lignes for ligne mère ID: {} - Total persisted: {}",
                ligneMereId, updatedSousLignes.size());

        return ligneFonctionnelleMapper.toDto(updatedLigneMere);
    }


    @Transactional
    public void deactivateLigneFonctionnelle(Long ligneId) {
        logger.info("Attempting to deactivate ligne fonctionnelle ID: {}", ligneId);

        LigneFonctionnelle ligneToDeactivate = ligneFonctionnelleRepository.findById(ligneId)
                .orElseThrow(() -> {
                    logger.error("Ligne Fonctionnelle not found with ID: {} for deactivation", ligneId);
                    return new ResourceNotFoundException("Aucune Ligne Fonctionnelle avec l'ID: " + ligneId);
                });

        if (ligneToDeactivate.getLigneMere() != null) {
            logger.error("Cannot deactivate sous-ligne directly - ID: {}, Ligne mère ID: {}",
                    ligneId, ligneToDeactivate.getLigneMere().getId());
            throw new IllegalArgumentException(
                    "La désactivation directe des Sous-lignes n'est pas autorisée. Veuillez désactiver la Ligne Mère (ID: " + ligneToDeactivate.getLigneMere().getId() + ") pour affecter tous les enfants."
            );
        }

        if (!ligneToDeactivate.isActif()) {
            logger.info("Ligne fonctionnelle ID: {} is already inactive, no action needed", ligneId);
            return;
        }

        ligneToDeactivate.setActif(false);
        logger.debug("Deactivated ligne Mère/Indépendante ID: {}", ligneId);

        if (ligneToDeactivate.getSousLignes() != null && !ligneToDeactivate.getSousLignes().isEmpty()) {
            int deactivatedCount = 0;
            for (LigneFonctionnelle sousLigne : ligneToDeactivate.getSousLignes()) {
                if (sousLigne.isActif()) {
                    sousLigne.setActif(false);
                    deactivatedCount++;
                }
            }
            logger.debug("Deactivated {} sous-ligne(s) in cascade", deactivatedCount);
        }

        ligneFonctionnelleRepository.save(ligneToDeactivate);
        logger.info("Successfully deactivated ligne fonctionnelle ID: {} and its sous-lignes", ligneId);
    }

    @Transactional
    public void deleteLigneFonctionnelle(Long id) {
        logger.info("Attempting to delete ligne fonctionnelle ID: {}", id);

        LigneFonctionnelle ligneToDelete = ligneFonctionnelleRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Ligne Fonctionnelle not found with ID: {} for deletion", id);
                    return new ResourceNotFoundException("Ligne Fonctionnelle non trouvée avec l'ID: " + id);
                });

        if (ligneToDelete.getLigneMere() != null) {
            logger.error("Cannot delete sous-ligne directly - ID: {}, Ligne mère ID: {}",
                    id, ligneToDelete.getLigneMere().getId());
            throw new IllegalArgumentException(
                    "La suppression directe des Sous-lignes n'est pas autorisée. Veuillez supprimer la Ligne Mère (ID: " + ligneToDelete.getLigneMere().getId() + ") pour supprimer tous les enfants en cascade."
            );
        }

        int sousLignesCount = ligneToDelete.getSousLignes() != null ? ligneToDelete.getSousLignes().size() : 0;
        logger.debug("Deleting ligne mère with {} sous-ligne(s)", sousLignesCount);

        try {
            ligneFonctionnelleRepository.delete(ligneToDelete);
            logger.info("Successfully deleted ligne fonctionnelle ID: {} and {} sous-ligne(s)", id, sousLignesCount);
        } catch (Exception e) {
            logger.error("Failed to delete ligne fonctionnelle ID: {}", id, e);
            throw new RuntimeException("Échec de la suppression de la ligne fonctionnelle et de ses dépendances.", e);
        }
    }


    // =========================================================================================
    // 9. CHART HISTORY
    // =========================================================================================

    @Transactional(readOnly = true)
    public LigneFonctionnelleChartData getHistoricalChartData(Long banqueId, Long natureLigneFonctionnelleId) {
        logger.info("Fetching historical chart data - Banque ID: {}, Nature ID: {}", banqueId, natureLigneFonctionnelleId);

        if (!banqueRepository.existsById(banqueId)) {
            logger.error("Banque not found with ID: {} for historical chart data", banqueId);
            throw new ResourceNotFoundException("Banque non trouvée avec l'ID: " + banqueId);
        }

        natureLigneFonctionnelleRepository.findById(natureLigneFonctionnelleId)
                .orElseThrow(() -> {
                    logger.error("NatureLigneFonctionnelle not found with ID: {} for historical chart data", natureLigneFonctionnelleId);
                    return new ResourceNotFoundException("NatureLigneFonctionnelle non trouvée avec l'ID: " + natureLigneFonctionnelleId);
                });

        List<LigneFonctionnelle> historicalLines = ligneFonctionnelleRepository.findByBanqueIdAndNatureLigneFonctionnelleIdOrderByDateDepartAsc(banqueId, natureLigneFonctionnelleId);

        if (historicalLines.isEmpty()) {
            logger.info("No historical lines found for banque ID: {} and nature ID: {}", banqueId, natureLigneFonctionnelleId);
            return new LigneFonctionnelleChartData(new ArrayList<>(), new ArrayList<>());
        }

        logger.debug("Found {} historical ligne(s) for chart", historicalLines.size());

        List<String> labels = historicalLines.stream()
                .map(line -> String.valueOf(line.getDateDepart().getYear()))
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        List<BigDecimal> tauxData = historicalLines.stream()
                .map(LigneFonctionnelle::getTaux)
                .collect(Collectors.toList());

        List<BigDecimal> plafondData = historicalLines.stream()
                .map(LigneFonctionnelle::getPlafond)
                .collect(Collectors.toList());

        LigneFonctionnelleChartData.ChartDataset tauxDataset = new LigneFonctionnelleChartData.ChartDataset(
                "Taux", tauxData, false, "var(--blue-500)", 0.4
        );

        LigneFonctionnelleChartData.ChartDataset plafondDataset = new LigneFonctionnelleChartData.ChartDataset(
                "Plafond", plafondData, false, "var(--pink-500)", 0.4
        );

        List<LigneFonctionnelleChartData.ChartDataset> datasets = new ArrayList<>();
        datasets.add(tauxDataset);
        datasets.add(plafondDataset);

        logger.info("Successfully prepared historical chart data - {} labels, {} taux data points, {} plafond data points",
                labels.size(), tauxData.size(), plafondData.size());

        return new LigneFonctionnelleChartData(labels, datasets);
    }
    // =========================================================================================
    // 10. VALIDATIONS ET HELPERS PRIVÉS
    // =========================================================================================

    /**
     * Règle Métier 1: Valide si la NatureLigneFonctionnelle est autorisée à être une Ligne Mère.
     * Seule la 'Caution Administrative' peut contenir des sous-lignes.
     */
    private void validateNatureForMere(NatureLigneFonctionnelle nature) {
        if (!CAUTION_ADMINISTRATIVE_LABEL.equals(nature.getLabel())) {
            logger.warn("Validation de nature échouée: La nature '{}' ne peut pas être une ligne mère.", nature.getLabel());
            throw new IllegalArgumentException(
                    "Seule la nature ayant le label '" + CAUTION_ADMINISTRATIVE_LABEL + "' est autorisée à être une Ligne Mère (contenir des sous-lignes)."
            );
        }
    }



    /**
     * Helper pour mettre à jour les propriétés spécifiques (Plafond, Nature) et héritées (Taux, Dates, Banque) d'une sous-ligne existante.
     */
    private void updateSousLigneProperties(LigneFonctionnelle sousLigneEntity,
                                           SousLigneRequestDto request,
                                           NatureLigneFonctionnelle newNatureSousLigne,
                                           Banque mereBanque,
                                           BigDecimal mereTaux,
                                           OffsetDateTime mereDateDepart,
                                           OffsetDateTime mereDateFin) {

        // Propriétés spécifiques
        sousLigneEntity.setPlafond(request.getPlafond());
        sousLigneEntity.setNatureLigneFonctionnelle(newNatureSousLigne);

        // Propriétés héritées
        sousLigneEntity.setBanque(mereBanque);
        sousLigneEntity.setTaux(mereTaux);
        sousLigneEntity.setDateDepart(mereDateDepart);
        sousLigneEntity.setDateFin(mereDateFin);
    }

    private void validateDateRange(OffsetDateTime dateDepart, OffsetDateTime dateFin) {
        if (dateDepart != null && dateFin != null && dateFin.isBefore(dateDepart)) {
            logger.warn("Validation de date échouée: dateFin ({}) est antérieure à dateDepart ({}).", dateFin, dateDepart);
            throw new IllegalArgumentException("La date de fin ne peut pas être antérieure à la date de départ.");
        }
    }

    private void avoidDuplicateActifLines(Banque banque, NatureLigneFonctionnelle natureLigneFonctionnelle) {

        Optional<LigneFonctionnelle> existingActifLineWithSameBanqueAndNature = ligneFonctionnelleRepository.findByBanqueIdAndNatureLigneFonctionnelleIdAndActifTrue(banque.getId(), natureLigneFonctionnelle.getId());

        existingActifLineWithSameBanqueAndNature.ifPresent(existingActiveLine -> {
            logger.warn("Tentative de création d'un doublon actif pour la combinaison: Banque '{}', Nature '{}'.",
                    banque.getNom(), natureLigneFonctionnelle.getLabel());
            throw new DuplicateEntryException(
                    "Une ligne fonctionnelle ACTIVE de type '" + natureLigneFonctionnelle.getLabel() +
                            "' existe déjà pour la banque '" + banque.getNom() + "'."
            );
        });
    }
}