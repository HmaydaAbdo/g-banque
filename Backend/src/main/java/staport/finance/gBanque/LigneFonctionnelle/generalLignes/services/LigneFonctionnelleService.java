package staport.finance.gBanque.LigneFonctionnelle.generalLignes.services;

import com.lowagie.text.Chunk;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.dtos.*;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.mappers.LigneFonctionnelleMapper;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.repositories.LigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.specifications.LigneFonctionnelleSpecification;
import staport.finance.gBanque.reports.service.StaportPdfReportService;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.enums.PorteeNature;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.services.LigneFonctionnelleSpecifiqueReader;
import staport.finance.gBanque.droitConstate.dtos.response.AgregatedSuivieDcLigneResponseDto;
import staport.finance.gBanque.moyensDePaiements.lcn.dtos.AgregatedSuivieLcnLigneResponseDto;
import staport.finance.gBanque.droitConstate.dtos.response.SuivieDcLigneResponseDto;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.droitConstate.mapper.DroitConstateMapper;
import staport.finance.gBanque.droitConstate.service.DroitConstateReader;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import staport.finance.gBanque.factoring.dtos.response.AgregatedSuivieFactoringLigneResponseDto;
import staport.finance.gBanque.factoring.dtos.response.SuivieFactoringLineResponseDto;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.factoring.mapper.FactoringMapper;
import staport.finance.gBanque.factoring.service.FactoringReader;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.service.IMarcheService;
import staport.finance.gBanque.moyensDePaiements.lcn.dtos.LcnResponseDTO;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.lcn.mapper.LcnMapper;
import staport.finance.gBanque.moyensDePaiements.lcn.services.LcnReader;
import staport.finance.gBanque.moyensDePaiements.lcn.specs.LcnSpecification;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.prefinancement.dtos.response.AgregatedSuiviePrefiLigneResponseDto;
import staport.finance.gBanque.prefinancement.dtos.response.SuiviePrefiLigneResponseDto;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.mapper.PrefinancementMapper;
import staport.finance.gBanque.prefinancement.services.PrefinancementReader;
import staport.finance.gBanque.reports.utils.MontantFormatter;


@Service
public class LigneFonctionnelleService implements ILigneFonctionnelleService{

    private static final Logger log = LoggerFactory.getLogger(LigneFonctionnelleService.class);
    // Constante de Règle Métier : Seule cette nature peut être une Ligne Mère
    private static final String CAUTION_ADMINISTRATIVE_CODE = "CAUT_ADMIN";
    private static final String CAUTION_ADMINISTRATIVE_LABEL = "Caution Administrative";

    private final LigneFonctionnelleRepository ligneFonctionnelleRepository;
    private final LigneFonctionnelleMapper ligneFonctionnelleMapper;
    private final IMarcheService marcheService;
    private final LigneFonctionnelleSpecifiqueReader ligneFonctionnelleSpecifiqueReader;
    private final PrefinancementReader prefinancementReader;
    private final FactoringReader factoringReader;
    private final DroitConstateReader droitConstateReader;
    private final PrefinancementMapper prefinancementMapper;
    private final LcnReader lcnReader;
    private final LcnMapper lcnMapper;
    private final FactoringMapper factoringMapper;
    private final DroitConstateMapper droitConstateMapper;
    private final StaportPdfReportService staportPdfReportService;

    @Autowired
    public LigneFonctionnelleService(LigneFonctionnelleRepository ligneFonctionnelleRepository,
                                     LigneFonctionnelleMapper ligneFonctionnelleMapper,
                                     IMarcheService marcheService, LigneFonctionnelleSpecifiqueReader ligneFonctionnelleSpecifiqueReader, PrefinancementReader prefinancementReader, FactoringReader factoringReader, DroitConstateReader droitConstateReader, PrefinancementMapper prefinancementMapper, LcnReader lcnReader, LcnMapper lcnMapper, FactoringMapper factoringMapper, DroitConstateMapper droitConstateMapper, StaportPdfReportService staportPdfReportService) {
        this.ligneFonctionnelleRepository = ligneFonctionnelleRepository;
        this.ligneFonctionnelleMapper = ligneFonctionnelleMapper;
        this.marcheService = marcheService;
        this.ligneFonctionnelleSpecifiqueReader = ligneFonctionnelleSpecifiqueReader;
        this.prefinancementReader = prefinancementReader;
        this.factoringReader = factoringReader;
        this.droitConstateReader = droitConstateReader;
        this.prefinancementMapper = prefinancementMapper;
        this.lcnReader = lcnReader;
        this.lcnMapper = lcnMapper;
        this.factoringMapper = factoringMapper;
        this.droitConstateMapper = droitConstateMapper;
        this.staportPdfReportService = staportPdfReportService;
    }


    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = LigneFonctionnelle.class)
    public LigneFonctionnelleResponse createLigneFonctionnelle(
            LigneFonctionnelleRequest request, Banque banque, NatureLigneFonctionnelle natureLigneFonctionnelle,
            Map<Long, NatureLigneFonctionnelle> sousLigneNatures) {

        log.debug("Creating ligne fonctionnelle, Full creation request: {}", request);

        // 1. Validation de la plage de dates
        validateDateRange(request.dateDepart(), request.dateFin());

        log.debug("Found banque: {} (ID: {})", banque.getNom(), banque.getId());
        log.debug("Found nature: {} (ID: {})", natureLigneFonctionnelle.getLabel(), natureLigneFonctionnelle.getId());

        // Validation portée : une ligne générale n'accepte que GENERALE ou GENERALE_ET_SPECIFIQUE
        validatePorteeForLigneGenerale(natureLigneFonctionnelle);

        //  4 : Récupération et validation des Marchés (optionnel)
        List<Marche> marches = validateAndGetMarches(request.marcheIds(), request.banqueId());

        if (!marches.isEmpty()) {
            log.debug("Found {} marché(s): {}",
                    marches.size(),
                    marches.stream().map(Marche::getNumero).collect(Collectors.joining(", ")));
        } else {
            log.debug("No marche specified - creating general ligne fonctionnelle");
        }

        // 5 : Vérification des doublons avec les marchés
        avoidDuplicateActifLines(banque, natureLigneFonctionnelle, marches);

        //  6 : Création de l'entité avec les marchés
        LigneFonctionnelle ligneFonctionnelle = ligneFonctionnelleMapper.toEntity(
                request, banque, natureLigneFonctionnelle, marches
        );

        // 7. Gestion des sous-lignes (inchangé)
        if (request.sousLignes() != null && !request.sousLignes().isEmpty()) {
            log.info("Processing {} sous-ligne(s) for creation", request.sousLignes().size());

            validateNatureForMere(natureLigneFonctionnelle);

            List<LigneFonctionnelle> sousLignesEntities = new ArrayList<>();

            for (SousLigneRequestDto sousLigneRequest : request.sousLignes()) {
                log.debug("Processing sous-ligne - Nature ID: {}, Plafond: {}",
                        sousLigneRequest.natureLigneFonctionnelleId(), sousLigneRequest.plafond());

                if (sousLigneRequest.id() != null) {
                    log.error("Sous-ligne ID must be NULL during cascade creation, but got: {}", sousLigneRequest.id());
                    throw new IllegalArgumentException("L'ID de la sous-ligne doit être NULL lors de la création en cascade.");
                }

                NatureLigneFonctionnelle natureSousLigne = sousLigneNatures.get(sousLigneRequest.natureLigneFonctionnelleId());
                if (natureSousLigne == null) {
                    log.error("NatureLigneFonctionnelle for sous-ligne not found with ID: {}", sousLigneRequest.natureLigneFonctionnelleId());
                    throw new ResourceNotFoundException("NatureLigneFonctionnelle pour Sous-ligne non trouvée avec l'ID: " + sousLigneRequest.natureLigneFonctionnelleId());
                }

                LigneFonctionnelle sousLigneEntity = ligneFonctionnelleMapper.toSousLigneEntity(
                        sousLigneRequest,
                        ligneFonctionnelle,
                        natureSousLigne
                );

                sousLignesEntities.add(sousLigneEntity);
            }

            ligneFonctionnelle.setSousLignes(sousLignesEntities);
            log.debug("Successfully prepared {} sous-ligne(s) for cascade creation", sousLignesEntities.size());
        }

        // 8. Sauvegarde
        LigneFonctionnelle savedLigneFonctionnelle = ligneFonctionnelleRepository.save(ligneFonctionnelle);
        log.info("Successfully created ligne fonctionnelle with ID: {}, Marchés: [{}], Has {} sous-ligne(s)",
                savedLigneFonctionnelle.getId(),
                marches.stream().map(Marche::getNumero).collect(Collectors.joining(", ")),
                savedLigneFonctionnelle.getSousLignes() != null ? savedLigneFonctionnelle.getSousLignes().size() : 0);

        return ligneFonctionnelleMapper.toDto(savedLigneFonctionnelle);
    }


    @Override
    @Transactional(readOnly = true)
    public LigneFonctionnelleResponse getLigneFonctionnelleById(Long id) {
        log.info("Fetching ligne fonctionnelle by ID: {}", id);

        LigneFonctionnelle ligneFonctionnelle = ligneFonctionnelleRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Ligne Fonctionnelle not found with ID: {}", id);
                    return new ResourceNotFoundException("Ligne Fonctionnelle non trouvée avec l'ID: " + id);
                });

        log.debug("Successfully retrieved ligne fonctionnelle ID: {}, Nature: {}, Plafond: {}",
                ligneFonctionnelle.getId(),
                ligneFonctionnelle.getNatureLigneFonctionnelle().getLabel(),
                ligneFonctionnelle.getPlafond());

        return ligneFonctionnelleMapper.toDto(ligneFonctionnelle);
    }

    // =========================================================================================
    // 3. READ (PAGINATION FILTRÉE - MÈRES/INDÉPENDANTES UNIQUEMENT)
    // =========================================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<LigneFonctionnelleResponse> getPaginatedLigneFonctionnelles(LigneFonctionnelleSearchCriteria criteria, Pageable pageable) {
        log.info("Fetching paginated lignes fonctionnelles (Mère/Indépendante only) - Page: {}, Size: {}", pageable.getPageNumber(), pageable.getPageSize());
        log.debug("Search criteria: {}", criteria);

        Specification<LigneFonctionnelle> spec = LigneFonctionnelleSpecification.searchByCriteria(criteria);
        Specification<LigneFonctionnelle> onlyParentsSpec = LigneFonctionnelleSpecification.isLigneMereOrIndependante();
        spec = spec.and(onlyParentsSpec);

        Page<LigneFonctionnelle> ligneFonctionnellesPage = ligneFonctionnelleRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.info("Retrieved {} ligne(s) fonctionnelle(s) Mère/Indépendante out of {} total (Page {}/{})",
                ligneFonctionnellesPage.getNumberOfElements(),
                ligneFonctionnellesPage.getTotalElements(),
                ligneFonctionnellesPage.getNumber() + 1,
                ligneFonctionnellesPage.getTotalPages());

        return ligneFonctionnellesPage.map(ligneFonctionnelleMapper::toDto);
    }
    // =========================================================================================
    // 4. READ (PAGINATION FILTRÉE PAR BANQUE - MÈRES/INDÉPENDANTES UNIQUEMENT)
    // =========================================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<LigneFonctionnelleResponse> getPaginatedLignesFonctionnellesByBanqueId(Long banqueId, LigneFonctionnelleSearchCriteria criteria, Pageable pageable) {
        log.info("Fetching paginated lignes fonctionnelles for banque ID: {} - Page: {}, Size: {}",
                banqueId, pageable.getPageNumber(), pageable.getPageSize());
        log.debug("Search criteria for banque {}: {}", banqueId, criteria);

        // Existence de la banque validée par le Controller (IBanqueService.findOrThrow) avant l'appel.
        criteria = criteria.withBanqueId(banqueId);

        Specification<LigneFonctionnelle> spec = LigneFonctionnelleSpecification.searchByCriteria(criteria);
        Specification<LigneFonctionnelle> onlyParentsSpec = LigneFonctionnelleSpecification.isLigneMereOrIndependante();
        spec = spec.and(onlyParentsSpec);

        Page<LigneFonctionnelle> ligneFonctionnellesPage = ligneFonctionnelleRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.info("Retrieved {} ligne(s) Mère/Indépendante for banque ID: {} out of {} total (Page {}/{})",
                ligneFonctionnellesPage.getNumberOfElements(),
                banqueId,
                ligneFonctionnellesPage.getTotalElements(),
                ligneFonctionnellesPage.getNumber() + 1,
                ligneFonctionnellesPage.getTotalPages());

        return ligneFonctionnellesPage.map(ligneFonctionnelleMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LigneFonctionnelleResponse> getPaginatedLignesFonctionnellesByMarcheId(
            Long marcheId,
            LigneFonctionnelleSearchCriteria criteria,
            Pageable pageable) {

        log.info("Fetching paginated lignes fonctionnelles for marche ID: {} - Page: {}, Size: {}",
                marcheId, pageable.getPageNumber(), pageable.getPageSize());
        log.debug("Search criteria for marche {}: {}", marcheId, criteria);

        // Vérifier que le marché existe
        if (!marcheService.existsById(marcheId)) {
            log.error("Marche not found with ID: {} while fetching lignes fonctionnelles", marcheId);
            throw new ResourceNotFoundException("Marché non trouvé avec l'ID: " + marcheId + ". Impossible de récupérer les lignes fonctionnelles.");
        }

        // Ajouter le marcheId aux critères de recherche
        criteria = criteria.withMarcheId(marcheId);

        // Construire la spécification avec les critères
        Specification<LigneFonctionnelle> spec = LigneFonctionnelleSpecification.searchByCriteria(criteria);

        // Filtrer uniquement les lignes Mère/Indépendante (pas les sous-lignes)
        Specification<LigneFonctionnelle> onlyParentsSpec = LigneFonctionnelleSpecification.isLigneMereOrIndependante();
        spec = spec.and(onlyParentsSpec);

        Page<LigneFonctionnelle> ligneFonctionnellesPage = ligneFonctionnelleRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.info("Retrieved {} ligne(s) Mère/Indépendante for marche ID: {} out of {} total (Page {}/{})",
                ligneFonctionnellesPage.getNumberOfElements(),
                marcheId,
                ligneFonctionnellesPage.getTotalElements(),
                ligneFonctionnellesPage.getNumber() + 1,
                ligneFonctionnellesPage.getTotalPages());

        return ligneFonctionnellesPage.map(ligneFonctionnelleMapper::toDto);
    }
    // =========================================================================================
    // 5. UPDATE (LIGNE MÈRE/INDÉPENDANTE)
    // =========================================================================================
    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = LigneFonctionnelle.class)
    public LigneFonctionnelleResponse updateLigneFonctionnelle(
            Long id, LigneFonctionnelleRequest request, Banque targetBanque, NatureLigneFonctionnelle targetNature,
            Map<Long, NatureLigneFonctionnelle> sousLigneNatures) {

        log.info("Updating ligne fonctionnelle ID: {} - New plafond: {}, New taux: {}",
                id, request.plafond(), request.taux());
        log.debug("Full update request for ID {}: {}", id, request);

        // 1. Récupération de la ligne existante
        LigneFonctionnelle existingLigneFonctionnelle = ligneFonctionnelleRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Ligne Fonctionnelle not found with ID: {} for update", id);
                    return new ResourceNotFoundException("Ligne Fonctionnelle non trouvée avec l'ID: " + id);
                });

        log.debug("Found existing ligne fonctionnelle: ID {}, Current plafond: {}, Current marchés: [{}], Has {} sous-ligne(s)",
                existingLigneFonctionnelle.getId(),
                existingLigneFonctionnelle.getPlafond(),
                existingLigneFonctionnelle.getMarches().stream().map(Marche::getNumero).collect(Collectors.joining(", ")),
                existingLigneFonctionnelle.getSousLignes() != null ? existingLigneFonctionnelle.getSousLignes().size() : 0);

        // 2. Validation de la plage de dates
        validateDateRange(request.dateDepart(), request.dateFin());

        // Validation portée : une ligne générale n'accepte que GENERALE ou GENERALE_ET_SPECIFIQUE
        validatePorteeForLigneGenerale(targetNature);

        // 5. Récupération et validation des marchés cibles (optionnel)
        List<Marche> targetMarches = validateAndGetMarches(request.marcheIds(), request.banqueId());

        if (!targetMarches.isEmpty()) {
            log.debug("Target marchés: [{}]",
                    targetMarches.stream().map(Marche::getNumero).collect(Collectors.joining(", ")));
        } else {
            log.debug("No target marche - ligne will be/remain general");
        }

        // 6. Validation pour les lignes mères
        if (existingLigneFonctionnelle.getSousLignes() != null && !existingLigneFonctionnelle.getSousLignes().isEmpty()) {
            log.debug("Ligne mère has {} sous-ligne(s), validating nature",
                    existingLigneFonctionnelle.getSousLignes().size());
            validateNatureForMere(targetNature);
        }

        // 7. Vérification des doublons si clé modifiée (banque, nature OU marchés)
        if (existingLigneFonctionnelle.isActif() && hasKeyChanged(existingLigneFonctionnelle, targetBanque, targetNature, targetMarches)) {
            log.debug("Key changed (banque, nature, or marchés), checking for duplicate active lines");
            avoidDuplicateActifLinesForUpdate(id, targetBanque, targetNature, targetMarches);
        }

        // 8. Mise à jour de la ligne avec les marchés
        ligneFonctionnelleMapper.updateLigneFonctionnelleFromDto(
                request,
                existingLigneFonctionnelle,
                targetBanque,
                targetNature,
                targetMarches
        );

        // 9. Propagation des propriétés héritées aux sous-lignes
        if (existingLigneFonctionnelle.getSousLignes() != null && !existingLigneFonctionnelle.getSousLignes().isEmpty()) {
            log.debug("Propagating inherited properties to {} sous-ligne(s)",
                    existingLigneFonctionnelle.getSousLignes().size());
            for (LigneFonctionnelle sousLigne : existingLigneFonctionnelle.getSousLignes()) {
                sousLigne.setTaux(existingLigneFonctionnelle.getTaux());
                sousLigne.setBanque(existingLigneFonctionnelle.getBanque());
                sousLigne.setMarches(new ArrayList<>(existingLigneFonctionnelle.getMarches()));
                sousLigne.setDateDepart(existingLigneFonctionnelle.getDateDepart());
                sousLigne.setDateFin(existingLigneFonctionnelle.getDateFin());
            }
        }

        // 10. Sauvegarde
        LigneFonctionnelle updatedLigneFonctionnelle = ligneFonctionnelleRepository.save(existingLigneFonctionnelle);
        log.info("Successfully updated ligne fonctionnelle ID: {}", id);

        // Only the supported mother nature manages children. Null means no child edit;
        // an empty list explicitly removes all children of a supported mother.
        if (request.sousLignes() != null
                && supportsSousLignes(targetNature)) {
            return manageSousLignes(id, request.sousLignes(), sousLigneNatures);
        }
        if (request.sousLignes() != null && !request.sousLignes().isEmpty()) {
            throw new IllegalArgumentException("Cette nature de ligne ne peut pas contenir de sous-lignes.");
        }

        return ligneFonctionnelleMapper.toDto(updatedLigneFonctionnelle);
    }


    // =========================================================================================
    // 6. MANAGE SOUS-LIGNES (CRUD sur la collection d'enfants)
    // =========================================================================================

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = LigneFonctionnelle.class)
    public LigneFonctionnelleResponse manageSousLignes(
            Long ligneMereId, List<SousLigneRequestDto> sousLignesRequests,
            Map<Long, NatureLigneFonctionnelle> sousLigneNatures) {
        log.info("Managing sous-lignes for ligne mère ID: {} - {} sous-ligne(s) in request",
                ligneMereId, sousLignesRequests.size());
        log.debug("Sous-lignes requests: {}", sousLignesRequests);

        LigneFonctionnelle ligneMere = ligneFonctionnelleRepository.findById(ligneMereId)
                .orElseThrow(() -> {
                    log.error("Ligne Mère not found with ID: {}", ligneMereId);
                    return new ResourceNotFoundException("Ligne Mère non trouvée avec l'ID: " + ligneMereId);
                });

        if (ligneMere.getLigneMere() != null) {
            log.error("ID {} corresponds to a sous-ligne, cannot manage children", ligneMereId);
            throw new IllegalArgumentException("L'ID fourni correspond à une Sous-ligne. Seule une Ligne Mère peut gérer ses enfants.");
        }

        validateNatureForMere(ligneMere.getNatureLigneFonctionnelle());

        Map<Long, LigneFonctionnelle> existingSousLignesMap = ligneMere.getSousLignes().stream()
                .collect(Collectors.toMap(LigneFonctionnelle::getId, Function.identity()));

        log.debug("Existing sous-lignes count: {}", existingSousLignesMap.size());

        List<LigneFonctionnelle> updatedSousLignes = new ArrayList<>();

        Banque mereBanque = ligneMere.getBanque();
        BigDecimal mereTaux = ligneMere.getTaux();
        LocalDate mereDateDepart = ligneMere.getDateDepart();
        LocalDate mereDateFin = ligneMere.getDateFin();
        List<Marche> mereMarches = ligneMere.getMarches();


        int updateCount = 0;
        int createCount = 0;

        for (SousLigneRequestDto sousLigneRequest : sousLignesRequests) {
            LigneFonctionnelle currentSousLigne;

            NatureLigneFonctionnelle natureSousLigne = sousLigneNatures.get(sousLigneRequest.natureLigneFonctionnelleId());
            if (natureSousLigne == null) {
                log.error("Nature for sous-ligne not found with ID: {}", sousLigneRequest.natureLigneFonctionnelleId());
                throw new ResourceNotFoundException("NatureLigneFonctionnelle pour Sous-ligne non trouvée avec l'ID: " + sousLigneRequest.natureLigneFonctionnelleId());
            }

            if (sousLigneRequest.id() != null) {
                log.debug("Updating existing sous-ligne ID: {}", sousLigneRequest.id());
                currentSousLigne = existingSousLignesMap.get(sousLigneRequest.id());
                if (currentSousLigne == null) {
                    log.error("Sous-ligne ID {} not found or doesn't belong to ligne mère {}", sousLigneRequest.id(), ligneMereId);
                    throw new ResourceNotFoundException("Sous-ligne non trouvée ou n'appartient pas à la ligne Mère: " + sousLigneRequest.id());
                }

                updateSousLigneProperties(
                        currentSousLigne, sousLigneRequest, natureSousLigne,
                        mereBanque, mereTaux, mereDateDepart, mereDateFin, mereMarches
                );
                updateCount++;
            } else {
                log.debug("Creating new sous-ligne with nature ID: {}, plafond: {}",
                        sousLigneRequest.natureLigneFonctionnelleId(), sousLigneRequest.plafond());
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
        log.debug("Sous-lignes management summary - Creating: {}, Updating: {}, Deleting: {}",
                createCount, updateCount, deleteCount);

        ligneMere.getSousLignes().clear();
        ligneMere.getSousLignes().addAll(updatedSousLignes);

        LigneFonctionnelle updatedLigneMere = ligneFonctionnelleRepository.save(ligneMere);
        log.info("Successfully managed sous-lignes for ligne mère ID: {} - Total persisted: {}",
                ligneMereId, updatedSousLignes.size());

        return ligneFonctionnelleMapper.toDto(updatedLigneMere);
    }


    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = LigneFonctionnelle.class)
    public void deactivateLigneFonctionnelle(Long ligneId) {
        log.info("Attempting to deactivate ligne fonctionnelle ID: {}", ligneId);

        LigneFonctionnelle ligneToDeactivate = ligneFonctionnelleRepository.findById(ligneId)
                .orElseThrow(() -> {
                    log.error("Ligne Fonctionnelle not found with ID: {} for deactivation", ligneId);
                    return new ResourceNotFoundException("Aucune Ligne Fonctionnelle avec l'ID: " + ligneId);
                });

        if (ligneToDeactivate.getLigneMere() != null) {
            log.error("Cannot deactivate sous-ligne directly - ID: {}, Ligne mère ID: {}",
                    ligneId, ligneToDeactivate.getLigneMere().getId());
            throw new IllegalArgumentException(
                    "La désactivation directe des Sous-lignes n'est pas autorisée. Veuillez désactiver la Ligne Mère (ID: " + ligneToDeactivate.getLigneMere().getId() + ") pour affecter tous les enfants."
            );
        }

        if (!ligneToDeactivate.isActif()) {
            log.info("Ligne fonctionnelle ID: {} is already inactive, no action needed", ligneId);
            return;
        }

        ligneToDeactivate.setActif(false);
        log.debug("Deactivated ligne Mère/Indépendante ID: {}", ligneId);

        if (ligneToDeactivate.getSousLignes() != null && !ligneToDeactivate.getSousLignes().isEmpty()) {
            int deactivatedCount = 0;
            for (LigneFonctionnelle sousLigne : ligneToDeactivate.getSousLignes()) {
                if (sousLigne.isActif()) {
                    sousLigne.setActif(false);
                    deactivatedCount++;
                }
            }
            log.debug("Deactivated {} sous-ligne(s) in cascade", deactivatedCount);
        }

        ligneFonctionnelleRepository.save(ligneToDeactivate);
        log.info("Successfully deactivated ligne fonctionnelle ID: {} and its sous-lignes", ligneId);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = LigneFonctionnelle.class)
    public void deleteLigneFonctionnelle(Long id) {
        log.info("Attempting to delete ligne fonctionnelle ID: {}", id);

        LigneFonctionnelle ligneToDelete = ligneFonctionnelleRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Ligne Fonctionnelle not found with ID: {} for deletion", id);
                    return new ResourceNotFoundException("Ligne Fonctionnelle non trouvée avec l'ID: " + id);
                });

        if (ligneToDelete.getLigneMere() != null) {
            log.error("Cannot delete sous-ligne directly - ID: {}, Ligne mère ID: {}",
                    id, ligneToDelete.getLigneMere().getId());
            throw new IllegalArgumentException(
                    "La suppression directe des Sous-lignes n'est pas autorisée. Veuillez supprimer la Ligne Mère (ID: " + ligneToDelete.getLigneMere().getId() + ") pour supprimer tous les enfants en cascade."
            );
        }

        int sousLignesCount = ligneToDelete.getSousLignes() != null ? ligneToDelete.getSousLignes().size() : 0;
        log.debug("Deleting ligne mère with {} sous-ligne(s)", sousLignesCount);

        try {
            ligneFonctionnelleRepository.delete(ligneToDelete);
            log.info("Successfully deleted ligne fonctionnelle ID: {} and {} sous-ligne(s)", id, sousLignesCount);
        } catch (Exception e) {
            log.error("Failed to delete ligne fonctionnelle ID: {}", id, e);
            throw new RuntimeException("Échec de la suppression de la ligne fonctionnelle et de ses dépendances.", e);
        }
    }


    // =========================================================================================
    // 9. CHART HISTORY
    // =========================================================================================

    @Override
    @Transactional(readOnly = true)
    public LigneFonctionnelleChartData getHistoricalChartData(Long banqueId, Long natureLigneFonctionnelleId) {
        log.info("Fetching historical chart data - Banque ID: {}, Nature ID: {}", banqueId, natureLigneFonctionnelleId);

        // Existence de la banque et de la nature validée par le Controller (findOrThrow) avant l'appel.
        List<LigneFonctionnelle> historicalLines = ligneFonctionnelleRepository.findByBanqueIdAndNatureLigneFonctionnelleIdOrderByDateDepartAsc(banqueId, natureLigneFonctionnelleId);

        if (historicalLines.isEmpty()) {
            log.info("No historical lines found for banque ID: {} and nature ID: {}", banqueId, natureLigneFonctionnelleId);
            return new LigneFonctionnelleChartData(new ArrayList<>(), new ArrayList<>());
        }

        log.debug("Found {} historical ligne(s) for chart", historicalLines.size());

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

        log.info("Successfully prepared historical chart data - {} labels, {} taux data points, {} plafond data points",
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
    /**
     * Règle Métier — Portée : vérifie qu'une nature est autorisée pour les lignes générales.
     * Portées valides : GENERALE ou GENERALE_ET_SPECIFIQUE.
     */
    private void validatePorteeForLigneGenerale(NatureLigneFonctionnelle nature) {
        if (nature.getPortee() == PorteeNature.SPECIFIQUE) {
            log.warn("Portée invalide pour ligne générale — nature '{}' a portée SPECIFIQUE", nature.getLabel());
            throw new IllegalArgumentException(
                    "La nature '" + nature.getLabel() + "' est réservée aux lignes spécifiques et ne peut pas être utilisée dans une ligne générale."
            );
        }
    }

    private void validateNatureForMere(NatureLigneFonctionnelle nature) {
        if (!supportsSousLignes(nature)) {
            log.warn("Validation de nature échouée: La nature '{}' ne peut pas être une ligne mère.", nature.getLabel());
            throw new IllegalArgumentException(
                    "Seule la nature ayant le label '" + CAUTION_ADMINISTRATIVE_LABEL + "' est autorisée à être une Ligne Mère (contenir des sous-lignes)."
            );
        }
    }

    private boolean supportsSousLignes(NatureLigneFonctionnelle nature) {
        return nature != null && CAUTION_ADMINISTRATIVE_CODE.equals(nature.getCode());
    }



    /**
     * Helper pour mettre à jour les propriétés spécifiques (Plafond, Nature) et héritées (Taux, Dates, Banque) d'une sous-ligne existante.
     */
    private void updateSousLigneProperties(LigneFonctionnelle sousLigneEntity,
                                           SousLigneRequestDto request,
                                           NatureLigneFonctionnelle newNatureSousLigne,
                                           Banque mereBanque,
                                           BigDecimal mereTaux,
                                           LocalDate mereDateDepart,
                                           LocalDate mereDateFin,
                                           List<Marche> mereMarches) {

        // Propriétés spécifiques
        sousLigneEntity.setPlafond(request.plafond());
        sousLigneEntity.setNatureLigneFonctionnelle(newNatureSousLigne);

        // Propriétés héritées
        sousLigneEntity.setBanque(mereBanque);
        sousLigneEntity.setMarches(new ArrayList<>(mereMarches));
        sousLigneEntity.setTaux(mereTaux);
        sousLigneEntity.setDateDepart(mereDateDepart);
        sousLigneEntity.setDateFin(mereDateFin);
    }



    @Override
    @Transactional(readOnly = true)
    public AgregatedSuiviePrefiLigneResponseDto getSuiviePrefiLigneResponse(Long lineId, Pageable pageable) {
        log.info("Récupération du suivi de préfinancement pour la ligne ID: {}", lineId);

        LigneFonctionnelle Line = ligneFonctionnelleRepository.findById(lineId)
                .orElseThrow(() -> {
                    log.error("Ligne fonctionnelle générale non trouvée avec l'ID: {}", lineId);
                    return new ResourceNotFoundException("Ligne Fonctionnelle générale non trouvée avec l'ID: " + lineId);
                });

        // Un marché archivé est exclu définitivement du suivi (cf. Marche.actif), même règle que
        // le calcul d'encours (LigneFonctionnelleMapper.computeEncoursAndSolde).
        List<Marche> marches = Line.getMarches().stream()
                .filter(Marche::isActif)
                .collect(Collectors.toList());

        if (marches.isEmpty()) {
            log.error("Aucun marché actif associé à la ligne ID: {}", lineId);
            throw new ResourceNotFoundException("Aucun marché actif associé à la ligne fonctionnelle générale avec l'ID: " + lineId);
        }

        log.debug("{} marché(s) associé(s): [{}]", marches.size(),
                marches.stream().map(Marche::getNumero).collect(Collectors.joining(", ")));

        List<Prefinancement> allPrefi = marches.stream()
                .flatMap(m -> prefinancementReader.findByMarche(m).stream())
                .collect(Collectors.toList());
        log.debug("{} préfinancements trouvés", allPrefi.size());

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (Prefinancement prefi : allPrefi) {
            if (prefi.getType() == staport.finance.gBanque.prefinancement.enums.Type.REMBOURSEMENT) totalDebit = totalDebit.add(prefi.getMontant());
            else if (prefi.getType() == staport.finance.gBanque.prefinancement.enums.Type.DEBLOCAGE) totalCredit = totalCredit.add(prefi.getMontant());
        }
        BigDecimal reste = totalCredit.subtract(totalDebit);

        log.debug("Totaux calculés - Débit: {}, Crédit: {}, Reste: {}", totalDebit, totalCredit, reste);

        List<Long> marcheIds = marches.stream().map(Marche::getId).collect(Collectors.toList());
        Specification<Prefinancement> spec = (root, query, cb) -> root.get("marche").get("id").in(marcheIds);

        Page<Prefinancement> prefiPage = prefinancementReader.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));
        log.debug("{} préfinancements récupérés pour la page courante", prefiPage.getNumberOfElements());

        Page<SuiviePrefiLigneResponseDto> pageOfDtos = prefiPage.map(prefinancementMapper::toSuiviePrefiLigneResponseDto);

        String marcheNoms = marches.stream().map(Marche::getNom).collect(Collectors.joining(", "));
        String marcheNumeros = marches.stream().map(Marche::getNumero).collect(Collectors.joining(", "));

        AgregatedSuiviePrefiLigneResponseDto aggregatedDto = new AgregatedSuiviePrefiLigneResponseDto(
                Line.getBanque().getNom(), marcheNoms, marcheNumeros, totalDebit, totalCredit, reste, pageOfDtos);

        log.info("Suivi de préfinancement récupéré avec succès pour la ligne ID: {}", lineId);
        return aggregatedDto;
    }

    @Override
    @Transactional(readOnly = true)
    public AgregatedSuivieDcLigneResponseDto getSuivieDcLigneResponse(Long lineId, Pageable pageable) {
        log.info("Récupération du suivi de droit constaté pour la ligne ID: {}", lineId);

        LigneFonctionnelle line = ligneFonctionnelleRepository.findById(lineId)
                .orElseThrow(() -> {
                    log.error("Ligne fonctionnelle général non trouvée avec l'ID: {}", lineId);
                    return new ResourceNotFoundException("Ligne Fonctionnelle général non trouvée avec l'ID: " + lineId);
                });

        // Un marché archivé est exclu définitivement du suivi (cf. Marche.actif), même règle que
        // le calcul d'encours (LigneFonctionnelleMapper.computeEncoursAndSolde).
        List<Marche> marches = line.getMarches().stream()
                .filter(Marche::isActif)
                .collect(Collectors.toList());
        if (marches.isEmpty()) {
            log.error("Aucun marché actif associé à la ligne ID: {}", lineId);
            throw new ResourceNotFoundException("Aucun marché actif associé à la ligne fonctionnelle avec l'ID: " + lineId);
        }

        log.debug("{} marché(s) associé(s): [{}]", marches.size(),
                marches.stream().map(Marche::getNumero).collect(Collectors.joining(", ")));

        List<DroitConstate> allDc = marches.stream()
                .flatMap(m -> droitConstateReader.findByMarche(m).stream())
                .collect(Collectors.toList());
        log.debug("{} droits constatés trouvés", allDc.size());

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (DroitConstate dc : allDc) {
            if (dc.getType() == staport.finance.gBanque.prefinancement.enums.Type.REMBOURSEMENT) totalDebit = totalDebit.add(dc.getMontant());
            else if (dc.getType() == staport.finance.gBanque.prefinancement.enums.Type.DEBLOCAGE) totalCredit = totalCredit.add(dc.getMontant());
        }

        BigDecimal reste = totalCredit.subtract(totalDebit);

        log.debug("Totaux calculés - Débit: {}, Crédit: {}, Reste: {}", totalDebit, totalCredit, reste);

        List<Long> marcheIds = marches.stream().map(Marche::getId).collect(Collectors.toList());
        Specification<DroitConstate> spec = (root, query, cb) -> root.get("marche").get("id").in(marcheIds);
        Page<DroitConstate> dcPage = droitConstateReader.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.debug("{} droits constatés récupérés pour la page courante", dcPage.getNumberOfElements());

        Page<SuivieDcLigneResponseDto> pageOfDtos = dcPage.map(droitConstateMapper::toSuivieDcLigneResponseDto);

        String marcheNomsDc = marches.stream().map(Marche::getNom).collect(Collectors.joining(", "));
        String marcheNumerosDc = marches.stream().map(Marche::getNumero).collect(Collectors.joining(", "));

        AgregatedSuivieDcLigneResponseDto aggregatedDto = new AgregatedSuivieDcLigneResponseDto(
                line.getBanque().getNom(), marcheNomsDc, marcheNumerosDc, totalDebit, totalCredit, reste, pageOfDtos);

        log.info("Suivi de droit constaté récupéré avec succès pour la ligne ID: {}", lineId);
        return aggregatedDto;
    }

    @Override
    @Transactional(readOnly = true)
    public AgregatedSuivieFactoringLigneResponseDto getSuivieFactoringLigneResponse(Long lineId, Pageable pageable) {
        log.info("Récupération du suivi de factoring pour la ligne ID: {}", lineId);

        LigneFonctionnelle line = ligneFonctionnelleRepository.findById(lineId)
                .orElseThrow(() -> {
                    log.error("Ligne fonctionnelle général non trouvée avec l'ID: {}", lineId);
                    return new ResourceNotFoundException("Ligne Fonctionnelle général non trouvée avec l'ID: " + lineId);
                });

        // Un marché archivé est exclu définitivement du suivi (cf. Marche.actif), même règle que
        // le calcul d'encours (LigneFonctionnelleMapper.computeEncoursAndSolde).
        List<Marche> marches = line.getMarches().stream()
                .filter(Marche::isActif)
                .collect(Collectors.toList());
        if (marches.isEmpty()) {
            log.error("Aucun marché actif associé à la ligne ID: {}", lineId);
            throw new ResourceNotFoundException("Aucun marché actif associé à la ligne fonctionnelle avec l'ID: " + lineId);
        }

        log.debug("{} marché(s) associé(s): [{}]", marches.size(),
                marches.stream().map(Marche::getNumero).collect(Collectors.joining(", ")));

        List<Factoring> allPaidFactoring = marches.stream()
                .flatMap(m -> factoringReader.findByMarche(m).stream())
                .collect(Collectors.toList());
        log.debug("{} factorings trouvés", allPaidFactoring.size());

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (Factoring f : allPaidFactoring) {
            if (f.getType() == staport.finance.gBanque.prefinancement.enums.Type.REMBOURSEMENT) totalDebit = totalDebit.add(f.getMontant());
            else if (f.getType() == staport.finance.gBanque.prefinancement.enums.Type.DEBLOCAGE) totalCredit = totalCredit.add(f.getMontant());
        }
        BigDecimal reste = totalCredit.subtract(totalDebit);

        log.debug("Totaux calculés - Débit: {}, Crédit: {}, Reste: {}", totalDebit, totalCredit, reste);

        List<Long> marcheIds = marches.stream().map(Marche::getId).collect(Collectors.toList());
        Specification<Factoring> spec = (root, query, cb) -> root.get("marche").get("id").in(marcheIds);
        Page<Factoring> factoringPage = factoringReader.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.debug("{} factorings récupérés pour la page courante", factoringPage.getNumberOfElements());

        Page<SuivieFactoringLineResponseDto> pageOfDtos = factoringPage.map(factoringMapper::toSuivieFactoringLigneResponseDto);

        String marcheNomsF = marches.stream().map(Marche::getNom).collect(Collectors.joining(", "));
        String marcheNumerosF = marches.stream().map(Marche::getNumero).collect(Collectors.joining(", "));

        AgregatedSuivieFactoringLigneResponseDto aggregatedDto = new AgregatedSuivieFactoringLigneResponseDto(
                line.getBanque().getNom(), marcheNomsF, marcheNumerosF, totalDebit, totalCredit, reste, pageOfDtos);

        log.info("Suivi de factoring récupéré avec succès pour la ligne ID: {}", lineId);
        return aggregatedDto;
    }

    @Override
    @Transactional(readOnly = true)
    public AgregatedSuivieLcnLigneResponseDto getSuivieLcnLigneResponse(Long lineId, Pageable pageable) {
        log.info("Récupération du suivi LCN pour la ligne ID: {}", lineId);

        LigneFonctionnelle line = ligneFonctionnelleRepository.findById(lineId)
                .orElseThrow(() -> {
                    log.error("Ligne fonctionnelle générale non trouvée avec l'ID: {}", lineId);
                    return new ResourceNotFoundException("Ligne Fonctionnelle générale non trouvée avec l'ID: " + lineId);
                });


        Specification<Lcn> spec = Specification.where(LcnSpecification.hasLigneFonctionnelle(lineId));

        List<Lcn> allLcn = lcnReader.findAll(spec);
        log.debug("{} LCN trouvés", allLcn.size());


        Page<Lcn> lcnPage = lcnReader.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.debug("{} LCN récupérés pour la page courante", lcnPage.getNumberOfElements());

        // Libellé/observation sont des champs d'affichage calculés (pas portés par le Mapper,
        // cf. architecture.md) — même règle que LcnService.toDto, dupliquée ici car ce service
        // ne doit pas dépendre de LcnService (domaines distincts).
        Page<LcnResponseDTO> pageOfDtos = lcnPage.map(lcn -> {
            LcnResponseDTO dto = lcnMapper.toDto(lcn);
            dto.setObservation(" LCN N° " + lcn.getNumero());
            dto.setLibelle(lcn.getType() == Type.DECAISSEMENT ? lcn.getBeneficiaire() : lcn.getEmetteur());
            return dto;
        });

        AgregatedSuivieLcnLigneResponseDto aggregatedDto = new AgregatedSuivieLcnLigneResponseDto();
        aggregatedDto.setBanqueName(line.getBanque().getNom());
        aggregatedDto.setTotal(allLcn.stream().map(Lcn::getMontant).reduce(BigDecimal.ZERO,BigDecimal::add));
        aggregatedDto.setPage(pageOfDtos);

        log.info("Suivi LCN récupéré avec succès pour la ligne ID: {}", lineId);
        return aggregatedDto;
    }

    private void validateDateRange(LocalDate dateDepart, LocalDate dateFin) {
        if (dateDepart != null && dateFin != null && dateFin.isBefore(dateDepart)) {
            log.warn("Validation de date échouée: dateFin ({}) est antérieure à dateDepart ({}).", dateFin, dateDepart);
            throw new IllegalArgumentException("La date de fin ne peut pas être antérieure à la date de départ.");
        }
    }

    /**
     * Vérifie qu'il n'existe pas déjà une ligne fonctionnelle active avec la même combinaison.
     * RÈGLE MÉTIER :
     * - Ligne sans marché : une seule ligne générale active par banque + nature.
     * - Ligne avec marchés : pour chaque marché, un seul actif par nature (général ou spécifique).
     */
    private void avoidDuplicateActifLines(Banque banque, NatureLigneFonctionnelle nature, List<Marche> marches) {

        if (marches == null || marches.isEmpty()) {
            // Ligne générale sans marché
            ligneFonctionnelleRepository.findActiveGeneralLine(banque.getId(), nature.getId())
                    .ifPresent(existing -> {
                        log.warn("Duplicate active ligne générale - Banque: '{}', Nature: '{}'",
                                banque.getNom(), nature.getLabel());
                        throw new DuplicateEntryException(
                                "Une ligne fonctionnelle générale ACTIVE de type '" + nature.getLabel() +
                                        "' existe déjà pour la banque '" + banque.getNom() + "' (ID: " + existing.getId() + ")."
                        );
                    });
        } else {
            // Ligne liée à des marchés — vérifier chaque marché
            for (Marche marche : marches) {
                checkNoConflictWithSpecificLine(marche, nature);
                ligneFonctionnelleRepository.findActiveLineForMarche(marche.getId(), nature.getId())
                        .ifPresent(existing -> {
                            log.warn("Duplicate active ligne pour le marché '{}', Nature: '{}'",
                                    marche.getNumero(), nature.getLabel());
                            throw new DuplicateEntryException(
                                    "Une ligne fonctionnelle ACTIVE de type '" + nature.getLabel() +
                                            "' existe déjà pour le marché '" + marche.getNumero() + "' (ID: " + existing.getId() + ")."
                            );
                        });
            }
        }

        log.debug("No duplicate active ligne found - validation passed");
    }

    /**
     * Charge et valide une liste de marchés depuis leurs IDs.
     * Chaque marché doit appartenir à la banque spécifiée.
     *
     * @param marcheIds Liste d'IDs de marchés (peut être null ou vide = ligne générale sans marché)
     * @param banqueId  ID de la banque propriétaire
     * @return Liste de Marche validés (vide si aucun marché spécifié)
     */
    private List<Marche> validateAndGetMarches(List<Long> marcheIds, Long banqueId) {
        if (marcheIds == null || marcheIds.isEmpty()) {
            log.debug("Aucun marcheId fourni - ligne générale sans marché");
            return new ArrayList<>();
        }

        List<Marche> result = new ArrayList<>();
        for (Long marcheId : marcheIds) {
            log.debug("Validating marche ID: {} for banque ID: {}", marcheId, banqueId);
            Marche marche = marcheService.findOrThrow(marcheId);
            if (!marche.getBanque().getId().equals(banqueId)) {
                log.error("Marche ID: {} appartient à la banque {}, pas à la banque {}",
                        marcheId, marche.getBanque().getId(), banqueId);
                throw new IllegalArgumentException(
                        "Le marché '" + marche.getNumero() + "' n'appartient pas à la banque spécifiée. " +
                                "Il appartient à la banque '" + marche.getBanque().getNom() + "'."
                );
            }
            result.add(marche);
        }
        log.debug("{} marché(s) validé(s)", result.size());
        return result;
    }

    /**
     * Vérifie si la clé métier (Banque + Nature + ensemble de marchés) a changé.
     */
    private boolean hasKeyChanged(LigneFonctionnelle existing, Banque newBanque,
                                  NatureLigneFonctionnelle newNature, List<Marche> newMarches) {

        boolean banqueChanged = !existing.getBanque().getId().equals(newBanque.getId());
        boolean natureChanged = !existing.getNatureLigneFonctionnelle().getId().equals(newNature.getId());

        Set<Long> existingMarcheIds = existing.getMarches().stream()
                .map(Marche::getId)
                .collect(Collectors.toSet());
        List<Marche> safeNewMarches = newMarches != null ? newMarches : Collections.emptyList();
        Set<Long> newMarcheIds = safeNewMarches.stream()
                .map(Marche::getId)
                .collect(Collectors.toSet());
        boolean marchesChanged = !existingMarcheIds.equals(newMarcheIds);

        if (banqueChanged) log.debug("Banque changed from ID {} to ID {}", existing.getBanque().getId(), newBanque.getId());
        if (natureChanged) log.debug("Nature changed from ID {} to ID {}", existing.getNatureLigneFonctionnelle().getId(), newNature.getId());
        if (marchesChanged) log.debug("Marchés changed from {} to {}", existingMarcheIds, newMarcheIds);

        return banqueChanged || natureChanged || marchesChanged;
    }

    /**
     * Vérifie qu'il n'existe pas déjà une AUTRE ligne active avec la même clé métier (hors ligne courante).
     */
    private void avoidDuplicateActifLinesForUpdate(Long currentId, Banque banque,
                                                   NatureLigneFonctionnelle nature, List<Marche> marches) {

        if (marches == null || marches.isEmpty()) {
            ligneFonctionnelleRepository.findActiveGeneralLine(banque.getId(), nature.getId())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(currentId)) {
                            log.warn("Duplicate general active ligne during update - Banque: '{}', Nature: '{}', Existing ID: {}",
                                    banque.getNom(), nature.getLabel(), existing.getId());
                            throw new DuplicateEntryException(
                                    "Une autre ligne fonctionnelle générale ACTIVE de type '" + nature.getLabel() +
                                            "' existe déjà pour la banque '" + banque.getNom() + "' (ID: " + existing.getId() + ")."
                            );
                        }
                    });
        } else {
            for (Marche marche : marches) {
                checkNoConflictWithSpecificLine(marche, nature);
                ligneFonctionnelleRepository.findActiveLineForMarche(marche.getId(), nature.getId())
                        .ifPresent(existing -> {
                            if (!existing.getId().equals(currentId)) {
                                log.warn("Duplicate marche-specific active ligne during update - Marche: '{}', Nature: '{}', Existing ID: {}",
                                        marche.getNumero(), nature.getLabel(), existing.getId());
                                throw new DuplicateEntryException(
                                        "Une autre ligne fonctionnelle ACTIVE de type '" + nature.getLabel() +
                                                "' existe déjà pour le marché '" + marche.getNumero() + "' (ID: " + existing.getId() + ")."
                                );
                            }
                        });
            }
        }

        log.debug("No duplicate active ligne found for update - validation passed");
    }

    /**
     * Vérifie qu'il n'existe pas de LigneFonctionnelleSpecifique active avec la même nature
     * pour le marché spécifié.
     * Cette règle garantit qu'un marché ne peut avoir qu'UNE ligne active par nature,
     * qu'elle soit générale ou spécifique.
     */
    private void checkNoConflictWithSpecificLine(Marche marche, NatureLigneFonctionnelle nature) {
        if (marche == null) {
            // Pas de marché = ligne générale sans marché spécifique
            // Pas besoin de vérifier les lignes spécifiques
            return;
        }

        Optional<LigneFonctionnelleSpecifique> existingSpecificLine =
                ligneFonctionnelleSpecifiqueReader.findByMarcheAndNatureLigneFonctionnelleAndActifTrue(
                        marche, nature
                );

        existingSpecificLine.ifPresent(existing -> {
            log.error("Conflit détecté - Une LigneFonctionnelleSpecifique active existe déjà - Marché: '{}', Nature: '{}', ID: {}",
                    marche.getNumero(), nature.getLabel(), existing.getId());

            throw new DuplicateEntryException(
                    "Impossible de créer une ligne générale. " +
                            "Le marché '" + marche.getNumero() + "' possède déjà une ligne fonctionnelle SPÉCIFIQUE active " +
                            "de type '" + nature.getLabel() + "' (ID: " + existing.getId() + "). " +
                            "Un marché ne peut avoir qu'une seule ligne active par nature."
            );
        });

        log.debug("Aucun conflit avec les lignes spécifiques - validation réussie");
    }

    @Override
    public BigDecimal getPlafondFCInternal(Long banqueId, Long fcNatureId) {
        return ligneFonctionnelleRepository
                .findByBanqueIdAndNatureLigneFonctionnelleIdAndActifTrue(banqueId, fcNatureId)
                .map(LigneFonctionnelle::getPlafond)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public Map<Long, BigDecimal> getActivePlafondsForBanks(List<Long> bankIds, String fcCode) {
        if (bankIds == null || bankIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // Fetching List<Object[]> where [0] is BankId and [1] is Plafond
        List<Object[]> results = ligneFonctionnelleRepository.findActivePlafondsByBankIds(bankIds, fcCode);

        return results.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (BigDecimal) row[1],
                        (existing, replacement) -> existing // Guard against unexpected duplicates
                ));
    }

    @Override
    public boolean hasActiveLignesForBanque(Long banqueId) {
        return ligneFonctionnelleRepository.existsByBanqueIdAndActifTrue(banqueId);
    }

    @Override
    public boolean existsByNatureId(Long natureId) {
        return ligneFonctionnelleRepository.existsByNatureLigneFonctionnelleId(natureId);
    }

    // =========================================================================================
    // 11. EXPORT PDF
    // =========================================================================================

    /**
     * Exporte en PDF l'intégralité des lignes fonctionnelles générales (mères/indépendantes,
     * comme {@link #getPaginatedLigneFonctionnelles}) correspondant aux filtres fournis — sans
     * pagination. Réutilisé par les écrans encours ({@code criteria.actif() == true}) et archivées
     * ({@code criteria.actif() == false}) : seul le titre du rapport varie selon ce critère.
     * <p>
     * Passe par {@link LigneFonctionnelleMapper#toDto} (comme les écrans) pour le calcul
     * encours/solde/expired — jamais recalculé ici (pas de duplication de logique métier,
     * cf. CLAUDE.md).
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] exportPdf(LigneFonctionnelleSearchCriteria criteria) {
        log.info("Export PDF des lignes fonctionnelles générales — critères: {}", criteria);

        Specification<LigneFonctionnelle> spec = LigneFonctionnelleSpecification.searchByCriteria(criteria)
                .and(LigneFonctionnelleSpecification.isLigneMereOrIndependante());
        List<LigneFonctionnelle> entities = ligneFonctionnelleRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "id"));
        List<LigneFonctionnelleResponse> lignes = entities.stream().map(ligneFonctionnelleMapper::toDto).collect(Collectors.toList());

        String title = "Lignes Fonctionnelles Générales" + statusSuffix(criteria);
        BigDecimal totalPlafond = lignes.stream()
                .map(LigneFonctionnelleResponse::plafond)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        Color medBlue = Color.decode("#2563eb");
        Color lightBlue = Color.decode("#dbeafe");
        Color rowAlt = Color.decode("#f8fafc");
        Color green = Color.decode("#16a34a");
        Color red = Color.decode("#dc2626");
        Color borderGray = Color.decode("#e5e7eb");
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        try {
            return staportPdfReportService.generateReport(title, PageSize.A4.rotate(), (doc, writer) -> {
                try {
                    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
                    Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.decode("#374151"));
                    Font hdrFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
                    Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
                    Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.decode("#1e3a8a"));

                    doc.add(Chunk.NEWLINE);
                    doc.add(Chunk.NEWLINE);

                    Paragraph titleParagraph = new Paragraph(title, titleFont);
                    titleParagraph.setAlignment(Element.ALIGN_CENTER);
                    doc.add(titleParagraph);

                    Paragraph sub = new Paragraph(
                            "Exporté le " + LocalDate.now().format(dateFmt) + "  •  " + lignes.size() + " ligne(s)",
                            subFont);
                    sub.setAlignment(Element.ALIGN_CENTER);
                    sub.setSpacingAfter(8f);
                    doc.add(sub);

                    float[] colWidths = {14f, 12f, 16f, 10f, 10f, 10f, 8f, 10f, 10f, 8f};
                    PdfPTable table = new PdfPTable(colWidths);
                    table.setWidthPercentage(100f);
                    table.setSpacingBefore(8f);
                    table.setHeaderRows(1);

                    for (String h : new String[]{
                            "Banque", "Nature", "Marché(s)", "Plafond (MAD)", "Encours (MAD)",
                            "Solde (MAD)", "Taux (%)", "Date Départ", "Date Fin", "Statut"}) {
                        PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                        c.setBackgroundColor(medBlue);
                        c.setPadding(5f);
                        c.setHorizontalAlignment(Element.ALIGN_CENTER);
                        c.setBorderColor(Color.WHITE);
                        c.setBorderWidth(0.5f);
                        table.addCell(c);
                    }

                    for (int i = 0; i < lignes.size(); i++) {
                        LigneFonctionnelleResponse l = lignes.get(i);
                        Color rowBg = i % 2 == 0 ? Color.WHITE : rowAlt;

                        String marchesStr = (l.marches() == null || l.marches().isEmpty())
                                ? "Général"
                                : l.marches().stream().map(MarcheRefDto::numero).collect(Collectors.joining(", "));

                        addPdfCell(table, l.banqueNom(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                        addPdfCell(table, l.natureLigneFonctionnelleLabel(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                        addPdfCell(table, marchesStr, cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                        addPdfCell(table, MontantFormatter.format(l.plafond()), cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);
                        addPdfCell(table, MontantFormatter.format(l.encours()), cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);
                        addPdfCell(table, MontantFormatter.format(l.solde()), cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);
                        addPdfCell(table, MontantFormatter.format(l.taux()), cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);
                        addPdfCell(table, l.dateDepart() != null ? l.dateDepart().format(dateFmt) : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);
                        addPdfCell(table, l.dateFin() != null ? l.dateFin().format(dateFmt) : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);

                        boolean expired = l.expired();
                        PdfPCell statusCell = new PdfPCell(new Phrase(
                                expired ? "EXPIRÉE" : "EN COURS",
                                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
                        statusCell.setBackgroundColor(expired ? red : green);
                        statusCell.setPadding(4f);
                        statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        statusCell.setBorderColor(borderGray);
                        statusCell.setBorderWidth(0.5f);
                        table.addCell(statusCell);
                    }

                    PdfPCell totalLabel = new PdfPCell(
                            new Phrase("TOTAL — " + lignes.size() + " ligne(s)", totalFont));
                    totalLabel.setColspan(3);
                    totalLabel.setBackgroundColor(lightBlue);
                    totalLabel.setPadding(5f);
                    totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalLabel.setBorderColor(medBlue);
                    table.addCell(totalLabel);

                    PdfPCell totalVal = new PdfPCell(
                            new Phrase(MontantFormatter.format(totalPlafond), totalFont));
                    totalVal.setBackgroundColor(lightBlue);
                    totalVal.setPadding(5f);
                    totalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalVal.setBorderColor(medBlue);
                    table.addCell(totalVal);

                    for (int j = 0; j < 6; j++) {
                        PdfPCell empty = new PdfPCell(new Phrase(""));
                        empty.setBackgroundColor(lightBlue);
                        empty.setBorderColor(medBlue);
                        table.addCell(empty);
                    }

                    doc.add(table);
                } catch (DocumentException e) {
                    throw new RuntimeException("Erreur lors de la génération du contenu du PDF des lignes fonctionnelles.", e);
                }
            });
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des lignes fonctionnelles.", e);
        }
    }

    private String statusSuffix(LigneFonctionnelleSearchCriteria criteria) {
        if (criteria == null || criteria.actif() == null) return "";
        return criteria.actif() ? " — En Cours" : " — Archivées";
    }

    private void addPdfCell(PdfPTable table, String text, Font font, Color bg, Color border, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", font));
        cell.setBackgroundColor(bg);
        cell.setPadding(4f);
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(border);
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
    }

}
