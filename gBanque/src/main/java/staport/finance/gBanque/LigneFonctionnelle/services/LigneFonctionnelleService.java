package staport.finance.gBanque.LigneFonctionnelle.services;

import staport.finance.gBanque.LigneFonctionnelle.dtos.LigneFonctionnelleChartData;
import staport.finance.gBanque.LigneFonctionnelle.dtos.LigneFonctionnelleRequest;
import staport.finance.gBanque.LigneFonctionnelle.dtos.LigneFonctionnelleResponse;
import staport.finance.gBanque.LigneFonctionnelle.dtos.LigneFonctionnelleSearchCriteria;
import staport.finance.gBanque.LigneFonctionnelle.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.mappers.LigneFonctionnelleMapper; // Corrected import to your provided manual mapper name
import staport.finance.gBanque.LigneFonctionnelle.repositories.LigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.specifications.LigneFonctionnelleSpecification;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories.NatureLigneFonctionnelleRepository;
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

    @Transactional
    public LigneFonctionnelleResponse createLigneFonctionnelle(LigneFonctionnelleRequest request) {
        log.debug("Requête de création de ligne fonctionnelle: {}", request);

        // assurer que la date de fin > la date de départ
        validateDateRange(request.getDateDepart(), request.getDateFin());

        // récupérer la banque
        Banque banque = banqueRepository.findById(request.getBanqueId())
                .orElseThrow(() -> new ResourceNotFoundException("Banque non trouvée avec l'ID: " + request.getBanqueId()));

        // récupérer la nature de la ligne fonctionnelle
        NatureLigneFonctionnelle natureLigneFonctionnelle = natureLigneFonctionnelleRepository.findById(request.getNatureLigneFonctionnelleId())
                .orElseThrow(() -> new ResourceNotFoundException("NatureLigneFonctionnelle non trouvée avec l'ID: " + request.getNatureLigneFonctionnelleId()));


        // avoir having more than one actif line by banque and nature
        avoidDuplicateActifLines(banque, natureLigneFonctionnelle);

        LigneFonctionnelle ligneFonctionnelle = ligneFonctionnelleMapper.toEntity(request, banque, natureLigneFonctionnelle);

        LigneFonctionnelle savedLigneFonctionnelle = ligneFonctionnelleRepository.save(ligneFonctionnelle);
        log.info("Ligne fonctionnelle créée avec succès, ID: {}", savedLigneFonctionnelle.getId());
        return ligneFonctionnelleMapper.toDto(savedLigneFonctionnelle);

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

        Banque targetBanque = banqueRepository.findById(request.getBanqueId())
                .orElseThrow(() -> {
                    log.warn("Banque cible non trouvée avec l'ID: {}.", request.getBanqueId());
                    return new ResourceNotFoundException("Banque non trouvée avec l'ID: " + request.getBanqueId());
                });

        log.debug("Banque cible trouvée: {}", targetBanque.getNom());

        NatureLigneFonctionnelle targetNature = natureLigneFonctionnelleRepository.findById(request.getNatureLigneFonctionnelleId())
                .orElseThrow(() -> {
                    log.warn("NatureLigneFonctionnelle cible non trouvée avec l'ID: {}.", request.getNatureLigneFonctionnelleId());
                    return new ResourceNotFoundException("NatureLigneFonctionnelle non trouvée avec l'ID: " + request.getNatureLigneFonctionnelleId());
                });

        log.debug("NatureLigneFonctionnelle cible trouvée: {}", targetNature.getLabel());

        avoidDuplicateActifLines(targetBanque, targetNature);

        ligneFonctionnelleMapper.updateLigneFonctionnelleFromDto(request, existingLigneFonctionnelle, targetBanque, targetNature);

        LigneFonctionnelle updatedLigneFonctionnelle = ligneFonctionnelleRepository.save(existingLigneFonctionnelle);

        return ligneFonctionnelleMapper.toDto(updatedLigneFonctionnelle);

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

    public Page<LigneFonctionnelleResponse> getPaginatedLigneFonctionnelles(LigneFonctionnelleSearchCriteria criteria, Pageable pageable) {
        log.debug("Récupération des lignes fonctionnelles ACTIVES avec critères: {} et pageable: {}", criteria, pageable);
        Specification<LigneFonctionnelle> spec = LigneFonctionnelleSpecification.searchByCriteria(criteria);
        Page<LigneFonctionnelle> ligneFonctionnellesPage = ligneFonctionnelleRepository.findAll(spec, pageable);
        log.debug("Nombre de lignes fonctionnelles ACTIVES trouvées: {}", ligneFonctionnellesPage.getTotalElements());
        return ligneFonctionnellesPage.map(ligneFonctionnelleMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<LigneFonctionnelleResponse> getPaginatedLignesFonctionnellesByBanqueId(Long banqueId, LigneFonctionnelleSearchCriteria criteria, Pageable pageable) {
        log.debug("Récupération paginée et filtrée des lignes fonctionnelles pour Banque ID: {} avec critères: {} et pageable: {}", banqueId, criteria, pageable);

        if (!banqueRepository.existsById(banqueId)) {
            log.warn("Banque non trouvée avec l'ID: {}", banqueId);
            throw new ResourceNotFoundException("Banque non trouvée avec l'ID: " + banqueId + ". Impossible de récupérer les lignes fonctionnelles.");
        }

        criteria.setBanqueId(banqueId);

        Specification<LigneFonctionnelle> spec = LigneFonctionnelleSpecification.searchByCriteria(criteria);
        Page<LigneFonctionnelle> ligneFonctionnellesPage = ligneFonctionnelleRepository.findAll(spec, pageable);

        log.debug("Nombre de lignes fonctionnelles trouvées pour la banque {} avec filtres: {}", banqueId, ligneFonctionnellesPage.getTotalElements());
        return ligneFonctionnellesPage.map(ligneFonctionnelleMapper::toDto);
    }


    @Transactional
    public void deactivateLigneFonctionnelleByNatureAndBank(Long ligneId) {

        log.debug("Tentative de désactivation de la ligne fonctionnelle  avec l ID: {} ", ligneId);

        LigneFonctionnelle ligneToDeactivate = ligneFonctionnelleRepository.findById(ligneId).orElseThrow(
                ()->new ResourceNotFoundException("Aucune Ligne Fonctionnelle avec l'ID: " + ligneId)
        );

        ligneToDeactivate.setActif(false);
        ligneFonctionnelleRepository.save(ligneToDeactivate);
        log.info("Ligne fonctionnelle avec ID {}  désactivée avec succès.", ligneToDeactivate.getId());
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

    private void validateDateRange(OffsetDateTime dateDepart, OffsetDateTime dateFin) {
        if (dateDepart != null && dateFin != null && dateFin.isBefore(dateDepart)) {
            log.warn("Validation de date échouée: dateFin ({}) est antérieure à dateDepart ({}).", dateFin, dateDepart);
            throw new IllegalArgumentException("La date de fin (" + dateFin + ") ne peut pas être antérieure à la date de départ (" + dateDepart + ").");
        }
    }

    private void avoidDuplicateActifLines(Banque banque, NatureLigneFonctionnelle natureLigneFonctionnelle) {

        Optional<LigneFonctionnelle> existingActifLineWithSameBanqueAndNature = ligneFonctionnelleRepository.findByBanqueIdAndNatureLigneFonctionnelleIdAndActifTrue(banque.getId(), natureLigneFonctionnelle.getId());

        existingActifLineWithSameBanqueAndNature.ifPresent(existingActiveLine -> {
            log.warn("Tentative de création d'un doublon actif pour la combinaison: Banque '{}' (ID: {}), Nature '{}' (ID: {}).",
                    banque.getNom(), banque.getId(), natureLigneFonctionnelle.getLabel(), natureLigneFonctionnelle.getId());
            throw new DuplicateEntryException(
                    "Une ligne fonctionnelle ACTIVE de type '" + natureLigneFonctionnelle.getLabel() +
                            "' existe déjà pour la banque '" + banque.getNom() + "'."
            );
        });
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

        if (!banqueRepository.existsById(banqueId)) {
            log.warn("Banque non trouvée avec l'ID: {}", banqueId);
            throw new ResourceNotFoundException("Banque non trouvée avec l'ID: " + banqueId);
        }
        natureLigneFonctionnelleRepository.findById(natureLigneFonctionnelleId)
                .orElseThrow(() -> {
                    log.warn("NatureLigneFonctionnelle non trouvée avec l'ID: {}", natureLigneFonctionnelleId);
                    return new ResourceNotFoundException("NatureLigneFonctionnelle non trouvée avec l'ID: " + natureLigneFonctionnelleId);
                });
        log.debug("Banque et NatureLigneFonctionnelle validées.");

        List<LigneFonctionnelle> historicalLines = ligneFonctionnelleRepository.findByBanqueIdAndNatureLigneFonctionnelleIdOrderByDateDepartAsc(banqueId, natureLigneFonctionnelleId);

        if (historicalLines.isEmpty()) {
            log.info("Aucune ligne fonctionnelle historique trouvée pour Banque ID: {} et Nature ID: {}.", banqueId, natureLigneFonctionnelleId);
            return new LigneFonctionnelleChartData(new ArrayList<>(), new ArrayList<>());
        }

        // --- CORRECTED SECTION FOR LABELS AND DATA ---
        // Extract distinct years for labels using a stream and sort them
        List<String> labels = historicalLines.stream()
                .map(line -> String.valueOf(line.getDateDepart().getYear()))
                .distinct() // Ensure unique years
                .sorted()   // Sort the years
                .collect(Collectors.toList());

        // Extract taux data
        List<BigDecimal> tauxData = historicalLines.stream()
                .map(LigneFonctionnelle::getTaux)
                .collect(Collectors.toList());

        // Extract plafond data
        List<BigDecimal> plafondData = historicalLines.stream()
                .map(LigneFonctionnelle::getPlafond)
                .collect(Collectors.toList());
        // --- END CORRECTED SECTION ---

        LigneFonctionnelleChartData.ChartDataset tauxDataset = new LigneFonctionnelleChartData.ChartDataset(
                "Taux",
                tauxData,
                false,
                "var(--blue-500)",
                0.4
        );

        LigneFonctionnelleChartData.ChartDataset plafondDataset = new LigneFonctionnelleChartData.ChartDataset(
                "Plafond",
                plafondData,
                false,
                "var(--pink-500)",
                0.4
        );

        List<LigneFonctionnelleChartData.ChartDataset> datasets = new ArrayList<>();
        datasets.add(tauxDataset);
        datasets.add(plafondDataset);

        log.debug("Données historiques du graphique préparées: Labels={}, Taux Data={}, Plafond Data={}", labels, tauxData.size(), plafondData.size());
        return new LigneFonctionnelleChartData(labels, datasets);
    }



}