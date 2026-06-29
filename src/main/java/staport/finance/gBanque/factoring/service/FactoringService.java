package staport.finance.gBanque.factoring.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories.NatureLigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.repositories.LigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.repositories.LigneFonctionnelleSpecifiqueRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.factoring.dtos.*;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.factoring.mapper.FactoringMapper;
import staport.finance.gBanque.factoring.repository.FactoringRepository;
import staport.finance.gBanque.factoring.spec.FactoringSpec;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.repositories.MarcheRepository;
import staport.finance.gBanque.prefinancement.enums.Status;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

@Service
public class FactoringService {


    private static final Logger log = LoggerFactory.getLogger(FactoringService.class);
    private final FactoringRepository factoringRepository;
    private final MarcheRepository marcheRepository;
    private final FactoringMapper factoringMapper;
    private final LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository;
    private final LigneFonctionnelleRepository ligneFonctionnelleRepository;
    private final NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository;
    LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

    public FactoringService(FactoringRepository factoringRepository, MarcheRepository marcheRepository, FactoringMapper factoringMapper, LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository, LigneFonctionnelleRepository ligneFonctionnelleRepository, NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository) {
        this.factoringRepository = factoringRepository;
        this.marcheRepository = marcheRepository;
        this.factoringMapper = factoringMapper;
        this.ligneFonctionnelleSpecifiqueRepository = ligneFonctionnelleSpecifiqueRepository;
        this.ligneFonctionnelleRepository = ligneFonctionnelleRepository;
        this.natureLigneFonctionnelleRepository = natureLigneFonctionnelleRepository;
        log.info("FactoringService initialisé avec succès");
    }

    @Transactional
    public FactoringResponseDto createFactoring(FactoringRequestDto requestDTO) throws Exception {
        log.info("Début de la création d'un factoring pour le marché ID: {}", requestDTO.getMarcheId());
        log.debug("Données reçues: {}", requestDTO);

        Marche marche = marcheRepository.findById(requestDTO.getMarcheId())
                .orElseThrow(() -> {
                    log.error("Marché non trouvé avec l'ID: {}", requestDTO.getMarcheId());
                    return new ResourceNotFoundException("Marche not found with ID: " + requestDTO.getMarcheId());
                });

        log.debug("Marché récupéré: {} (Numéro: {})", marche.getNom(), marche.getNumero());

        // vérifiez si le marché a une ligne spécifique actif de nature Factoring

        // Step 1: Find the "Factoring" nature line by its label
        log.debug("Recherche de la nature de ligne fonctionnelle 'Factoring'");
        NatureLigneFonctionnelle factoringNature = getFactoringNature();
        validateActiveFactoringLine(marche, factoringNature);


        Factoring factoring = factoringMapper.toEntity(requestDTO, marche);
        factoring = factoringRepository.save(factoring);

        log.info("Factoring créé avec succès - ID: {}, Type: {}, Montant: {}",
                factoring.getId(), factoring.getType(), factoring.getMontant());

        return factoringMapper.toDto(factoring);
    }

    @Transactional
    public FactoringResponseDto updateFactoring(Long id, FactoringRequestDto requestDTO) throws Exception {
        log.info("Début de la modification du factoring ID: {}", id);
        log.debug("Nouvelles données reçues: {}", requestDTO);

        Factoring existingFactoring = factoringRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Factoring non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Factoring not found with ID: " + id);
                });

        log.debug("Factoring existant récupéré - Ancien montant: {}, Ancien type: {}",
                existingFactoring.getMontant(), existingFactoring.getType());

        Marche marche = marcheRepository.findById(requestDTO.getMarcheId())
                .orElseThrow(() -> {
                    log.error("Marché non trouvé avec l'ID: {}", requestDTO.getMarcheId());
                    return new ResourceNotFoundException("Marche not found with ID: " + requestDTO.getMarcheId());
                });

        log.debug("Marché récupéré pour la mise à jour: {}", marche.getNom());
        if (!existingFactoring.getMarche().getId().equals(marche.getId())) {
            log.warn("Changement de marché détecté (ancien: {}, nouveau: {}), re-validation nécessaire",
                    existingFactoring.getMarche().getNom(), marche.getNom());

            NatureLigneFonctionnelle factoringNature = getFactoringNature();
            validateActiveFactoringLine(marche, factoringNature);
        }
        existingFactoring.setMarche(marche);
        existingFactoring.setDateEmission(requestDTO.getDateEmission());
        existingFactoring.setMontant(requestDTO.getMontant());
        existingFactoring.setType(requestDTO.getType());
        existingFactoring.setObservation("Marche: " + marche.getNumero() );
        Factoring updatedFactoring = factoringRepository.save(existingFactoring);

        log.info("Factoring modifié avec succès - ID: {}, Nouveau montant: {}, Nouveau type: {}",
                id, updatedFactoring.getMontant(), updatedFactoring.getType());

        return factoringMapper.toDto(updatedFactoring);
    }

    @Transactional
    public void deleteFactoring(Long id) {
        log.info("Tentative de suppression du factoring ID: {}", id);

        if (!factoringRepository.existsById(id)) {
            log.error("Impossible de supprimer - Factoring non trouvé avec l'ID: {}", id);
            throw new ResourceNotFoundException("Factoring not found with ID: " + id);
        }

        factoringRepository.deleteById(id);
        log.info("Factoring ID: {} supprimé avec succès", id);
    }

    @Transactional(readOnly = true)
    public FactoringResponseDto findById(Long id) {
        log.debug("Recherche du factoring avec l'ID: {}", id);

        Factoring factoring = factoringRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Factoring non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Factoring not found with ID: " + id);
                });

        log.debug("Factoring trouvé - Marché: {}, Montant: {}",
                factoring.getMarche().getNom(), factoring.getMontant());

        return factoringMapper.toDto(factoring);
    }

    @Transactional(readOnly = true)
    public Page<FactoringResponseDto> findAll(FactoringSearchDto searchDTO, Pageable pageable) {
        log.info("Recherche de factorings avec critères: {}", searchDTO);
        log.debug("Pagination demandée - Page: {}, Taille: {}", pageable.getPageNumber(), pageable.getPageSize());

        Specification<Factoring> spec = FactoringSpec.bySearchCriteria(searchDTO);
        Page<Factoring> factoringPage = factoringRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.info("Recherche terminée - {} résultat(s) trouvé(s) sur {} élément(s) au total",
                factoringPage.getNumberOfElements(), factoringPage.getTotalElements());

        return factoringPage.map(factoringMapper::toDto);
    }

    @Transactional
    public FactoringResponseDto toggleStatus(Long id ) {
        log.info("Changement du statut du factoring ID: {}", id);

        Factoring factoring = factoringRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Factoring non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Factoring not found with ID: " + id);
                });

        Status existingStatus = factoring.getStatus();
        Status newStatus = existingStatus == Status.PAYE ? Status.PREVU : Status.PAYE;

        log.debug("Changement de statut - Ancien: {}, Nouveau: {}", existingStatus, newStatus);

        factoring.setStatus(newStatus);
        Factoring updatedFactoring = factoringRepository.save(factoring);

        log.info("Statut du factoring ID: {} modifié avec succès de {} vers {}",
                id, existingStatus, newStatus);

        return factoringMapper.toDto(updatedFactoring);
    }

    @Transactional(readOnly = true)
    public EncoursFactoringDto getRemboursementEncoursForBanque(EncoursFactoringSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Récupération des encours de remboursement factoring pour la banque ID: {}", banqueId);
        log.debug("Critères de recherche: {}", searchDto);

        // Build the specification to filter the records
        Specification<Factoring> spec = FactoringSpec.byBanqueId(banqueId)
                .and(FactoringSpec.outstandingRemboursementUntil(endOfMonth))
                .and(FactoringSpec.byMarcheName(searchDto.getMarcheName()))
                .and(FactoringSpec.byMarcheNumero(searchDto.getMarcheNumero()))
                .and(FactoringSpec.hasMontant(searchDto.getMontant()));

        // Fetch the paginated list of factoring records
        Page<Factoring> factoringPage = factoringRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.debug("Nombre de résultats paginés: {}", factoringPage.getNumberOfElements());

        // Fetch the total sum of the montant for the same criteria
        BigDecimal totalDesEncours = factoringRepository.findAll(spec).stream()
                .map(Factoring::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total des encours de remboursement factoring calculé: {} pour {} enregistrement(s)",
                totalDesEncours, factoringPage.getTotalElements());

        // Map the page of entities to a page of DTOs
        Page<FactoringResponseDto> pageDto = factoringPage.map(factoringMapper::toDto);

        // Create and return the final DTO
        EncoursFactoringDto result = new EncoursFactoringDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        return result;
    }

    @Transactional(readOnly = true)
    public EncoursFactoringDto getDeblocageEncoursForBanque(EncoursFactoringSearchDto searchDto,Long banqueId, Pageable pageable) {
        log.info("Récupération des encours de déblocage factoring pour la banque ID: {}", banqueId);
        log.debug("Critères de recherche: {}", searchDto);

        // Build the specification to filter the records
        Specification<Factoring> spec = FactoringSpec.byBanqueId(banqueId)
                .and(FactoringSpec.outstandingDeblocageUntil(endOfMonth))
                .and(FactoringSpec.byMarcheName(searchDto.getMarcheName()))
                .and(FactoringSpec.byMarcheNumero(searchDto.getMarcheNumero()))
                .and(FactoringSpec.hasMontant(searchDto.getMontant()));

        // Fetch the paginated list of factoring records
        Page<Factoring> factoringPage = factoringRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.debug("Nombre de résultats paginés: {}", factoringPage.getNumberOfElements());

        // Fetch the total sum of the montant for the same criteria
        BigDecimal totalDesEncours = factoringRepository.findAll(spec).stream()
                .map(Factoring::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total des encours de déblocage factoring calculé: {} pour {} enregistrement(s)",
                totalDesEncours, factoringPage.getTotalElements());

        // Map the page of entities to a page of DTOs
        Page<FactoringResponseDto> pageDto = factoringPage.map(factoringMapper::toDto);

        // Create and return the final DTO
        EncoursFactoringDto result = new EncoursFactoringDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        return result;
    }


    private NatureLigneFonctionnelle getFactoringNature() {
        log.debug("Recherche de la nature de ligne fonctionnelle Factoring");
        return natureLigneFonctionnelleRepository.findByLabel("Factoring")
                .orElseThrow(() -> {
                    log.error("Nature de ligne fonctionnelle Factoring non trouvée");
                    return new ResourceNotFoundException(
                            "Nature de ligne fonctionnelle Factoring non trouvée."
                    );
                });
    }
    /**
     * Validates if a Marche has an active "Factoring" line.
     * Searches in LigneFonctionnelleSpecifique (priority), then LigneFonctionnelle (fallback).
     *
     * @param marche The Marche to validate
     * @param factoringNature The "Factoring" nature
     * @throws Exception if no active line is found
     */
    private void validateActiveFactoringLine(Marche marche, NatureLigneFonctionnelle factoringNature) throws Exception {
        log.debug("Validation de l'existence d'une ligne Factoring active pour le marché ID: {}", marche.getId());

        // ÉTAPE 1 : LigneFonctionnelleSpecifique (PRIORITAIRE)
        Optional<LigneFonctionnelleSpecifique> ligneSpecifique =
                ligneFonctionnelleSpecifiqueRepository.findByMarcheAndNatureLigneFonctionnelleAndActifTrue(
                        marche, factoringNature
                );

        if (ligneSpecifique.isPresent()) {
            log.info(" Ligne fonctionnelle spécifique Factoring active trouvée pour le marché '{}' (ID: {})", marche.getNom(), ligneSpecifique.get().getId());
            return;
        }

        log.debug("Aucune ligne spécifique trouvée, recherche dans les lignes générales...");

        // ÉTAPE 2 : LigneFonctionnelle liée au marché spécifique
        Optional<LigneFonctionnelle> ligneGeneraleMarcheSpecifique =
                ligneFonctionnelleRepository.findActiveLineForMarche(
                        marche.getId(),
                        factoringNature.getId()
                );

        if (ligneGeneraleMarcheSpecifique.isPresent()) {
            log.info(" Ligne fonctionnelle générale Factoring active trouvée pour le marché spécifique '{}' (ID: {})", marche.getNom(), ligneGeneraleMarcheSpecifique.get().getId());
            return;
        }

        log.debug("Aucune ligne générale spécifique au marché trouvée, recherche de ligne générale de la banque...");

        // ÉTAPE 3 : LigneFonctionnelle générale de la banque
        Optional<LigneFonctionnelle> ligneGeneraleBanque =
                ligneFonctionnelleRepository.findActiveGeneralLine(
                        marche.getBanque().getId(),
                        factoringNature.getId()
                );

        if (ligneGeneraleBanque.isPresent()) {
            log.info(" Ligne fonctionnelle générale Factoring active trouvée pour la banque '{}' (ID: {})", marche.getBanque().getNom(), ligneGeneraleBanque.get().getId());
            return;
        }

        // ÉTAPE 4 : Aucune ligne active trouvée
        log.error(" Aucune ligne fonctionnelle Factoring active trouvée pour le marché '{}'",marche.getNom());

        throw new Exception(
                "Impossible de créer un factoring. Le marché '" + marche.getNom() +
                        "' n'est pas associé à une ligne fonctionnelle Factoring active. " +
                        "Veuillez créer une ligne spécifique au marché ou une ligne générale pour la banque '" +
                        marche.getBanque().getNom() + "'."
        );
    }

}