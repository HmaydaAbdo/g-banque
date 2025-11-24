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

    private static final Logger log = LoggerFactory.getLogger(LigneFonctionnelleService.class);
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

    // =========================================================================================
    // 1. CREATE
    // =========================================================================================

    @Transactional
    public LigneFonctionnelleResponse createLigneFonctionnelle(LigneFonctionnelleRequest request) {
        log.debug("Requête de création de ligne fonctionnelle: {}", request);

        // 1. Validations et Récupération des entités Mère
        validateDateRange(request.getDateDepart(), request.getDateFin());

        Banque banque = banqueRepository.findById(request.getBanqueId())
                .orElseThrow(() -> new ResourceNotFoundException("Banque non trouvée avec l'ID: " + request.getBanqueId()));

        NatureLigneFonctionnelle natureLigneFonctionnelle = natureLigneFonctionnelleRepository.findById(request.getNatureLigneFonctionnelleId())
                .orElseThrow(() -> new ResourceNotFoundException("NatureLigneFonctionnelle non trouvée avec l'ID: " + request.getNatureLigneFonctionnelleId()));

        // Validation métier pour la ligne Mère/Indépendante (Unicité)
        avoidDuplicateActifLines(banque, natureLigneFonctionnelle);

        // 2. Mappage de la Ligne Mère/Indépendante
        LigneFonctionnelle ligneFonctionnelle = ligneFonctionnelleMapper.toEntity(request, banque, natureLigneFonctionnelle);

        // 3. Gestion de la création en cascade des Sous-lignes
        if (request.getSousLignes() != null && !request.getSousLignes().isEmpty()) {

            // RÈGLE MÉTIER 1 : Validation de la Nature pour être Ligne Mère
            validateNatureForMere(natureLigneFonctionnelle);


            List<LigneFonctionnelle> sousLignesEntities = new ArrayList<>();

            for (SousLigneRequestDto sousLigneRequest : request.getSousLignes()) {

                if (sousLigneRequest.getId() != null) {
                    throw new IllegalArgumentException("L'ID de la sous-ligne doit être NULL lors de la création en cascade.");
                }

                NatureLigneFonctionnelle natureSousLigne = natureLigneFonctionnelleRepository.findById(sousLigneRequest.getNatureLigneFonctionnelleId())
                        .orElseThrow(() -> new ResourceNotFoundException("NatureLigneFonctionnelle pour Sous-ligne non trouvée avec l'ID: " + sousLigneRequest.getNatureLigneFonctionnelleId()));

                LigneFonctionnelle sousLigneEntity = ligneFonctionnelleMapper.toSousLigneEntity(
                        sousLigneRequest,
                        ligneFonctionnelle, // Ligne Mère (pour héritage et lien de retour)
                        natureSousLigne
                );

                sousLignesEntities.add(sousLigneEntity);
            }

            ligneFonctionnelle.setSousLignes(sousLignesEntities);
        }

        // 4. Sauvegarde de l'ensemble de la hiérarchie
        LigneFonctionnelle savedLigneFonctionnelle = ligneFonctionnelleRepository.save(ligneFonctionnelle);
        log.info("Ligne fonctionnelle créée avec succès, ID: {}", savedLigneFonctionnelle.getId());
        return ligneFonctionnelleMapper.toDto(savedLigneFonctionnelle);
    }

    // =========================================================================================
    // 2. READ (BY ID)
    // =========================================================================================

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

    // =========================================================================================
    // 3. READ (PAGINATION FILTRÉE - MÈRES/INDÉPENDANTES UNIQUEMENT)
    // =========================================================================================

    public Page<LigneFonctionnelleResponse> getPaginatedLigneFonctionnelles(LigneFonctionnelleSearchCriteria criteria, Pageable pageable) {
        log.debug("Récupération des lignes fonctionnelles de niveau Mère/Indépendante avec critères: {} et pageable: {}", criteria, pageable);

        Specification<LigneFonctionnelle> spec = LigneFonctionnelleSpecification.searchByCriteria(criteria);

        // Ajouter le critère pour exclure les sous-lignes (Ligne Mere IS NULL)
        Specification<LigneFonctionnelle> onlyParentsSpec = LigneFonctionnelleSpecification.isLigneMereOrIndependante();

        spec = spec.and(onlyParentsSpec);

        Page<LigneFonctionnelle> ligneFonctionnellesPage = ligneFonctionnelleRepository.findAll(spec, pageable);

        log.debug("Nombre de lignes fonctionnelles Mères/Indépendantes trouvées: {}", ligneFonctionnellesPage.getTotalElements());
        return ligneFonctionnellesPage.map(ligneFonctionnelleMapper::toDto);
    }

    // =========================================================================================
    // 4. READ (PAGINATION FILTRÉE PAR BANQUE - MÈRES/INDÉPENDANTES UNIQUEMENT)
    // =========================================================================================

    @Transactional(readOnly = true)
    public Page<LigneFonctionnelleResponse> getPaginatedLignesFonctionnellesByBanqueId(Long banqueId, LigneFonctionnelleSearchCriteria criteria, Pageable pageable) {
        log.debug("Récupération paginée et filtrée des lignes Mères/Indépendantes pour Banque ID: {} avec critères: {} et pageable: {}", banqueId, criteria, pageable);

        if (!banqueRepository.existsById(banqueId)) {
            log.warn("Banque non trouvée avec l'ID: {}", banqueId);
            throw new ResourceNotFoundException("Banque non trouvée avec l'ID: " + banqueId + ". Impossible de récupérer les lignes fonctionnelles.");
        }

        criteria.setBanqueId(banqueId);

        Specification<LigneFonctionnelle> spec = LigneFonctionnelleSpecification.searchByCriteria(criteria);

        // Ajouter le critère pour exclure les sous-lignes (Ligne Mere IS NULL)
        Specification<LigneFonctionnelle> onlyParentsSpec = LigneFonctionnelleSpecification.isLigneMereOrIndependante();

        spec = spec.and(onlyParentsSpec);

        Page<LigneFonctionnelle> ligneFonctionnellesPage = ligneFonctionnelleRepository.findAll(spec, pageable);

        log.debug("Nombre de lignes Mères/Indépendantes trouvées pour la banque {} avec filtres: {}", banqueId, ligneFonctionnellesPage.getTotalElements());
        return ligneFonctionnellesPage.map(ligneFonctionnelleMapper::toDto);
    }

    // =========================================================================================
    // 5. UPDATE (LIGNE MÈRE/INDÉPENDANTE)
    // =========================================================================================

    @Transactional
    public LigneFonctionnelleResponse updateLigneFonctionnelle(Long id, LigneFonctionnelleRequest request) {
        log.debug("--- Début de la mise à jour de LigneFonctionnelle (ID: {}) ---", id);

        LigneFonctionnelle existingLigneFonctionnelle = ligneFonctionnelleRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Ligne Fonctionnelle non trouvée avec l'ID: {} pour la mise à jour.", id);
                    return new ResourceNotFoundException("Ligne Fonctionnelle non trouvée avec l'ID: " + id);
                });

        validateDateRange(request.getDateDepart(), request.getDateFin());

        Banque targetBanque = banqueRepository.findById(request.getBanqueId())
                .orElseThrow(() -> new ResourceNotFoundException("Banque non trouvée avec l'ID: " + request.getBanqueId()));

        NatureLigneFonctionnelle targetNature = natureLigneFonctionnelleRepository.findById(request.getNatureLigneFonctionnelleId())
                .orElseThrow(() -> new ResourceNotFoundException("NatureLigneFonctionnelle non trouvée avec l'ID: " + request.getNatureLigneFonctionnelleId()));

        // GESTION DU PLAFOND ET DE LA NATURE SI C'EST UNE LIGNE MÈRE
        if (existingLigneFonctionnelle.getSousLignes() != null && !existingLigneFonctionnelle.getSousLignes().isEmpty()) {

            // RÈGLE MÉTIER 1 : Assurer que la nouvelle nature permet d'avoir des Sous-lignes
            validateNatureForMere(targetNature);

            // RÈGLE MÉTIER 2 : Vérification du Plafond contre les enfants existants
            BigDecimal existingChildrenTotalPlafond = existingLigneFonctionnelle.getSousLignes().stream()
                    .map(s -> Objects.requireNonNullElse(s.getPlafond(), BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);



            if (request.getPlafond().compareTo(existingChildrenTotalPlafond) < 0) {
                log.warn("Validation de plafond échouée: Le nouveau plafond de la Mère ({}) est inférieur à la somme des plafonds des Sous-lignes existantes ({}).",
                        request.getPlafond(), existingChildrenTotalPlafond);
                throw new IllegalArgumentException(
                        "Le nouveau plafond de la ligne mère (" + request.getPlafond() +
                                ") ne peut pas être inférieur à la somme des plafonds de ses sous-lignes existantes (" + existingChildrenTotalPlafond + ")."
                );
            }
        }

        // Règle d'Unicité (si la clé change)
        if (existingLigneFonctionnelle.isActif() && (
                !existingLigneFonctionnelle.getBanque().getId().equals(targetBanque.getId()) ||
                        !existingLigneFonctionnelle.getNatureLigneFonctionnelle().getId().equals(targetNature.getId()))
        ) {
            avoidDuplicateActifLines(targetBanque, targetNature);
        }

        // Mappage des propriétés de la Mère
        ligneFonctionnelleMapper.updateLigneFonctionnelleFromDto(request, existingLigneFonctionnelle, targetBanque, targetNature);

        // Mise à jour en cascade des propriétés héritées sur les enfants existants (Taux, Dates, Banque, etc.)
        if (existingLigneFonctionnelle.getSousLignes() != null) {
            for (LigneFonctionnelle sousLigne : existingLigneFonctionnelle.getSousLignes()) {
                sousLigne.setTaux(existingLigneFonctionnelle.getTaux());
                sousLigne.setBanque(existingLigneFonctionnelle.getBanque());
                sousLigne.setDateDepart(existingLigneFonctionnelle.getDateDepart());
                sousLigne.setDateFin(existingLigneFonctionnelle.getDateFin());
            }
        }

        LigneFonctionnelle updatedLigneFonctionnelle = ligneFonctionnelleRepository.save(existingLigneFonctionnelle);
        log.info("Ligne fonctionnelle (Mère) ID {} mise à jour avec succès.", id);
        return ligneFonctionnelleMapper.toDto(updatedLigneFonctionnelle);
    }


    // =========================================================================================
    // 6. MANAGE SOUS-LIGNES (CRUD sur la collection d'enfants)
    // =========================================================================================

    @Transactional
    public LigneFonctionnelleResponse manageSousLignes(Long ligneMereId, List<SousLigneRequestDto> sousLignesRequests) {
        log.debug("Début de la gestion des sous-lignes pour la ligne mère ID: {}", ligneMereId);

        // 1. Chargement de la Ligne Mère
        LigneFonctionnelle ligneMere = ligneFonctionnelleRepository.findById(ligneMereId)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne Mère non trouvée avec l'ID: " + ligneMereId));

        // 2. Validations préalables
        if (ligneMere.getLigneMere() != null) {
            throw new IllegalArgumentException("L'ID fourni correspond à une Sous-ligne. Seule une Ligne Mère peut gérer ses enfants.");
        }
        // RÈGLE MÉTIER 1 : Assurer que la nature de la Mère permet d'avoir des Sous-lignes
        validateNatureForMere(ligneMere.getNatureLigneFonctionnelle());




        // 3. Préparation pour la gestion de la cascade (Modification, Création, Suppression)
        Map<Long, LigneFonctionnelle> existingSousLignesMap = ligneMere.getSousLignes().stream()
                .collect(Collectors.toMap(LigneFonctionnelle::getId, Function.identity()));

        List<LigneFonctionnelle> updatedSousLignes = new ArrayList<>();

        // Propriétés héritées (elles sont les plus à jour suite à l'appel de updateLigneFonctionnelle)
        Banque mereBanque = ligneMere.getBanque();
        BigDecimal mereTaux = ligneMere.getTaux();
        OffsetDateTime mereDateDepart = ligneMere.getDateDepart();
        OffsetDateTime mereDateFin = ligneMere.getDateFin();


        // 4. Traitement des Sous-lignes de la Requête
        for (SousLigneRequestDto sousLigneRequest : sousLignesRequests) {

            LigneFonctionnelle currentSousLigne;

            NatureLigneFonctionnelle natureSousLigne = natureLigneFonctionnelleRepository.findById(sousLigneRequest.getNatureLigneFonctionnelleId())
                    .orElseThrow(() -> new ResourceNotFoundException("NatureLigneFonctionnelle pour Sous-ligne non trouvée avec l'ID: " + sousLigneRequest.getNatureLigneFonctionnelleId()));

            // Cas A: Mise à jour (ID présent)
            if (sousLigneRequest.getId() != null) {
                currentSousLigne = existingSousLignesMap.get(sousLigneRequest.getId());
                if (currentSousLigne == null) {
                    throw new ResourceNotFoundException("Sous-ligne non trouvée ou n'appartient pas à la ligne Mère: " + sousLigneRequest.getId());
                }

                // Mise à jour des propriétés spécifiques et héritées
                updateSousLigneProperties(
                        currentSousLigne, sousLigneRequest, natureSousLigne,
                        mereBanque, mereTaux, mereDateDepart, mereDateFin
                );
            }
            // Cas B: Création (ID NULL)
            else {
                currentSousLigne = ligneFonctionnelleMapper.toSousLigneEntity(
                        sousLigneRequest,
                        ligneMere, // Ligne Mère (pour héritage et lien)
                        natureSousLigne
                );
            }

            updatedSousLignes.add(currentSousLigne);
        }

        // 5. Gestion de la Suppression (Orphelins) et Réaffectation
        // L'entité LigneMere est mise à jour. Grâce à `orphanRemoval=true` dans l'entité mère,
        // les sous-lignes retirées de la collection sont supprimées de la base.
        ligneMere.getSousLignes().clear();
        ligneMere.getSousLignes().addAll(updatedSousLignes);


        // 6. Sauvegarde et Retour
        LigneFonctionnelle updatedLigneMere = ligneFonctionnelleRepository.save(ligneMere);
        log.info("Gestion des sous-lignes pour Ligne Mère ID {} terminée. {} sous-lignes persistées.", ligneMereId, updatedSousLignes.size());

        return ligneFonctionnelleMapper.toDto(updatedLigneMere);
    }

    // =========================================================================================
    // 7. DEACTIVATE
    // =========================================================================================

    @Transactional
    public void deactivateLigneFonctionnelle(Long ligneId) {
        log.debug("Tentative de désactivation de la ligne fonctionnelle avec l'ID: {}", ligneId);

        LigneFonctionnelle ligneToDeactivate = ligneFonctionnelleRepository.findById(ligneId)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune Ligne Fonctionnelle avec l'ID: " + ligneId));

        // RÈGLE MÉTIER : Empêcher la désactivation directe des Sous-lignes
        if (ligneToDeactivate.getLigneMere() != null) {
            log.warn("Tentative de désactivation d'une Sous-ligne (ID: {}). Action non autorisée.", ligneId);
            throw new IllegalArgumentException(
                    "La désactivation directe des Sous-lignes n'est pas autorisée. Veuillez désactiver la Ligne Mère (ID: " + ligneToDeactivate.getLigneMere().getId() + ") pour affecter tous les enfants."
            );
        }

        if (!ligneToDeactivate.isActif()) {
            log.info("Ligne fonctionnelle ID {} est déjà inactive. Aucune action nécessaire.", ligneId);
            return;
        }

        // Désactivation de la Ligne Mère/Indépendante
        ligneToDeactivate.setActif(false);
        log.info("Ligne Mère/Indépendante ID {} désactivée.", ligneId);

        // Désactivation des Sous-lignes en cascade
        if (ligneToDeactivate.getSousLignes() != null && !ligneToDeactivate.getSousLignes().isEmpty()) {

            for (LigneFonctionnelle sousLigne : ligneToDeactivate.getSousLignes()) {
                if (sousLigne.isActif()) {
                    sousLigne.setActif(false);
                }
            }
        }

        ligneFonctionnelleRepository.save(ligneToDeactivate);
        log.info("Ligne fonctionnelle ID {} et ses Sous-lignes ont été désactivées avec succès.", ligneId);
    }

    // =========================================================================================
    // 8. DELETE
    // =========================================================================================

    @Transactional
    public void deleteLigneFonctionnelle(Long id) {
        log.debug("Requête de suppression de ligne fonctionnelle pour ID: {}", id);

        LigneFonctionnelle ligneToDelete = ligneFonctionnelleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne Fonctionnelle non trouvée avec l'ID: " + id));

        // RÈGLE MÉTIER : Empêcher la suppression directe d'une Sous-ligne
        if (ligneToDelete.getLigneMere() != null) {
            log.warn("Tentative de suppression directe d'une Sous-ligne (ID: {}). Action non autorisée.", id);
            throw new IllegalArgumentException(
                    "La suppression directe des Sous-lignes n'est pas autorisée. Veuillez supprimer la Ligne Mère (ID: " + ligneToDelete.getLigneMere().getId() + ") pour supprimer tous les enfants en cascade."
            );
        }

        try {
            ligneFonctionnelleRepository.delete(ligneToDelete); // Supprime la Mère et les enfants via cascade
            log.info("Ligne fonctionnelle avec ID {} (et ses Sous-lignes) supprimée avec succès.", id);
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de la ligne fonctionnelle ID {}.", id, e);
            throw new RuntimeException("Échec de la suppression de la ligne fonctionnelle et de ses dépendances.", e);
        }
    }


    // =========================================================================================
    // 9. CHART HISTORY
    // =========================================================================================

    @Transactional(readOnly = true)
    public LigneFonctionnelleChartData getHistoricalChartData(Long banqueId, Long natureLigneFonctionnelleId) {
        log.debug("Récupération des données historiques pour le graphique pour Banque ID: {} et Nature ID: {}", banqueId, natureLigneFonctionnelleId);

        if (!banqueRepository.existsById(banqueId)) {
            throw new ResourceNotFoundException("Banque non trouvée avec l'ID: " + banqueId);
        }
        natureLigneFonctionnelleRepository.findById(natureLigneFonctionnelleId)
                .orElseThrow(() -> new ResourceNotFoundException("NatureLigneFonctionnelle non trouvée avec l'ID: " + natureLigneFonctionnelleId));

        List<LigneFonctionnelle> historicalLines = ligneFonctionnelleRepository.findByBanqueIdAndNatureLigneFonctionnelleIdOrderByDateDepartAsc(banqueId, natureLigneFonctionnelleId);

        if (historicalLines.isEmpty()) {
            log.info("Aucune ligne fonctionnelle historique trouvée.");
            return new LigneFonctionnelleChartData(new ArrayList<>(), new ArrayList<>());
        }

        // Extraction des données pour le graphique
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

        log.debug("Données historiques du graphique préparées: Labels={}, Taux Data={}, Plafond Data={}", labels, tauxData.size(), plafondData.size());
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
            log.warn("Validation de nature échouée: La nature '{}' ne peut pas être une ligne mère.", nature.getLabel());
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
            log.warn("Validation de date échouée: dateFin ({}) est antérieure à dateDepart ({}).", dateFin, dateDepart);
            throw new IllegalArgumentException("La date de fin ne peut pas être antérieure à la date de départ.");
        }
    }

    private void avoidDuplicateActifLines(Banque banque, NatureLigneFonctionnelle natureLigneFonctionnelle) {

        Optional<LigneFonctionnelle> existingActifLineWithSameBanqueAndNature = ligneFonctionnelleRepository.findByBanqueIdAndNatureLigneFonctionnelleIdAndActifTrue(banque.getId(), natureLigneFonctionnelle.getId());

        existingActifLineWithSameBanqueAndNature.ifPresent(existingActiveLine -> {
            log.warn("Tentative de création d'un doublon actif pour la combinaison: Banque '{}', Nature '{}'.",
                    banque.getNom(), natureLigneFonctionnelle.getLabel());
            throw new DuplicateEntryException(
                    "Une ligne fonctionnelle ACTIVE de type '" + natureLigneFonctionnelle.getLabel() +
                            "' existe déjà pour la banque '" + banque.getNom() + "'."
            );
        });
    }
}