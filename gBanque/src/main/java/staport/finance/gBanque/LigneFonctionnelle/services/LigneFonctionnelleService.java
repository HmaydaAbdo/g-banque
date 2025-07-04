package staport.finance.gBanque.LigneFonctionnelle.services;

import staport.finance.gBanque.LigneFonctionnelle.dtos.LigneFonctionnelleChartData;
import staport.finance.gBanque.LigneFonctionnelle.dtos.LigneFonctionnelleRequest;
import staport.finance.gBanque.LigneFonctionnelle.dtos.LigneFonctionnelleResponse;
import staport.finance.gBanque.LigneFonctionnelle.dtos.LigneFonctionnelleSearchCriteria;
import staport.finance.gBanque.LigneFonctionnelle.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.mappers.LigneFonctionnelleMapper;
import staport.finance.gBanque.LigneFonctionnelle.repositories.LigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.specifications.LigneFonctionnelleSpecification;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.NatureLigneFonctionnelleService;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class LigneFonctionnelleService {

    private static final Logger log = LoggerFactory.getLogger(LigneFonctionnelleService.class);

    private final LigneFonctionnelleRepository ligneFonctionnelleRepository;
    private final LigneFonctionnelleMapper ligneFonctionnelleMapper;
    private final BanqueRepository banqueRepository;
    private final NatureLigneFonctionnelleService natureLigneFonctionnelleService;

    @Autowired
    public LigneFonctionnelleService(LigneFonctionnelleRepository ligneFonctionnelleRepository,
                                     LigneFonctionnelleMapper ligneFonctionnelleMapper,
                                     BanqueRepository banqueRepository,
                                     NatureLigneFonctionnelleService natureLigneFonctionnelleService) {
        this.ligneFonctionnelleRepository = ligneFonctionnelleRepository;
        this.ligneFonctionnelleMapper = ligneFonctionnelleMapper;
        this.banqueRepository = banqueRepository;
        this.natureLigneFonctionnelleService = natureLigneFonctionnelleService;
    }

    @Transactional
    public LigneFonctionnelleResponse createLigneFonctionnelle(LigneFonctionnelleRequest request) {

        log.debug("Requête de création de ligne fonctionnelle: {}", request);
        validateDateRange(request.getDateDepart(), request.getDateFin());

        // Récupérer la banque et la nature pour l'entité LigneFonctionnelle
        Banque banque = banqueRepository.findById(request.getBanqueId())
                .orElseThrow(() -> new ResourceNotFoundException("Banque non trouvée avec l'ID: " + request.getBanqueId()));
        log.debug("Banque trouvée: {}", banque.getNom());

        NatureLigneFonctionnelle natureLigneFonctionnelle = natureLigneFonctionnelleService.getNatureLigneFonctionnelleEntityById(request.getNatureLigneFonctionnelleId());
        log.debug("NatureLigneFonctionnelle trouvée: {}", natureLigneFonctionnelle.getLabel());

        //  BUSINESS RULE: Check if an active LigneFonctionnelle with the same nature already exists for this bank
        // Assuming default behavior is creating an ACTIVE one
        boolean requestActif = request.getActif() == null || request.getActif();

        if (requestActif) {
            ligneFonctionnelleRepository.findByBanqueIdAndNatureLigneFonctionnelleIdAndActifTrue(
                    request.getBanqueId(),
                    request.getNatureLigneFonctionnelleId()
            ).ifPresent(existingActiveLigne -> {
                log.warn("Tentative de création d'un doublon actif pour la combinaison: Banque '{}' (ID: {}), Nature '{}' (ID: {}). Conflit avec ID: {}",
                        banque.getNom(), request.getBanqueId(), natureLigneFonctionnelle.getLabel(), request.getNatureLigneFonctionnelleId(), existingActiveLigne.getId());
                throw new DuplicateEntryException(
                        "Une ligne fonctionnelle ACTIVE de type '" + natureLigneFonctionnelle.getLabel() +
                                "' existe déjà pour la banque '" + banque.getNom() + "'."
                );
            });
        }

        LigneFonctionnelle ligneFonctionnelle = ligneFonctionnelleMapper.toEntity(request);
        ligneFonctionnelle.setBanque(banque);
        ligneFonctionnelle.setNatureLigneFonctionnelle(natureLigneFonctionnelle);
        ligneFonctionnelle.setActif(requestActif);

        try {
            LigneFonctionnelle savedLigneFonctionnelle = ligneFonctionnelleRepository.save(ligneFonctionnelle);
            log.info("Ligne fonctionnelle créée avec succès, ID: {}", savedLigneFonctionnelle.getId());
            return ligneFonctionnelleMapper.toDto(savedLigneFonctionnelle);
        } catch (DataIntegrityViolationException e) {
            log.error("Erreur d'intégrité des données lors de la création de la ligne fonctionnelle: {}", e.getMessage(), e);
            throw new DuplicateEntryException(
                    "Impossible de créer la ligne fonctionnelle. La combinaison Banque '" + banque.getNom() +
                            "', Nature '" + natureLigneFonctionnelle.getLabel() + "' existe déjà en tant que ligne ACTIVE." );
        }
    }

    public LigneFonctionnelleResponse getLigneFonctionnelleById(Long id) {
        log.debug("Récupération de ligne fonctionnelle par ID: {}", id);
        LigneFonctionnelle ligneFonctionnelle = ligneFonctionnelleRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Ligne Fonctionnelle non trouvée avec l'ID: {}", id);
                    return new ResourceNotFoundException("Ligne Fonctionnelle non trouvée avec l'ID: " + id);
                });
        log.debug("Ligne fonctionnelle trouvée: {}", ligneFonctionnelle.getId());
        return ligneFonctionnelleMapper.toDto(ligneFonctionnelle);
    }

    public Page<LigneFonctionnelleResponse> getActiveLigneFonctionnelles(LigneFonctionnelleSearchCriteria criteria, Pageable pageable) {
        log.debug("Récupération des lignes fonctionnelles ACTIVES avec critères: {} et pageable: {}", criteria, pageable);
        Specification<LigneFonctionnelle> spec = LigneFonctionnelleSpecification.searchActiveByCriteria(criteria);
        Page<LigneFonctionnelle> ligneFonctionnellesPage = ligneFonctionnelleRepository.findAll(spec, pageable);
        log.debug("Nombre de lignes fonctionnelles ACTIVES trouvées: {}", ligneFonctionnellesPage.getTotalElements());
        return ligneFonctionnellesPage.map(ligneFonctionnelleMapper::toDto);
    }

    /**
     * Get all functional lines associated with a specific bank ID, with pagination, sorting, and search capabilities.
     * This method can retrieve both active and inactive lines based on criteria.
     * @param banqueId The ID of the bank.
     * @param criteria Search criteria (natureLigneFonctionnelleId, yearDepart, actif)
     * @param pageable Pagination and sorting information.
     * @return A Page of functional lines as DTOs.
     */
    @Transactional(readOnly = true)
    public Page<LigneFonctionnelleResponse> getLignesFonctionnellesByBanqueId(Long banqueId, LigneFonctionnelleSearchCriteria criteria, Pageable pageable) {
        log.debug("Récupération paginée et filtrée des lignes fonctionnelles pour Banque ID: {} avec critères: {} et pageable: {}", banqueId, criteria, pageable);

        if (!banqueRepository.existsById(banqueId)) {
            log.warn("Banque non trouvée avec l'ID: {}", banqueId);
            throw new ResourceNotFoundException("Banque non trouvée avec l'ID: " + banqueId + ". Impossible de récupérer les lignes fonctionnelles.");
        }

        criteria.setBanqueId(banqueId); // Set the banqueId in the criteria, as it comes from the path variable

        Specification<LigneFonctionnelle> spec = LigneFonctionnelleSpecification.searchByCriteria(criteria);
        Page<LigneFonctionnelle> ligneFonctionnellesPage = ligneFonctionnelleRepository.findAll(spec, pageable);

        log.debug("Nombre de lignes fonctionnelles trouvées pour la banque {} avec filtres: {}", banqueId, ligneFonctionnellesPage.getTotalElements());
        return ligneFonctionnellesPage.map(ligneFonctionnelleMapper::toDto);
    }

    // This method needs careful review as it only checks 'actif' status.
    // The original comment about date ranges is still relevant if you need to fetch based on "current date range" AND "active flag".
    // For now, it will simply find any active ligne with the given bank and nature.
    public Optional<LigneFonctionnelleResponse> getActifLigneFonctionnellesByBanqueIdAndNature(Long banqueId, Long natureLigneFonctionnelleId){
        log.debug("Recherche de ligne fonctionnelle par Banque ID: {} et Nature ID: {}", banqueId, natureLigneFonctionnelleId);
        return ligneFonctionnelleRepository.findByBanqueIdAndNatureLigneFonctionnelleIdAndActifTrue(banqueId, natureLigneFonctionnelleId)
                .map(ligneFonctionnelleMapper::toDto);
    }

    @Transactional
    public LigneFonctionnelleResponse updateLigneFonctionnelle(Long id, LigneFonctionnelleRequest request) {
        log.debug("--- Début de la mise à jour de LigneFonctionnelle (ID: {}) ---", id);
        log.debug("Requête reçue: {}", request);

        LigneFonctionnelle existingLigneFonctionnelle = ligneFonctionnelleRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Ligne Fonctionnelle non trouvée avec l'ID: {} pour la mise à jour.", id);
                    return new ResourceNotFoundException("Ligne Fonctionnelle non trouvée avec l'ID: " + id);
                });
        log.debug("Ligne fonctionnelle existante (avant toute modification): {}", existingLigneFonctionnelle.toString());

        validateDateRange(request.getDateDepart(), request.getDateFin());

        // Récupérer les entités Banque et NatureLigneFonctionnelle basées sur la requête
        Banque targetBanque = banqueRepository.findById(request.getBanqueId())
                .orElseThrow(() -> {
                    log.warn("Banque cible non trouvée avec l'ID: {}.", request.getBanqueId());
                    return new ResourceNotFoundException("Banque non trouvée avec l'ID: " + request.getBanqueId());
                });
        log.debug("Banque cible trouvée: {}", targetBanque.getNom());

        NatureLigneFonctionnelle targetNature = natureLigneFonctionnelleService.getNatureLigneFonctionnelleEntityById(request.getNatureLigneFonctionnelleId());
        log.debug("NatureLigneFonctionnelle cible trouvée: {}", targetNature.getLabel());

        // NEW BUSINESS RULE: If the request aims to make this line 'actif=true' AND its banqueId/natureId combo changes
        // OR if the 'actif' status itself changes to true for the same combo
        // We need to check if there's already an active line with the NEW (or potentially same) bank/nature combo
        boolean requestedActif = request.getActif() != null ? request.getActif() : existingLigneFonctionnelle.isActif(); // Default to current actif status if not provided


        if (requestedActif) { // Only check for active duplicates if the line is requested to be active
            Optional<LigneFonctionnelle> existingActiveDuplicate = ligneFonctionnelleRepository.findByBanqueIdAndNatureLigneFonctionnelleIdAndActifTrue(
                    request.getBanqueId(),
                    request.getNatureLigneFonctionnelleId()
            );

            if (existingActiveDuplicate.isPresent() && !existingActiveDuplicate.get().getId().equals(id)) {
                log.warn("Tentative de mise à jour créant un doublon actif pour la combinaison: Banque '{}' (ID: {}), Nature '{}' (ID: {}). Conflit avec ID: {}",
                        targetBanque.getNom(), request.getBanqueId(), targetNature.getLabel(), request.getNatureLigneFonctionnelleId(), existingActiveDuplicate.get().getId());
                throw new DuplicateEntryException(
                        "Impossible de mettre à jour la ligne fonctionnelle. Une ligne fonctionnelle ACTIVE de type '" + targetNature.getLabel() +
                                "' existe déjà pour la banque '" + targetBanque.getNom() + "'."
                );
            }
        }

        // Apply the updates of simple fields via the mapper
        ligneFonctionnelleMapper.updateLigneFonctionnelleFromDto(request, existingLigneFonctionnelle);

        // Set the managed entities for the relationships
        existingLigneFonctionnelle.setBanque(targetBanque);
        existingLigneFonctionnelle.setNatureLigneFonctionnelle(targetNature);
        // The mapper now handles 'actif', but this explicit set ensures it's correct if not provided in DTO
        existingLigneFonctionnelle.setActif(requestedActif);


        try {
            LigneFonctionnelle updatedLigneFonctionnelle = ligneFonctionnelleRepository.save(existingLigneFonctionnelle);
            ligneFonctionnelleRepository.flush();
            log.info("Ligne fonctionnelle avec ID {} mise à jour avec succès.", updatedLigneFonctionnelle.getId());
            log.debug("--- Fin de la mise à jour de LigneFonctionnelle (ID: {}) ---", updatedLigneFonctionnelle.getId());
            return ligneFonctionnelleMapper.toDto(updatedLigneFonctionnelle);
        } catch (DataIntegrityViolationException e) {
            log.error("Erreur d'intégrité des données (doublon ou autre) lors de la mise à jour de la ligne fonctionnelle (ID: {}): {}", id, e.getMessage(), e);
            // This catch block is for general DataIntegrityViolationException
            throw new DuplicateEntryException(
                    "Impossible de mettre à jour la ligne fonctionnelle. Une ligne fonctionnelle ACTIVE avec la même Nature existe déjà pour la Banque spécifiée." );
        }
    }

    @Transactional
    public void deleteLigneFonctionnelle(Long id) {
        log.debug("Requête de suppression de ligne fonctionnelle pour ID: {}", id);
        if (!ligneFonctionnelleRepository.existsById(id)) {
            log.warn("Ligne Fonctionnelle non trouvée avec l'ID: {} pour la suppression.", id);
            throw new ResourceNotFoundException("Ligne Fonctionnelle non trouvée avec l'ID: " + id);
        }
        ligneFonctionnelleRepository.deleteById(id);
        log.info("Ligne fonctionnelle avec ID {} supprimée avec succès.", id);
    }

    /**
     * Deactivates the currently active LigneFonctionnelle for a given bank and natureLigneFonctionnelle.
     * Sets its 'actif' flag to false.
     * @param banqueId The ID of the bank.
     * @param natureLigneFonctionnelleId The ID of the nature of the functional line to deactivate.
     * @throws ResourceNotFoundException if no active functional line is found for the given criteria.
     */
    @Transactional
    public void deactivateLigneFonctionnelleByNatureAndBank(Long banqueId, Long natureLigneFonctionnelleId) {

        log.debug("Tentative de désactivation de la ligne fonctionnelle pour Banque ID: {} et Nature ID: {}", banqueId, natureLigneFonctionnelleId);
        // Find the currently active ligne fonctionnelle
        LigneFonctionnelle ligneToDeactivate = ligneFonctionnelleRepository
                .findByBanqueIdAndNatureLigneFonctionnelleIdAndActifTrue(banqueId, natureLigneFonctionnelleId)
                .orElseThrow(() -> {
                    log.warn("Aucune ligne fonctionnelle ACTIVE trouvée pour Banque ID: {} et Nature ID: {}. Impossible de désactiver.", banqueId, natureLigneFonctionnelleId);
                    return new ResourceNotFoundException(
                            "Aucune ligne fonctionnelle ACTIVE trouvée pour la Banque avec l'ID " + banqueId +
                                    " et la Nature de ligne avec l'ID " + natureLigneFonctionnelleId + ". Désactivation impossible."
                    );
                });

        ligneToDeactivate.setActif(false);
        ligneFonctionnelleRepository.save(ligneToDeactivate);
        log.info("Ligne fonctionnelle avec ID {} (Banque ID: {}, Nature ID: {}) désactivée avec succès.",
                ligneToDeactivate.getId(), banqueId, natureLigneFonctionnelleId);
    }


    private void validateDateRange(OffsetDateTime dateDepart, OffsetDateTime dateFin) {
        if (dateDepart != null && dateFin != null && dateFin.isBefore(dateDepart)) {
            log.warn("Validation de date échouée: dateFin ({}) est antérieure à dateDepart ({}).", dateFin, dateDepart);
            throw new IllegalArgumentException("La date de fin (" + dateFin + ") ne peut pas être antérieure à la date de départ (" + dateDepart + ").");
        }
    }

    /**
     * Retrieves historical plafond and taux data for a specific bank and nature of functional line,
     * formatted for a PrimeNG line chart.
     * The data is sorted by dateDepart ascending.
     *
     * @param banqueId The ID of the bank.
     * @param natureLigneFonctionnelleId The ID of the nature of the functional line.
     * @return A LigneFonctionnelleChartData object containing labels (years), plafond data, and taux data.
     * @throws ResourceNotFoundException if the bank or natureLigneFonctionnelle is not found.
     */
    @Transactional(readOnly = true)
    public LigneFonctionnelleChartData getHistoricalChartData(Long banqueId, Long natureLigneFonctionnelleId) {
        log.debug("Récupération des données historiques pour le graphique pour Banque ID: {} et Nature ID: {}", banqueId, natureLigneFonctionnelleId);

        // Validate existence of Bank and NatureLigneFonctionnelle first
        if (!banqueRepository.existsById(banqueId)) {
            log.warn("Banque non trouvée avec l'ID: {}", banqueId);
            throw new ResourceNotFoundException("Banque non trouvée avec l'ID: " + banqueId);
        }
        // Assuming natureLigneFonctionnelleService.getNatureLigneFonctionnelleEntityById already throws ResourceNotFoundException
        natureLigneFonctionnelleService.getNatureLigneFonctionnelleEntityById(natureLigneFonctionnelleId);
        log.debug("Banque et NatureLigneFonctionnelle validées.");

        // Fetch all relevant LigneFonctionnelle entries, sorted by dateDepart
        List<LigneFonctionnelle> historicalLines = ligneFonctionnelleRepository.findByBanqueIdAndNatureLigneFonctionnelleIdOrderByDateDepartAsc(banqueId, natureLigneFonctionnelleId);

        if (historicalLines.isEmpty()) {
            log.info("Aucune ligne fonctionnelle historique trouvée pour Banque ID: {} et Nature ID: {}.", banqueId, natureLigneFonctionnelleId);
            return new LigneFonctionnelleChartData(new ArrayList<>(), new ArrayList<>()); // Return empty data structure
        }

        // Prepare lists for labels and datasets
        List<String> labels = new ArrayList<>();
        List<BigDecimal> tauxData = new ArrayList<>();
        List<BigDecimal> plafondData = new ArrayList<>();

        // Populate the lists
        // Using distinct years for labels
        List<String> finalLabels = labels;
        historicalLines
                .forEach(line -> {
                    String yearLabel = String.valueOf(line.getDateDepart().getYear());
                    if (!finalLabels.contains(yearLabel)) {
                        finalLabels.add(yearLabel);
                    }
                    tauxData.add(line.getTaux());
                    plafondData.add(line.getPlafond());
                });

        // Ensure labels are sorted if you extracted them dynamically
        labels = labels.stream().sorted().collect(Collectors.toList());

        // Create the datasets
        LigneFonctionnelleChartData.ChartDataset tauxDataset = new LigneFonctionnelleChartData.ChartDataset(
                "Taux",
                tauxData,
                false,
                // You might want to define these colors as constants or pass them from frontend
                "var(--blue-500)", // Using CSS variable names as strings
                0.4
        );

        LigneFonctionnelleChartData.ChartDataset plafondDataset = new LigneFonctionnelleChartData.ChartDataset(
                "Plafond",
                plafondData,
                false,
                "var(--pink-500)", // Using CSS variable names as strings
                0.4
        );

        List<LigneFonctionnelleChartData.ChartDataset> datasets = new ArrayList<>();
        datasets.add(tauxDataset);
        datasets.add(plafondDataset);

        log.debug("Données historiques du graphique préparées: Labels={}, Taux Data={}, Plafond Data={}", labels, tauxData.size(), plafondData.size());
        return new LigneFonctionnelleChartData(labels, datasets);
    }
}