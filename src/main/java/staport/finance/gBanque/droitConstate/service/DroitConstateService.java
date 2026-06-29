package staport.finance.gBanque.droitConstate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories.NatureLigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.repositories.LigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.repositories.LigneFonctionnelleSpecifiqueRepository;
import staport.finance.gBanque.droitConstate.dtos.*;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.droitConstate.mapper.DroitConstateMapper;
import staport.finance.gBanque.droitConstate.repositories.DroitConstateRepository;
import staport.finance.gBanque.droitConstate.spec.DroitConstateSpec;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.repositories.MarcheRepository;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

@Service
public class DroitConstateService {


    private static final Logger log = LoggerFactory.getLogger(DroitConstateService.class);
    private final DroitConstateRepository droitConstateRepository;
    private final DroitConstateMapper droitConstateMapper;
    private final MarcheRepository marcheRepository;
    private final BanqueRepository banqueRepository;
    private final NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository;
    private final LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository;
    private final LigneFonctionnelleRepository ligneFonctionnelleRepository;
    LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());


    public DroitConstateService(DroitConstateRepository droitConstateRepository,
                                DroitConstateMapper droitConstateMapper,
                                MarcheRepository marcheRepository,
                                BanqueRepository banqueRepository,
                                NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository,
                                LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository,
                                LigneFonctionnelleRepository ligneFonctionnelleRepository) {
        this.droitConstateRepository = droitConstateRepository;
        this.droitConstateMapper = droitConstateMapper;
        this.marcheRepository = marcheRepository;
        this.banqueRepository = banqueRepository;
        this.natureLigneFonctionnelleRepository = natureLigneFonctionnelleRepository;
        this.ligneFonctionnelleSpecifiqueRepository = ligneFonctionnelleSpecifiqueRepository;
        this.ligneFonctionnelleRepository = ligneFonctionnelleRepository;
    }

    /**
     * Résout la banque à associer à un DroitConstate.
     * Le banqueId est obligatoire — aucun fallback sur marche.banque.
     */
    private Banque resolveBanque(Long banqueId) {
        if (banqueId == null) {
            throw new ResourceNotFoundException("La banque est obligatoire pour un droit constaté.");
        }
        return banqueRepository.findById(banqueId)
                .orElseThrow(() -> {
                    log.error("Banque non trouvée avec l'ID: {}", banqueId);
                    return new ResourceNotFoundException("Banque non trouvée avec l'ID: " + banqueId);
                });
    }

    @Transactional
    public DroitConstateResponseDto createDroitConstate(DroitConstateRequestDto requestDTO) throws Exception {

        log.info("Début de la création d'un droit constaté pour le marché ID: {}", requestDTO.getMarcheId());
        log.debug("Données reçues: {}", requestDTO);

        Marche marche = marcheRepository.findById(requestDTO.getMarcheId())
                .orElseThrow(() -> {
                    log.error("Marché non trouvé avec l'ID: {}", requestDTO.getMarcheId());
                    return new ResourceNotFoundException("Marche not found with ID: " + requestDTO.getMarcheId());
                });

        log.debug("Marché récupéré: {} (Numéro: {})", marche.getNom(), marche.getNumero());

        // Step 1: Find the "Droit constaté" nature line by its label
        log.debug("Recherche de la nature de ligne fonctionnelle 'Droit constaté'");
        NatureLigneFonctionnelle dcNature = natureLigneFonctionnelleRepository.findByLabel("Droit constaté")
                .orElseThrow(() -> {
                    log.error("Nature de ligne fonctionnelle 'Droit constaté' non trouvée");
                    return new ResourceNotFoundException("Nature de ligne fonctionnelle 'Droit constaté' non trouvée.");
                });

        log.debug("Nature de ligne fonctionnelle trouvée: ID {}", dcNature.getId());

        // Step 2: Check for an active 'Droit constaté' specific line for the Marche
        log.debug("Vérification de l'existence d'une ligne fonctionnelle 'Droit constaté' active pour le marché");
        validateActiveDroitConstateLine(marche, dcNature);

        // Step 3: Resolve the bank (mandatory — no fallback)
        Banque banque = resolveBanque(requestDTO.getBanqueId());
        log.debug("Banque résolue pour le DC: {} (ID: {})", banque.getNom(), banque.getId());

        DroitConstate droitConstate = droitConstateMapper.toEntity(requestDTO, marche, banque);

        droitConstate = droitConstateRepository.save(droitConstate);

        log.info("Droit constaté créé avec succès - ID: {}, Type: {}, Montant: {}",
                droitConstate.getId(), droitConstate.getType(), droitConstate.getMontant());

        return droitConstateMapper.toDto(droitConstate);
    }

    @Transactional
    public DroitConstateResponseDto updateDroitConstate(Long id, DroitConstateRequestDto requestDTO) throws Exception {
        log.info("Début de la modification du droit constaté ID: {}", id);
        log.debug("Nouvelles données reçues: {}", requestDTO);

        DroitConstate existingDroitConstate = droitConstateRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Droit constaté non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Droit constaté not found with ID: " + id);
                });

        log.debug("Droit constaté existant récupéré - Ancien montant: {}, Ancien type: {}",
                existingDroitConstate.getMontant(), existingDroitConstate.getType());

        Marche marche = marcheRepository.findById(requestDTO.getMarcheId())
                .orElseThrow(() -> {
                    log.error("Marché non trouvé avec l'ID: {}", requestDTO.getMarcheId());
                    return new ResourceNotFoundException("Marche not found with ID: " + requestDTO.getMarcheId());
                });

        log.debug("Marché récupéré pour la mise à jour: {}", marche.getNom());

        if (!existingDroitConstate.getMarche().getId().equals(marche.getId())) {
            log.warn(" Changement de marché détecté (ancien: {}, nouveau: {}), re-validation nécessaire",
                    existingDroitConstate.getMarche().getNom(), marche.getNom());

            NatureLigneFonctionnelle dcNature = natureLigneFonctionnelleRepository.findByLabel("Droit constaté")
                    .orElseThrow(() -> new ResourceNotFoundException("Nature 'Droit constaté' non trouvée"));

            validateActiveDroitConstateLine(marche, dcNature);
        }
        // Resolve bank (mandatory — no fallback)
        Banque banque = resolveBanque(requestDTO.getBanqueId());
        log.debug("Banque résolue pour la mise à jour du DC: {} (ID: {})", banque.getNom(), banque.getId());

        existingDroitConstate.setMarche(marche);
        existingDroitConstate.setBanque(banque);
        existingDroitConstate.setDateEmission(requestDTO.getDateEmission());
        existingDroitConstate.setMontant(requestDTO.getMontant());
        existingDroitConstate.setType(requestDTO.getType());
        existingDroitConstate.setObservation("Marche: " + marche.getNumero());

        DroitConstate updatedPrefinancement = droitConstateRepository.save(existingDroitConstate);

        log.info("Droit constaté modifié avec succès - ID: {}, Nouveau montant: {}, Nouveau type: {}",
                id, updatedPrefinancement.getMontant(), updatedPrefinancement.getType());

        return droitConstateMapper.toDto(updatedPrefinancement);
    }

    @Transactional
    public void deleteDroitConstate(Long id) {
        log.info("Tentative de suppression du droit constaté ID: {}", id);

        if (!droitConstateRepository.existsById(id)) {
            log.error("Impossible de supprimer - Droit constaté non trouvé avec l'ID: {}", id);
            throw new ResourceNotFoundException("Droit constate not found with ID: " + id);
        }

        droitConstateRepository.deleteById(id);
        log.info("Droit constaté ID: {} supprimé avec succès", id);
    }

    @Transactional(readOnly = true)
    public DroitConstateResponseDto findById(Long id) {
        log.debug("Recherche du droit constaté avec l'ID: {}", id);

        DroitConstate droitConstate = droitConstateRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Droit constaté non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Droit constaté not found with ID: " + id);
                });

        log.debug("Droit constaté trouvé - Marché: {}, Montant: {}",
                droitConstate.getMarche().getNom(), droitConstate.getMontant());

        return droitConstateMapper.toDto(droitConstate);
    }

    @Transactional(readOnly = true)
    public Page<DroitConstateResponseDto> findAll(DroitConstateSearchDto searchDTO, Pageable pageable) {
        log.info("Recherche de droits constatés avec critères: {}", searchDTO);
        log.debug("Pagination demandée - Page: {}, Taille: {}", pageable.getPageNumber(), pageable.getPageSize());

        Specification<DroitConstate> spec = DroitConstateSpec.bySearchCriteria(searchDTO);
        Page<DroitConstate> droitConstatePage = droitConstateRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.info("Recherche terminée - {} résultat(s) trouvé(s) sur {} élément(s) au total",
                droitConstatePage.getNumberOfElements(), droitConstatePage.getTotalElements());

        return droitConstatePage.map(droitConstateMapper::toDto);
    }

    @Transactional
    public DroitConstateResponseDto toggleStatus(Long id ) {
        log.info("Changement du statut du droit constaté ID: {}", id);

        DroitConstate droitConstate = droitConstateRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Droit constaté non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Droit constate not found with ID: " + id);
                });

        Status existingStatus = droitConstate.getStatus();
        Status newStatus = existingStatus == Status.PAYE ? Status.PREVU : Status.PAYE;

        log.debug("Changement de statut - Ancien: {}, Nouveau: {}", existingStatus, newStatus);

        droitConstate.setStatus(newStatus);
        DroitConstate updatedDroitConstate = droitConstateRepository.save(droitConstate);

        log.info("Statut du droit constaté ID: {} modifié avec succès de {} vers {}",
                id, existingStatus, newStatus);

        return droitConstateMapper.toDto(updatedDroitConstate);
    }

    @Transactional(readOnly = true)
    public EncoursDroitConstateDto getRemboursementEncoursForBanque(EncoursDroitConstateSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Récupération des encours de remboursement pour la banque ID: {}", banqueId);
        log.debug("Critères de recherche: {}", searchDto);

        // Build the specification to filter the records
        Specification<DroitConstate> spec = DroitConstateSpec.byBanqueId(banqueId)
                .and(DroitConstateSpec.outstandingRemboursementUntil(endOfMonth))
                .and(DroitConstateSpec.byMarcheName(searchDto.getMarcheName()))
                .and(DroitConstateSpec.byMarcheNumero(searchDto.getMarcheNumero()))
                .and(DroitConstateSpec.hasMontant(searchDto.getMontant()));

        // Fetch the paginated list of droit constate records
        Page<DroitConstate> droitConstatePage = droitConstateRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.debug("Nombre de résultats paginés: {}", droitConstatePage.getNumberOfElements());

        // Fetch the total sum of the montant for the same criteria
        BigDecimal totalDesEncours = droitConstateRepository.findAll(spec).stream()
                .map(DroitConstate::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total des encours de remboursement calculé: {} pour {} enregistrement(s)",
                totalDesEncours, droitConstatePage.getTotalElements());

        // Map the page of entities to a page of DTOs
        Page<DroitConstateResponseDto> pageDto = droitConstatePage.map(droitConstateMapper::toDto);

        // Create and return the final DTO
        EncoursDroitConstateDto result = new EncoursDroitConstateDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        return result;
    }

    @Transactional(readOnly = true)
    public EncoursDroitConstateDto getDeblocageEncoursForBanque(EncoursDroitConstateSearchDto searchDto,Long banqueId, Pageable pageable) {
        log.info("Récupération des encours de déblocage pour la banque ID: {}", banqueId);
        log.debug("Critères de recherche: {}", searchDto);

        // Build the specification to filter the records
        Specification<DroitConstate> spec = DroitConstateSpec.byBanqueId(banqueId)
                .and(DroitConstateSpec.outstandingDeblocageUntil(endOfMonth))
                .and(DroitConstateSpec.byMarcheName(searchDto.getMarcheName()))
                .and(DroitConstateSpec.byMarcheNumero(searchDto.getMarcheNumero()))
                .and(DroitConstateSpec.hasMontant(searchDto.getMontant()));

        // Fetch the paginated list of droit constate records
        Page<DroitConstate> droitConstatePage = droitConstateRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.debug("Nombre de résultats paginés: {}", droitConstatePage.getNumberOfElements());

        // Fetch the total sum of the montant for the same criteria
        BigDecimal totalDesEncours = droitConstateRepository.findAll(spec).stream()
                .map(DroitConstate::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total des encours de déblocage calculé: {} pour {} enregistrement(s)",
                totalDesEncours, droitConstatePage.getTotalElements());

        // Map the page of entities to a page of DTOs
        Page<DroitConstateResponseDto> pageDto = droitConstatePage.map(droitConstateMapper::toDto);

        // Create and return the final DTO
        EncoursDroitConstateDto result = new EncoursDroitConstateDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        return result;
    }


    /**
     * Validates if a Marche has an active "Droit constaté" line.
     * Searches in both LigneFonctionnelleSpecifique (priority) and LigneFonctionnelle (fallback).
     *
     * @param marche The Marche to validate
     * @param dcNature The "Droit constaté" nature
     * @throws Exception if no active line is found
     */
    private void validateActiveDroitConstateLine(Marche marche, NatureLigneFonctionnelle dcNature) throws Exception {
        log.debug("Validation de l'existence d'une ligne 'Droit constaté' active pour le marché ID: {}", marche.getId());

        //  ÉTAPE 1 : Chercher d'abord dans LigneFonctionnelleSpecifique (prioritaire)
        Optional<LigneFonctionnelleSpecifique> ligneSpecifique =
                ligneFonctionnelleSpecifiqueRepository.findByMarcheAndNatureLigneFonctionnelleAndActifTrue(marche, dcNature);

        if (ligneSpecifique.isPresent()) {
            log.info(" Ligne fonctionnelle spécifique 'Droit constaté' active trouvée pour le marché '{}' (ID: {})",
                    marche.getNom(), ligneSpecifique.get().getId());
            return; // Validation réussie
        }

        log.debug("Aucune ligne spécifique trouvée, recherche dans les lignes générales...");

        // ÉTAPE 2 : Chercher dans LigneFonctionnelle (ligne générale de la banque)
        // Option A : Ligne générale liée à CE marché spécifique
        Optional<LigneFonctionnelle> ligneGeneraleMarcheSpecifique =
                ligneFonctionnelleRepository.findByMarcheIdAndNatureLigneFonctionnelleIdAndActifTrue(
                        marche.getId(),
                        dcNature.getId()
                );



        if (ligneGeneraleMarcheSpecifique.isPresent()) {
            log.info(" Ligne fonctionnelle générale 'Droit constaté' active trouvée pour le marché spécifique '{}' (ID: {})",
                    marche.getNom(), ligneGeneraleMarcheSpecifique.get().getId());
            return; // Validation réussie
        }

        log.debug("Aucune ligne générale spécifique au marché trouvée, recherche de ligne générale de la banque...");

        // Option B : Ligne générale de la banque (sans marché spécifique)
        Optional<LigneFonctionnelle> ligneGeneraleBanque =
                ligneFonctionnelleRepository.findByBanqueIdAndNatureLigneFonctionnelleIdAndActifTrue(
                        marche.getBanque().getId(),
                        dcNature.getId()
                );

        if (ligneGeneraleBanque.isPresent()) {
            log.info(" Ligne fonctionnelle générale 'Droit constaté' active trouvée pour la banque '{}' (ID: {})",
                    marche.getBanque().getNom(), ligneGeneraleBanque.get().getId());
            return; // Validation réussie
        }

        //  ÉTAPE 3 : Aucune ligne active trouvée
        log.error(" Aucune ligne fonctionnelle 'Droit constaté' active trouvée pour le marché '{}'", marche.getNom());
        throw new Exception(
                "Impossible de créer un droit constaté. Le marché '" + marche.getNom() +
                        "' n'est pas associé à une ligne fonctionnelle 'Droit constaté' active. " +
                        "Veuillez créer une ligne spécifique au marché ou une ligne générale pour la banque '" +
                        marche.getBanque().getNom() + "'."
        );
    }


}
