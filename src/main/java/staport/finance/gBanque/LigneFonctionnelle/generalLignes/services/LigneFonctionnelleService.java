package staport.finance.gBanque.LigneFonctionnelle.generalLignes.services;

import staport.finance.gBanque.LigneFonctionnelle.generalLignes.dtos.*;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.mappers.LigneFonctionnelleMapper;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.repositories.LigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.specifications.LigneFonctionnelleSpecification;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories.NatureLigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.repositories.LigneFonctionnelleSpecifiqueRepository;
import staport.finance.gBanque.droitConstate.dtos.AgregatedSuivieDcLigneResponseDto;
import staport.finance.gBanque.droitConstate.dtos.AgregatedSuivieLcnLigneResponseDto;
import staport.finance.gBanque.droitConstate.dtos.SuivieDcLigneResponseDto;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.droitConstate.mapper.DroitConstateMapper;
import staport.finance.gBanque.droitConstate.repositories.DroitConstateRepository;
import staport.finance.gBanque.droitConstate.spec.DroitConstateSpec;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import staport.finance.gBanque.factoring.dtos.AgregatedSuivieFactoringLigneResponseDto;
import staport.finance.gBanque.factoring.dtos.SuivieFactoringLineResponseDto;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.factoring.mapper.FactoringMapper;
import staport.finance.gBanque.factoring.repository.FactoringRepository;
import staport.finance.gBanque.factoring.spec.FactoringSpec;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.repositories.MarcheRepository;
import staport.finance.gBanque.moyensDePaiements.lcn.dtos.LcnResponseDTO;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.lcn.mapper.LcnMapper;
import staport.finance.gBanque.moyensDePaiements.lcn.repositories.LcnRepository;
import staport.finance.gBanque.moyensDePaiements.lcn.specs.LcnSpecification;
import staport.finance.gBanque.prefinancement.dtos.AgregatedSuiviePrefiLigneResponseDto;
import staport.finance.gBanque.prefinancement.dtos.SuiviePrefiLigneResponseDto;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Type;
import staport.finance.gBanque.prefinancement.mapper.PrefinancementMapper;
import staport.finance.gBanque.prefinancement.repository.PrefinancementRepository;
import staport.finance.gBanque.prefinancement.spec.PrefinancementSpecification;


@Service
public class LigneFonctionnelleService implements ILigneFonctionnelleService{

    private static final Logger logger = LoggerFactory.getLogger(LigneFonctionnelleService.class);
    // Constante de Règle Métier : Seule cette nature peut être une Ligne Mère
    private static final String CAUTION_ADMINISTRATIVE_LABEL = "Caution Administrative";

    private final LigneFonctionnelleRepository ligneFonctionnelleRepository;
    private final LigneFonctionnelleMapper ligneFonctionnelleMapper;
    private final BanqueRepository banqueRepository;
    private final NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository;
    private final MarcheRepository marcheRepository;
    private final LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository;
    private final PrefinancementRepository prefinancementRepository;
    private final FactoringRepository factoringRepository;
    private final DroitConstateRepository droitConstateRepository;
    private final PrefinancementMapper prefinancementMapper;
    private final LcnRepository lcnRepository;
    private final FactoringMapper factoringMapper;
    private final DroitConstateMapper droitConstateMapper;

    @Autowired
    public LigneFonctionnelleService(LigneFonctionnelleRepository ligneFonctionnelleRepository,
                                     LigneFonctionnelleMapper ligneFonctionnelleMapper,
                                     BanqueRepository banqueRepository,
                                     NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository, MarcheRepository marcheRepository, LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository, PrefinancementRepository prefinancementRepository, FactoringRepository factoringRepository, DroitConstateRepository droitConstateRepository, PrefinancementMapper prefinancementMapper, LcnRepository lcnRepository, FactoringMapper factoringMapper, DroitConstateMapper droitConstateMapper) {
        this.ligneFonctionnelleRepository = ligneFonctionnelleRepository;
        this.ligneFonctionnelleMapper = ligneFonctionnelleMapper;
        this.banqueRepository = banqueRepository;
        this.natureLigneFonctionnelleRepository = natureLigneFonctionnelleRepository;
        this.marcheRepository = marcheRepository;
        this.ligneFonctionnelleSpecifiqueRepository = ligneFonctionnelleSpecifiqueRepository;
        this.prefinancementRepository = prefinancementRepository;
        this.factoringRepository = factoringRepository;
        this.droitConstateRepository = droitConstateRepository;
        this.prefinancementMapper = prefinancementMapper;
        this.lcnRepository = lcnRepository;
        this.factoringMapper = factoringMapper;
        this.droitConstateMapper = droitConstateMapper;
    }


    public LigneFonctionnelleResponse createLigneFonctionnelle(LigneFonctionnelleRequest request) {

        logger.debug("Creating ligne fonctionnelle, Full creation request: {}", request);

        // 1. Validation de la plage de dates
        validateDateRange(request.getDateDepart(), request.getDateFin());

        // 2. Récupération et validation de la Banque
        Banque banque = banqueRepository.findById(request.getBanqueId())
                .orElseThrow(() -> {
                    logger.error("Banque not found with ID: {}", request.getBanqueId());
                    return new ResourceNotFoundException("Banque non trouvée avec l'ID: " + request.getBanqueId());
                });

        logger.debug("Found banque: {} (ID: {})", banque.getNom(), banque.getId());

        // 3. Récupération et validation de la Nature
        NatureLigneFonctionnelle natureLigneFonctionnelle = natureLigneFonctionnelleRepository.findById(request.getNatureLigneFonctionnelleId())
                .orElseThrow(() -> {
                    logger.error("NatureLigneFonctionnelle not found with ID: {}", request.getNatureLigneFonctionnelleId());
                    return new ResourceNotFoundException("NatureLigneFonctionnelle non trouvée avec l'ID: " + request.getNatureLigneFonctionnelleId());
                });

        logger.debug("Found nature: {} (ID: {})", natureLigneFonctionnelle.getLabel(), natureLigneFonctionnelle.getId());

        //  4 : Récupération et validation du Marché (optionnel)
        Marche marche = validateAndGetMarche(request.getMarcheId(), request.getBanqueId());

        if (marche != null) {
            logger.debug("Found marche: {} (ID: {}, Numero: {})",
                    marche.getNom(), marche.getId(), marche.getNumero());
        } else {
            logger.debug("No marche specified - creating general ligne fonctionnelle");
        }

        // 5 : Vérification des doublons avec le marché
        avoidDuplicateActifLines(banque, natureLigneFonctionnelle, marche);

        //  6 : Création de l'entité avec le marché
        LigneFonctionnelle ligneFonctionnelle = ligneFonctionnelleMapper.toEntity(
                request, banque, natureLigneFonctionnelle, marche
        );

        // 7. Gestion des sous-lignes (inchangé)
        if (request.getSousLignes() != null && !request.getSousLignes().isEmpty()) {
            logger.info("Processing {} sous-ligne(s) for creation", request.getSousLignes().size());

            validateNatureForMere(natureLigneFonctionnelle);

            List<LigneFonctionnelle> sousLignesEntities = new ArrayList<>();

            for (SousLigneRequestDto sousLigneRequest : request.getSousLignes()) {
                logger.debug("Processing sous-ligne - Nature ID: {}, Plafond: {}",
                        sousLigneRequest.getNatureLigneFonctionnelleId(), sousLigneRequest.getPlafond());

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

        // 8. Sauvegarde
        LigneFonctionnelle savedLigneFonctionnelle = ligneFonctionnelleRepository.save(ligneFonctionnelle);
        logger.info("Successfully created ligne fonctionnelle with ID: {}, Marche: {}, Has {} sous-ligne(s)",
                savedLigneFonctionnelle.getId(),
                marche != null ? marche.getNumero() : "N/A",
                savedLigneFonctionnelle.getSousLignes() != null ? savedLigneFonctionnelle.getSousLignes().size() : 0);

        return ligneFonctionnelleMapper.toDto(savedLigneFonctionnelle);
    }


    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public Page<LigneFonctionnelleResponse> getPaginatedLignesFonctionnellesByMarcheId(
            Long marcheId,
            LigneFonctionnelleSearchCriteria criteria,
            Pageable pageable) {

        logger.info("Fetching paginated lignes fonctionnelles for marche ID: {} - Page: {}, Size: {}",
                marcheId, pageable.getPageNumber(), pageable.getPageSize());
        logger.debug("Search criteria for marche {}: {}", marcheId, criteria);

        // Vérifier que le marché existe
        if (!marcheRepository.existsById(marcheId)) {
            logger.error("Marche not found with ID: {} while fetching lignes fonctionnelles", marcheId);
            throw new ResourceNotFoundException("Marché non trouvé avec l'ID: " + marcheId + ". Impossible de récupérer les lignes fonctionnelles.");
        }

        // Ajouter le marcheId aux critères de recherche
        criteria.setMarcheId(marcheId);

        // Construire la spécification avec les critères
        Specification<LigneFonctionnelle> spec = LigneFonctionnelleSpecification.searchByCriteria(criteria);

        // Filtrer uniquement les lignes Mère/Indépendante (pas les sous-lignes)
        Specification<LigneFonctionnelle> onlyParentsSpec = LigneFonctionnelleSpecification.isLigneMereOrIndependante();
        spec = spec.and(onlyParentsSpec);

        Page<LigneFonctionnelle> ligneFonctionnellesPage = ligneFonctionnelleRepository.findAll(spec, pageable);

        logger.info("Retrieved {} ligne(s) Mère/Indépendante for marche ID: {} out of {} total (Page {}/{})",
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
    @Transactional
    public LigneFonctionnelleResponse updateLigneFonctionnelle(Long id, LigneFonctionnelleRequest request) {

        logger.info("Updating ligne fonctionnelle ID: {} - New plafond: {}, New taux: {}",
                id, request.getPlafond(), request.getTaux());
        logger.debug("Full update request for ID {}: {}", id, request);

        // 1. Récupération de la ligne existante
        LigneFonctionnelle existingLigneFonctionnelle = ligneFonctionnelleRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Ligne Fonctionnelle not found with ID: {} for update", id);
                    return new ResourceNotFoundException("Ligne Fonctionnelle non trouvée avec l'ID: " + id);
                });

        logger.debug("Found existing ligne fonctionnelle: ID {}, Current plafond: {}, Current marche: {}, Has {} sous-ligne(s)",
                existingLigneFonctionnelle.getId(),
                existingLigneFonctionnelle.getPlafond(),
                existingLigneFonctionnelle.getMarche() != null ? existingLigneFonctionnelle.getMarche().getNumero() : "N/A",
                existingLigneFonctionnelle.getSousLignes() != null ? existingLigneFonctionnelle.getSousLignes().size() : 0);

        // 2. Validation de la plage de dates
        validateDateRange(request.getDateDepart(), request.getDateFin());

        // 3. Récupération et validation de la banque cible
        Banque targetBanque = banqueRepository.findById(request.getBanqueId())
                .orElseThrow(() -> {
                    logger.error("Target banque not found with ID: {}", request.getBanqueId());
                    return new ResourceNotFoundException("Banque non trouvée avec l'ID: " + request.getBanqueId());
                });

        // 4. Récupération et validation de la nature cible
        NatureLigneFonctionnelle targetNature = natureLigneFonctionnelleRepository.findById(request.getNatureLigneFonctionnelleId())
                .orElseThrow(() -> {
                    logger.error("Target nature not found with ID: {}", request.getNatureLigneFonctionnelleId());
                    return new ResourceNotFoundException("NatureLigneFonctionnelle non trouvée avec l'ID: " + request.getNatureLigneFonctionnelleId());
                });

        // ✅ 5. NOUVEAU : Récupération et validation du marché cible (optionnel)
        Marche targetMarche = validateAndGetMarche(request.getMarcheId(), request.getBanqueId());

        if (targetMarche != null) {
            logger.debug("Target marche: {} (ID: {}, Numero: {})",
                    targetMarche.getNom(), targetMarche.getId(), targetMarche.getNumero());
        } else {
            logger.debug("No target marche - ligne will be/remain general");
        }

        // 6. Validation pour les lignes mères
        if (existingLigneFonctionnelle.getSousLignes() != null && !existingLigneFonctionnelle.getSousLignes().isEmpty()) {
            logger.debug("Ligne mère has {} sous-ligne(s), validating nature",
                    existingLigneFonctionnelle.getSousLignes().size());
            validateNatureForMere(targetNature);
        }

        // ✅ 7. MODIFIÉ : Vérification des doublons si clé modifiée (banque, nature OU marché)
        if (existingLigneFonctionnelle.isActif() && hasKeyChanged(existingLigneFonctionnelle, targetBanque, targetNature, targetMarche)) {
            logger.debug("Key changed (banque, nature, or marche), checking for duplicate active lines");
            avoidDuplicateActifLinesForUpdate(id, targetBanque, targetNature, targetMarche);
        }

        // ✅ 8. MODIFIÉ : Mise à jour de la ligne avec le marché
        ligneFonctionnelleMapper.updateLigneFonctionnelleFromDto(
                request,
                existingLigneFonctionnelle,
                targetBanque,
                targetNature,
                targetMarche  // ✅ Ajout du marché
        );

        // 9. Propagation des propriétés héritées aux sous-lignes
        if (existingLigneFonctionnelle.getSousLignes() != null && !existingLigneFonctionnelle.getSousLignes().isEmpty()) {
            logger.debug("Propagating inherited properties to {} sous-ligne(s)",
                    existingLigneFonctionnelle.getSousLignes().size());
            for (LigneFonctionnelle sousLigne : existingLigneFonctionnelle.getSousLignes()) {
                sousLigne.setTaux(existingLigneFonctionnelle.getTaux());
                sousLigne.setBanque(existingLigneFonctionnelle.getBanque());
                sousLigne.setMarche(existingLigneFonctionnelle.getMarche()); // ✅ Propagation du marché
                sousLigne.setDateDepart(existingLigneFonctionnelle.getDateDepart());
                sousLigne.setDateFin(existingLigneFonctionnelle.getDateFin());
            }
        }

        // 10. Sauvegarde
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
        LocalDate mereDateDepart = ligneMere.getDateDepart();
        LocalDate mereDateFin = ligneMere.getDateFin();
        Marche mereMarche = ligneMere.getMarche();


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
                        mereBanque, mereTaux, mereDateDepart, mereDateFin,mereMarche
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
                                           LocalDate mereDateDepart,
                                           LocalDate mereDateFin,Marche mereMarche) {

        // Propriétés spécifiques
        sousLigneEntity.setPlafond(request.getPlafond());
        sousLigneEntity.setNatureLigneFonctionnelle(newNatureSousLigne);

        // Propriétés héritées
        sousLigneEntity.setBanque(mereBanque);
        sousLigneEntity.setMarche(mereMarche);
        sousLigneEntity.setTaux(mereTaux);
        sousLigneEntity.setDateDepart(mereDateDepart);
        sousLigneEntity.setDateFin(mereDateFin);
    }



    @Transactional(readOnly = true)
    public AgregatedSuiviePrefiLigneResponseDto getSuiviePrefiLigneResponse(Long lineId, Pageable pageable) {
        logger.info("Récupération du suivi de préfinancement pour la ligne ID: {}", lineId);

        LigneFonctionnelle Line = ligneFonctionnelleRepository.findById(lineId)
                .orElseThrow(() -> {
                    logger.error("Ligne fonctionnelle générale non trouvée avec l'ID: {}", lineId);
                    return new ResourceNotFoundException("Ligne Fonctionnelle générale non trouvée avec l'ID: " + lineId);
                });

        Marche marche = Line.getMarche();

        if (marche == null) {
            logger.error("Aucun marché associé à la ligne ID: {}", lineId);
            throw new ResourceNotFoundException("Aucun marché associé à la ligne fonctionnelle générale avec l'ID: " + lineId);
        }

        logger.debug("Marché associé: {} (ID: {})", marche.getNom(), marche.getId());

        List<Prefinancement> allPrefi = prefinancementRepository.findByMarche(marche);
        logger.debug("{} préfinancements  trouvés", allPrefi.size());

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (Prefinancement prefi : allPrefi) {
            if (prefi.getType() == Type.REMBOURSEMENT) totalDebit = totalDebit.add(prefi.getMontant());
            else if (prefi.getType() == Type.DEBLOCAGE) totalCredit = totalCredit.add(prefi.getMontant());
        }
        BigDecimal reste = totalCredit.subtract(totalDebit);

        logger.debug("Totaux calculés - Débit: {}, Crédit: {}, Reste: {}", totalDebit, totalCredit, reste);

        Specification<Prefinancement> spec = Specification.where(PrefinancementSpecification.byMarcheId(marche.getId()));

        Page<Prefinancement> prefiPage = prefinancementRepository.findAll(spec, pageable);
        logger.debug("{} préfinancements récupérés pour la page courante", prefiPage.getNumberOfElements());

        Page<SuiviePrefiLigneResponseDto> pageOfDtos = prefiPage.map(prefinancementMapper::toSuiviePrefiLigneResponseDto);

        AgregatedSuiviePrefiLigneResponseDto aggregatedDto = new AgregatedSuiviePrefiLigneResponseDto();
        aggregatedDto.setBanqueName(Line.getMarche().getBanque().getNom());
        aggregatedDto.setMarcheName(marche.getNom());
        aggregatedDto.setMarcheNumero(marche.getNumero());
        aggregatedDto.setTotalDebit(totalDebit);
        aggregatedDto.setTotalCredit(totalCredit);
        aggregatedDto.setReste(reste);
        aggregatedDto.setPage(pageOfDtos);

        logger.info("Suivi de préfinancement récupéré avec succès pour la ligne ID: {}", lineId);
        return aggregatedDto;
    }

    @Transactional(readOnly = true)
    public AgregatedSuivieDcLigneResponseDto getSuivieDcLigneResponse(Long lineId, Pageable pageable) {
        logger.info("Récupération du suivi de droit constaté pour la ligne ID: {}", lineId);

        LigneFonctionnelle line = ligneFonctionnelleRepository.findById(lineId)
                .orElseThrow(() -> {
                    logger.error("Ligne fonctionnelle général non trouvée avec l'ID: {}", lineId);
                    return new ResourceNotFoundException("Ligne Fonctionnelle général non trouvée avec l'ID: " + lineId);
                });

        Marche marche = line.getMarche();
        if (marche == null) {
            logger.error("Aucun marché associé à la ligne ID: {}", lineId);
            throw new ResourceNotFoundException("Aucun marché associé à la ligne fonctionnelle avec l'ID: " + lineId);
        }

        logger.debug("Marché associé: {} (ID: {})", marche.getNom(), marche.getId());

        List<DroitConstate> allDc = droitConstateRepository.findByMarche(marche);
        logger.debug("{} droits constatés  trouvés", allDc.size());

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (DroitConstate dc : allDc) {
            if (dc.getType() == Type.REMBOURSEMENT) totalDebit = totalDebit.add(dc.getMontant());
            else if (dc.getType() == Type.DEBLOCAGE) totalCredit = totalCredit.add(dc.getMontant());
        }

        BigDecimal reste = totalCredit.subtract(totalDebit);

        logger.debug("Totaux calculés - Débit: {}, Crédit: {}, Reste: {}", totalDebit, totalCredit, reste);

        Specification<DroitConstate> spec = Specification.where(DroitConstateSpec.byMarcheId(marche.getId())) ;
        Page<DroitConstate> dcPage = droitConstateRepository.findAll(spec, pageable);

        logger.debug("{} droits constatés récupérés pour la page courante", dcPage.getNumberOfElements());

        Page<SuivieDcLigneResponseDto> pageOfDtos = dcPage.map(droitConstateMapper::toSuivieDcLigneResponseDto);

        AgregatedSuivieDcLigneResponseDto aggregatedDto = new AgregatedSuivieDcLigneResponseDto();
        aggregatedDto.setBanqueName(line.getMarche().getBanque().getNom());
        aggregatedDto.setMarcheName(marche.getNom());
        aggregatedDto.setMarcheNumero(marche.getNumero());
        aggregatedDto.setTotalDebit(totalDebit);
        aggregatedDto.setTotalCredit(totalCredit);
        aggregatedDto.setReste(reste);
        aggregatedDto.setPage(pageOfDtos);

        logger.info("Suivi de droit constaté récupéré avec succès pour la ligne ID: {}", lineId);
        return aggregatedDto;
    }

    @Transactional(readOnly = true)
    public AgregatedSuivieFactoringLigneResponseDto getSuivieFactoringLigneResponse(Long lineId, Pageable pageable) {
        logger.info("Récupération du suivi de factoring pour la ligne ID: {}", lineId);

        LigneFonctionnelle line = ligneFonctionnelleRepository.findById(lineId)
                .orElseThrow(() -> {
                    logger.error("Ligne fonctionnelle général non trouvée avec l'ID: {}", lineId);
                    return new ResourceNotFoundException("Ligne Fonctionnelle général non trouvée avec l'ID: " + lineId);
                });

        Marche marche = line.getMarche();
        if (marche == null) {
            logger.error("Aucun marché associé à la ligne ID: {}", lineId);
            throw new ResourceNotFoundException("Aucun marché associé à la ligne fonctionnelle avec l'ID: " + lineId);
        }

        logger.debug("Marché associé: {} (ID: {})", marche.getNom(), marche.getId());

        List<Factoring> allPaidFactoring = factoringRepository.findByMarche(marche);
        logger.debug("{} factorings payés trouvés", allPaidFactoring.size());

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (Factoring f : allPaidFactoring) {
            if (f.getType() == Type.REMBOURSEMENT) totalDebit = totalDebit.add(f.getMontant());
            else if (f.getType() == Type.DEBLOCAGE) totalCredit = totalCredit.add(f.getMontant());
        }
        BigDecimal reste = totalCredit.subtract(totalDebit);

        logger.debug("Totaux calculés - Débit: {}, Crédit: {}, Reste: {}", totalDebit, totalCredit, reste);

        Specification<Factoring> spec = Specification.where(FactoringSpec.byMarcheId(marche.getId()));
        Page<Factoring> factoringPage = factoringRepository.findAll(spec, pageable);

        logger.debug("{} factorings récupérés pour la page courante", factoringPage.getNumberOfElements());

        Page<SuivieFactoringLineResponseDto> pageOfDtos = factoringPage.map(factoringMapper::toSuivieFactoringLigneResponseDto);

        AgregatedSuivieFactoringLigneResponseDto aggregatedDto = new AgregatedSuivieFactoringLigneResponseDto();
        aggregatedDto.setBanqueName(line.getMarche().getBanque().getNom());
        aggregatedDto.setMarcheName(marche.getNom());
        aggregatedDto.setMarcheNumero(marche.getNumero());
        aggregatedDto.setTotalDebit(totalDebit);
        aggregatedDto.setTotalCredit(totalCredit);
        aggregatedDto.setReste(reste);
        aggregatedDto.setPage(pageOfDtos);

        logger.info("Suivi de factoring récupéré avec succès pour la ligne ID: {}", lineId);
        return aggregatedDto;
    }

    @Transactional(readOnly = true)
    public AgregatedSuivieLcnLigneResponseDto getSuivieLcnLigneResponse(Long lineId, Pageable pageable) {
        logger.info("Récupération du suivi LCN pour la ligne ID: {}", lineId);

        LigneFonctionnelle line = ligneFonctionnelleRepository.findById(lineId)
                .orElseThrow(() -> {
                    logger.error("Ligne fonctionnelle générale non trouvée avec l'ID: {}", lineId);
                    return new ResourceNotFoundException("Ligne Fonctionnelle générale non trouvée avec l'ID: " + lineId);
                });


        Specification<Lcn> spec = Specification.where(LcnSpecification.hasLigneFonctionnelle(lineId));

        List<Lcn> allLcn = lcnRepository.findAll(spec);
        logger.debug("{} LCN trouvés", allLcn.size());


        Page<Lcn> lcnPage = lcnRepository.findAll(spec, pageable);

        logger.debug("{} LCN récupérés pour la page courante", lcnPage.getNumberOfElements());

        Page<LcnResponseDTO> pageOfDtos = lcnPage.map(LcnMapper::toDto);

        AgregatedSuivieLcnLigneResponseDto aggregatedDto = new AgregatedSuivieLcnLigneResponseDto();
        aggregatedDto.setBanqueName(line.getBanque().getNom());
        aggregatedDto.setTotal(allLcn.stream().map(Lcn::getMontant).reduce(BigDecimal.ZERO,BigDecimal::add));
        aggregatedDto.setPage(pageOfDtos);

        logger.info("Suivi LCN récupéré avec succès pour la ligne ID: {}", lineId);
        return aggregatedDto;
    }

    private void validateDateRange(LocalDate dateDepart, LocalDate dateFin) {
        if (dateDepart != null && dateFin != null && dateFin.isBefore(dateDepart)) {
            logger.warn("Validation de date échouée: dateFin ({}) est antérieure à dateDepart ({}).", dateFin, dateDepart);
            throw new IllegalArgumentException("La date de fin ne peut pas être antérieure à la date de départ.");
        }
    }

    /**
     *
     * Vérifie qu'il n'existe pas déjà une ligne fonctionnelle active avec la même combinaison
     * Banque + Nature + Marché.
     * RÈGLE MÉTIER :
     * - Une banque ne peut avoir qu'UNE SEULE ligne générale active par nature (sans marché)
     * - Une banque peut avoir plusieurs lignes actives de la même nature SI elles sont pour des marchés différents
     * - Un marché ne peut avoir qu'UNE SEULE ligne active par nature
     */
    private void avoidDuplicateActifLines(Banque banque, NatureLigneFonctionnelle nature, Marche marche) {


        checkNoConflictWithSpecificLine(marche, nature);

        if (marche == null) {
            // Cas 1 : Ligne sans marché
            Optional<LigneFonctionnelle> existingGeneralLine = ligneFonctionnelleRepository
                    .findByBanqueIdAndNatureLigneFonctionnelleIdAndMarcheIsNullAndActifTrue(
                            banque.getId(),
                            nature.getId()
                    );

            existingGeneralLine.ifPresent(existing -> {
                logger.warn("Duplicate  active ligne detected - Banque: '{}', Nature: '{}'",
                        banque.getNom(), nature.getLabel());
                throw new DuplicateEntryException(
                        "Une ligne fonctionnelle générale ACTIVE de type '" + nature.getLabel() +
                                "' existe déjà pour la banque '" + banque.getNom() + "' (ID: " + existing.getId() + ")."
                );
            });

        } else {
            // Cas 2 : Ligne spécifique à un marché
            Optional<LigneFonctionnelle> existingMarcheLine = ligneFonctionnelleRepository
                    .findByMarcheIdAndNatureLigneFonctionnelleIdAndActifTrue(
                            marche.getId(),
                            nature.getId()
                    );

            existingMarcheLine.ifPresent(existing -> {
                logger.warn("Duplicate marche-specific active ligne detected - Marche: '{}', Nature: '{}'",
                        marche.getNumero(), nature.getLabel());
                throw new DuplicateEntryException(
                        "Une ligne fonctionnelle ACTIVE de type '" + nature.getLabel() +
                                "' existe déjà pour le marché '" + marche.getNumero() + "' (ID: " + existing.getId() + ")."
                );
            });
        }

        logger.debug("No duplicate active ligne found - validation passed");
    }

    private Marche validateAndGetMarche(Long marcheId, Long banqueId) {

        if (marcheId == null) {
            logger.debug("No marcheId provided - ligne will be general (not associated with any marche)");
            return null;
        }

        logger.debug("Validating marche ID: {} for banque ID: {}", marcheId, banqueId);

        Marche marche = marcheRepository.findById(marcheId)
                .orElseThrow(() -> {
                    logger.error("Marche not found with ID: {}", marcheId);
                    return new ResourceNotFoundException("Marché non trouvé avec l'ID: " + marcheId);
                });

        // Vérification que le marché appartient bien à la banque
        if (!marche.getBanque().getId().equals(banqueId)) {
            logger.error("Marche ID: {} belongs to banque ID: {}, but requested for banque ID: {}",
                    marcheId, marche.getBanque().getId(), banqueId);
            throw new IllegalArgumentException(
                    "Le marché '" + marche.getNumero() + "' n'appartient pas à la banque spécifiée. " +
                            "Il appartient à la banque '" + marche.getBanque().getNom() + "'."
            );
        }

        logger.debug("Marche validated successfully - ID: {}, Numero: {}, Nom: {}",
                marche.getId(), marche.getNumero(), marche.getNom());

        return marche;
    }

    /**
     * Vérifie si les propriétés qui constituent la "clé métier" ont changé.
     * La clé métier comprend : Banque + Nature + Marché
     *
     * @param existing Ligne fonctionnelle existante
     * @param newBanque Nouvelle banque
     * @param newNature Nouvelle nature
     * @param newMarche Nouveau marché (peut être null)
     * @return true si au moins une propriété de la clé a changé
     */
    private boolean hasKeyChanged(LigneFonctionnelle existing, Banque newBanque,
                                  NatureLigneFonctionnelle newNature, Marche newMarche) {

        boolean banqueChanged = !existing.getBanque().getId().equals(newBanque.getId());
        boolean natureChanged = !existing.getNatureLigneFonctionnelle().getId().equals(newNature.getId());

        // Vérification du changement de marché (plus complexe car peut être null)
        boolean marcheChanged;
        if (existing.getMarche() == null && newMarche == null) {
            marcheChanged = false; // Les deux sont null, pas de changement
        } else if (existing.getMarche() == null || newMarche == null) {
            marcheChanged = true; // L'un est null, l'autre non = changement
        } else {
            marcheChanged = !existing.getMarche().getId().equals(newMarche.getId());
        }

        if (banqueChanged) {
            logger.debug("Banque changed from ID {} to ID {}", existing.getBanque().getId(), newBanque.getId());
        }
        if (natureChanged) {
            logger.debug("Nature changed from ID {} to ID {}",
                    existing.getNatureLigneFonctionnelle().getId(), newNature.getId());
        }
        if (marcheChanged) {
            logger.debug("Marche changed from {} to {}",
                    existing.getMarche() != null ? existing.getMarche().getId() : "null",
                    newMarche != null ? newMarche.getId() : "null");
        }

        return banqueChanged || natureChanged || marcheChanged;
    }

    /**
     * Vérifie qu'il n'existe pas déjà une AUTRE ligne active avec la même clé métier.
     * Exclut la ligne en cours de modification de la recherche.
     *
     * @param currentId ID de la ligne en cours de modification
     * @param banque Banque cible
     * @param nature Nature cible
     * @param marche Marché cible (peut être null)
     */
    private void avoidDuplicateActifLinesForUpdate(Long currentId, Banque banque,
                                                   NatureLigneFonctionnelle nature, Marche marche) {


        checkNoConflictWithSpecificLine(marche, nature);

        if (marche == null) {
            // Cas 1 : Ligne générale (sans marché)
            Optional<LigneFonctionnelle> existingGeneralLine = ligneFonctionnelleRepository
                    .findByBanqueIdAndNatureLigneFonctionnelleIdAndMarcheIsNullAndActifTrue(
                            banque.getId(),
                            nature.getId()
                    );

            existingGeneralLine.ifPresent(existing -> {
                // Vérifier que ce n'est pas la ligne en cours de modification
                if (!existing.getId().equals(currentId)) {
                    logger.warn("Duplicate general active ligne detected during update - Banque: '{}', Nature: '{}', Existing ID: {}",
                            banque.getNom(), nature.getLabel(), existing.getId());
                    throw new DuplicateEntryException(
                            "Une autre ligne fonctionnelle générale ACTIVE de type '" + nature.getLabel() +
                                    "' existe déjà pour la banque '" + banque.getNom() + "' (ID: " + existing.getId() + ")."
                    );
                }
            });

        } else {
            // Cas 2 : Ligne spécifique à un marché
            Optional<LigneFonctionnelle> existingMarcheLine = ligneFonctionnelleRepository
                    .findByMarcheIdAndNatureLigneFonctionnelleIdAndActifTrue(
                            marche.getId(),
                            nature.getId()
                    );

            existingMarcheLine.ifPresent(existing -> {
                // Vérifier que ce n'est pas la ligne en cours de modification
                if (!existing.getId().equals(currentId)) {
                    logger.warn("Duplicate marche-specific active ligne detected during update - Marche: '{}', Nature: '{}', Existing ID: {}",
                            marche.getNumero(), nature.getLabel(), existing.getId());
                    throw new DuplicateEntryException(
                            "Une autre ligne fonctionnelle ACTIVE de type '" + nature.getLabel() +
                                    "' existe déjà pour le marché '" + marche.getNumero() + "' (ID: " + existing.getId() + ")."
                    );
                }
            });
        }

        logger.debug("No duplicate active ligne found for update - validation passed");
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
                ligneFonctionnelleSpecifiqueRepository.findByMarcheAndNatureLigneFonctionnelleAndActifTrue(
                        marche, nature
                );

        existingSpecificLine.ifPresent(existing -> {
            logger.error("Conflit détecté - Une LigneFonctionnelleSpecifique active existe déjà - Marché: '{}', Nature: '{}', ID: {}",
                    marche.getNumero(), nature.getLabel(), existing.getId());

            throw new DuplicateEntryException(
                    "Impossible de créer une ligne générale. " +
                            "Le marché '" + marche.getNumero() + "' possède déjà une ligne fonctionnelle SPÉCIFIQUE active " +
                            "de type '" + nature.getLabel() + "' (ID: " + existing.getId() + "). " +
                            "Un marché ne peut avoir qu'une seule ligne active par nature."
            );
        });

        logger.debug("Aucun conflit avec les lignes spécifiques - validation réussie");
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

}