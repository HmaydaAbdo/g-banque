package staport.finance.gBanque.prefinancement.services;

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
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.repositories.MarcheRepository;
import staport.finance.gBanque.prefinancement.dtos.*;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.mapper.PrefinancementMapper;
import staport.finance.gBanque.prefinancement.repository.PrefinancementRepository;
import staport.finance.gBanque.prefinancement.spec.PrefinancementSpecification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;


@Service
public class PrefinancementService {

    private static final Logger log = LoggerFactory.getLogger(PrefinancementService.class);
    private final PrefinancementRepository prefinancementRepository;
    private final MarcheRepository marcheRepository;
    private final PrefinancementMapper prefinancementMapper;
    private final LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository;
    private final NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository;
    private final LigneFonctionnelleRepository ligneFonctionnelleRepository;
    LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

    public PrefinancementService(
            PrefinancementRepository prefinancementRepository,
            MarcheRepository marcheRepository,
            PrefinancementMapper prefinancementMapper,
            LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository,
            NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository, LigneFonctionnelleRepository ligneFonctionnelleRepository) {
        this.prefinancementRepository = prefinancementRepository;
        this.marcheRepository = marcheRepository;
        this.prefinancementMapper = prefinancementMapper;
        this.ligneFonctionnelleSpecifiqueRepository = ligneFonctionnelleSpecifiqueRepository;
        this.natureLigneFonctionnelleRepository = natureLigneFonctionnelleRepository;
        this.ligneFonctionnelleRepository = ligneFonctionnelleRepository;
        log.info("PrefinancementService initialisé avec succès");
    }

    @Transactional
    public PrefinancementResponseDTO createPrefinancement(PrefinancementRequestDTO requestDTO) throws Exception {
        log.info("Début de création d'un préfinancement pour le marché ID: {}", requestDTO.getMarcheId());
        log.debug("Détails de la requête - Type: {}, Montant: {}, Date d'émission: {}",
                requestDTO.getType(), requestDTO.getMontant(), requestDTO.getDateEmission());

        Marche marche = marcheRepository.findById(requestDTO.getMarcheId())
                .orElseThrow(() -> {
                    log.error("Marché non trouvé avec l'ID: {}", requestDTO.getMarcheId());
                    return new ResourceNotFoundException("Marche not found with ID: " + requestDTO.getMarcheId());
                });

        log.debug("Marché trouvé - Nom: {}, Numéro: {}", marche.getNom(), marche.getNumero());

        NatureLigneFonctionnelle prefiNature = getPrefinancementNature();
        validateActivePrefinancementLine(marche, prefiNature);

        Prefinancement prefinancement = prefinancementMapper.toEntity(requestDTO, marche);
        log.debug("Entité Prefinancement créée, prête à être sauvegardée");

        prefinancement = prefinancementRepository.save(prefinancement);
        log.info("Préfinancement créé avec succès - ID: {}, Montant: {}", prefinancement.getId(), prefinancement.getMontant());

        PrefinancementResponseDTO responseDTO = prefinancementMapper.toDto(prefinancement);
        log.debug("DTO de réponse créé avec succès pour le préfinancement ID: {}", prefinancement.getId());

        return responseDTO;
    }

    @Transactional
    public PrefinancementResponseDTO updatePrefinancement(Long id, PrefinancementRequestDTO requestDTO) throws Exception {
        log.info("Début de mise à jour du préfinancement ID: {}", id);
        log.debug("Nouvelles données - Marché ID: {}, Type: {}, Montant: {}",
                requestDTO.getMarcheId(), requestDTO.getType(), requestDTO.getMontant());

        Prefinancement existingPrefinancement = prefinancementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Préfinancement non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Prefinancement not found with ID: " + id);
                });

        log.debug("Préfinancement existant trouvé - Ancien montant: {}, Ancien type: {}",
                existingPrefinancement.getMontant(), existingPrefinancement.getType());

        Marche marche = marcheRepository.findById(requestDTO.getMarcheId())
                .orElseThrow(() -> {
                    log.error("Marché non trouvé avec l'ID: {}", requestDTO.getMarcheId());
                    return new ResourceNotFoundException("Marche not found with ID: " + requestDTO.getMarcheId());
                });

        log.debug("Marché trouvé pour mise à jour - Nom: {}, Numéro: {}", marche.getNom(), marche.getNumero());

        if (!existingPrefinancement.getMarche().getId().equals(marche.getId())) {
            log.warn("Changement de marché détecté (ancien: {}, nouveau: {}), re-validation nécessaire",
                    existingPrefinancement.getMarche().getNom(), marche.getNom());

            NatureLigneFonctionnelle prefiNature = getPrefinancementNature();
            validateActivePrefinancementLine(marche, prefiNature);
        }

        existingPrefinancement.setMarche(marche);
        existingPrefinancement.setDateEmission(requestDTO.getDateEmission());
        existingPrefinancement.setMontant(requestDTO.getMontant());
        existingPrefinancement.setType(requestDTO.getType());
        existingPrefinancement.setObservation("Marche: " + marche.getNumero());

        log.debug("Propriétés du préfinancement mises à jour, sauvegarde en cours");

        Prefinancement updatedPrefinancement = prefinancementRepository.save(existingPrefinancement);
        log.info("Préfinancement mis à jour avec succès - ID: {}, Nouveau montant: {}",
                updatedPrefinancement.getId(), updatedPrefinancement.getMontant());

        PrefinancementResponseDTO responseDTO = prefinancementMapper.toDto(updatedPrefinancement);
        log.debug("DTO de réponse créé avec succès pour la mise à jour");

        return responseDTO;
    }



    @Transactional
    public void deletePrefinancement(Long id) {
        log.info("Début de suppression du préfinancement ID: {}", id);

        if (!prefinancementRepository.existsById(id)) {
            log.error("Impossible de supprimer - Préfinancement non trouvé avec l'ID: {}", id);
            throw new ResourceNotFoundException("Prefinancement not found with ID: " + id);
        }

        log.debug("Préfinancement trouvé, suppression en cours");
        prefinancementRepository.deleteById(id);
        log.info("Préfinancement supprimé avec succès - ID: {}", id);
    }

    @Transactional(readOnly = true)
    public PrefinancementResponseDTO findById(Long id) {
        log.info("Recherche du préfinancement ID: {}", id);

        Prefinancement prefinancement = prefinancementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Préfinancement non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Prefinancement not found with ID: " + id);
                });

        log.debug("Préfinancement trouvé - Montant: {}, Type: {}, Statut: {}",
                prefinancement.getMontant(), prefinancement.getType(), prefinancement.getStatus());

        PrefinancementResponseDTO responseDTO = prefinancementMapper.toDto(prefinancement);
        log.debug("DTO de réponse créé avec succès");

        return responseDTO;
    }

    @Transactional(readOnly = true)
    public Page<PrefinancementResponseDTO> findAll(PrefinancementSearchDTO searchDTO, Pageable pageable) {
        log.info("Recherche de préfinancement avec critères - Marché ID: {}, Banque ID: {}, Type: {}, Statut: {}",
                searchDTO.getMarcheId(), searchDTO.getBanqueId(), searchDTO.getType(), searchDTO.getStatus());
        log.debug("Pagination - Page: {}, Taille: {}", pageable.getPageNumber(), pageable.getPageSize());

        Specification<Prefinancement> spec = PrefinancementSpecification.bySearchCriteria(searchDTO);
        log.debug("Spécification de recherche créée");

        Page<Prefinancement> prefinancementPage = prefinancementRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.info("Recherche terminée - {} enregistrement(s) trouvé(s) sur {} page(s)",
                prefinancementPage.getTotalElements(), prefinancementPage.getTotalPages());
        log.debug("Nombre d'éléments dans la page actuelle: {}", prefinancementPage.getNumberOfElements());

        Page<PrefinancementResponseDTO> result = prefinancementPage.map(prefinancementMapper::toDto);
        log.debug("Conversion des entités en DTOs terminée");

        return result;
    }

    @Transactional
    public PrefinancementResponseDTO toggleStatus(Long id) {
        log.info("Début de basculement du statut pour le préfinancement ID: {}", id);

        Prefinancement prefinancement = prefinancementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Préfinancement non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Prefinancement not found with ID: " + id);
                });

        Status existingStatus = prefinancement.getStatus();
        Status newStatus = existingStatus == Status.PAYE ? Status.PREVU : Status.PAYE;

        log.debug("Changement de statut - Ancien: {}, Nouveau: {}", existingStatus, newStatus);
        prefinancement.setStatus(newStatus);

        Prefinancement updatedPrefinancement = prefinancementRepository.save(prefinancement);
        log.info("Statut du préfinancement basculé avec succès - ID: {}, Nouveau statut: {}",
                updatedPrefinancement.getId(), updatedPrefinancement.getStatus());

        PrefinancementResponseDTO responseDTO = prefinancementMapper.toDto(updatedPrefinancement);
        log.debug("DTO de réponse créé avec succès");

        return responseDTO;
    }

    @Transactional(readOnly = true)
    public EncoursPrefinancementDto getRemboursementEncoursForBanque(EncoursPrefinancementSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Recherche des encours de remboursement pour la banque ID: {}", banqueId);
        log.debug("Critères de recherche - Nom marché: {}, Numéro marché: {}, Montant: {}",
                searchDto.getMarcheName(), searchDto.getMarcheNumero(), searchDto.getMontant());
        log.debug("Pagination - Page: {}, Taille: {}", pageable.getPageNumber(), pageable.getPageSize());

        Specification<Prefinancement> spec = PrefinancementSpecification.byBanqueId(banqueId)
                .and(PrefinancementSpecification.outstandingRemboursementUntil(endOfMonth))
                .and(PrefinancementSpecification.byMarcheName(searchDto.getMarcheName()))
                .and(PrefinancementSpecification.byMarcheNumero(searchDto.getMarcheNumero()))
                .and(PrefinancementSpecification.hasMontant(searchDto.getMontant()));

        log.debug("Spécification de recherche créée pour les remboursements en cours");

        Page<Prefinancement> prefinancementPage = prefinancementRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));
        log.debug("{} préfinancement(s) de remboursement trouvé(s) dans la page actuelle",
                prefinancementPage.getNumberOfElements());

        BigDecimal totalDesEncours = prefinancementRepository.findAll(spec).stream()
                .map(Prefinancement::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total des encours de remboursement calculé: {} pour la banque ID: {}", totalDesEncours, banqueId);

        Page<PrefinancementResponseDTO> pageDto = prefinancementPage.map(prefinancementMapper::toDto);
        log.debug("Conversion des entités en DTOs terminée");

        EncoursPrefinancementDto result = new EncoursPrefinancementDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        log.debug("DTO d'encours de remboursement créé avec succès");
        return result;
    }

    @Transactional(readOnly = true)
    public EncoursPrefinancementDto getDeblocageEncoursForBanque(EncoursPrefinancementSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Recherche des encours de déblocage pour la banque ID: {}", banqueId);
        log.debug("Critères de recherche - Nom marché: {}, Numéro marché: {}, Montant: {}",
                searchDto.getMarcheName(), searchDto.getMarcheNumero(), searchDto.getMontant());
        log.debug("Pagination - Page: {}, Taille: {}", pageable.getPageNumber(), pageable.getPageSize());

        Specification<Prefinancement> spec = PrefinancementSpecification.byBanqueId(banqueId)
                .and(PrefinancementSpecification.outstandingDeblocageUntil(endOfMonth))
                .and(PrefinancementSpecification.byMarcheName(searchDto.getMarcheName()))
                .and(PrefinancementSpecification.byMarcheNumero(searchDto.getMarcheNumero()))
                .and(PrefinancementSpecification.hasMontant(searchDto.getMontant()));

        log.debug("Spécification de recherche créée pour les déblocages en cours");

        Page<Prefinancement> prefinancementPage = prefinancementRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));
        log.debug("{} préfinancement(s) de déblocage trouvé(s) dans la page actuelle",
                prefinancementPage.getNumberOfElements());

        BigDecimal totalDesEncours = prefinancementRepository.findAll(spec).stream()
                .map(Prefinancement::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total des encours de déblocage calculé: {} pour la banque ID: {}", totalDesEncours, banqueId);

        Page<PrefinancementResponseDTO> pageDto = prefinancementPage.map(prefinancementMapper::toDto);
        log.debug("Conversion des entités en DTOs terminée");

        EncoursPrefinancementDto result = new EncoursPrefinancementDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        log.debug("DTO d'encours de déblocage créé avec succès");
        return result;
    }


    private NatureLigneFonctionnelle getPrefinancementNature() {

        log.debug("Recherche de la nature de ligne fonctionnelle Préfinancement");
        return natureLigneFonctionnelleRepository.findByLabel("Préfinancement")
                .orElseThrow(() -> {
                    log.error("Nature de ligne fonctionnelle Préfinancement non trouvée");
                    return new ResourceNotFoundException(
                            "Nature de ligne fonctionnelle Préfinancement non trouvée."
                    );
                });

    }

    /**
     * Validates if a Marche has an active "Préfinancement" line.
     * Searches in LigneFonctionnelleSpecifique (priority), then LigneFonctionnelle (fallback).
     *
     * @param marche The Marche to validate
     * @param prefiNature The "Préfinancement" nature
     * @throws Exception if no active line is found
     */
    private void validateActivePrefinancementLine(Marche marche, NatureLigneFonctionnelle prefiNature) throws Exception {
        log.debug("Validation de l'existence d'une ligne Préfinancement active pour le marché ID: {}", marche.getId());

        // ÉTAPE 1 : LigneFonctionnelleSpecifique (PRIORITAIRE)
        Optional<LigneFonctionnelleSpecifique> ligneSpecifique =
                ligneFonctionnelleSpecifiqueRepository.findByMarcheAndNatureLigneFonctionnelleAndActifTrue(
                        marche, prefiNature
                );
        if (ligneSpecifique.isPresent()) {
            log.info(" Ligne fonctionnelle spécifique Préfinancement active trouvée pour le marché '{}' (ID: {})", marche.getNom(), ligneSpecifique.get().getId());
            return;
        }

        log.debug("Aucune ligne spécifique trouvée, recherche dans les lignes générales...");


        // ÉTAPE 2 : LigneFonctionnelle liée au marché spécifique
        Optional<LigneFonctionnelle> ligneGeneraleMarcheSpecifique =
                ligneFonctionnelleRepository.findByMarcheIdAndNatureLigneFonctionnelleIdAndActifTrue(
                        marche.getId(),
                        prefiNature.getId()
                );

        if (ligneGeneraleMarcheSpecifique.isPresent()) {
            log.info(" Ligne fonctionnelle générale Préfinancement active trouvée pour le marché spécifique '{}' (ID: {})", marche.getNom(), ligneGeneraleMarcheSpecifique.get().getId());
            return;
        }

        log.debug("Aucune ligne générale spécifique au marché trouvée, recherche de ligne générale de la banque...");

        // ÉTAPE 3 : LigneFonctionnelle générale de la banque
        Optional<LigneFonctionnelle> ligneGeneraleBanque =
                ligneFonctionnelleRepository.findByBanqueIdAndNatureLigneFonctionnelleIdAndMarcheIsNullAndActifTrue(
                        marche.getBanque().getId(),
                        prefiNature.getId()
                );

        if (ligneGeneraleBanque.isPresent()) {
            log.info(" Ligne fonctionnelle générale Préfinancement active trouvée pour la banque '{}' (ID: {})", marche.getBanque().getNom(), ligneGeneraleBanque.get().getId());
            return;
        }

        // ÉTAPE 4 : Aucune ligne active trouvée
        log.error(" Aucune ligne fonctionnelle Préfinancement active trouvée pour le marché '{}'",marche.getNom());

        throw new Exception(
                "Impossible de créer un Préfinancement. Le marché '" + marche.getNom() +
                        "' n'est pas associé à une ligne fonctionnelle Préfinancement active. " +
                        "Veuillez créer une ligne spécifique ou une ligne générale au marché '" +
                        marche.getBanque().getNom() + "'."
        );
    }







}