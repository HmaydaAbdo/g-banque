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
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.services.LigneFonctionnelleReader;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.services.LigneFonctionnelleSpecifiqueReader;
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
    private final LigneFonctionnelleSpecifiqueReader ligneFonctionnelleSpecifiqueReader;
    private final NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository;
    private final LigneFonctionnelleReader ligneFonctionnelleReader;
    LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

    public PrefinancementService(
            PrefinancementRepository prefinancementRepository,
            MarcheRepository marcheRepository,
            PrefinancementMapper prefinancementMapper,
            LigneFonctionnelleSpecifiqueReader ligneFonctionnelleSpecifiqueReader,
            NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository, LigneFonctionnelleReader ligneFonctionnelleReader) {
        this.prefinancementRepository = prefinancementRepository;
        this.marcheRepository = marcheRepository;
        this.prefinancementMapper = prefinancementMapper;
        this.ligneFonctionnelleSpecifiqueReader = ligneFonctionnelleSpecifiqueReader;
        this.natureLigneFonctionnelleRepository = natureLigneFonctionnelleRepository;
        this.ligneFonctionnelleReader = ligneFonctionnelleReader;
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
        LigneResult ligneResult = findActivePrefinancementLine(marche, prefiNature);

        Prefinancement prefinancement = prefinancementMapper.toEntity(requestDTO, marche);
        prefinancement.setLigneFonctionnelleSpecifique(ligneResult.specifique());
        prefinancement.setLigneFonctionnelle(ligneResult.generale());
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
            LigneResult ligneResult = findActivePrefinancementLine(marche, prefiNature);
            existingPrefinancement.setLigneFonctionnelleSpecifique(ligneResult.specifique());
            existingPrefinancement.setLigneFonctionnelle(ligneResult.generale());
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
        log.debug("Recherche de la nature de ligne fonctionnelle Préfinancement (code=PREFINANCEMENT)");
        return natureLigneFonctionnelleRepository.findByCode("PREFINANCEMENT")
                .orElseThrow(() -> {
                    log.error("Nature de ligne fonctionnelle 'PREFINANCEMENT' introuvable en base");
                    return new ResourceNotFoundException(
                            "Nature de ligne fonctionnelle PREFINANCEMENT introuvable. " +
                            "Vérifiez la table nature_ligne_fonctionnelle."
                    );
                });
    }

    /**
     * Recherche et retourne la ligne fonctionnelle Préfinancement active pour un marché.
     * Priorité : spécifique > générale liée au marché.
     * Lève une exception si aucune ligne n'est trouvée.
     */
    private LigneResult findActivePrefinancementLine(Marche marche, NatureLigneFonctionnelle prefiNature) throws Exception {
        log.debug("Recherche d'une ligne Préfinancement active pour le marché ID: {}", marche.getId());

        // ÉTAPE 1 : LigneFonctionnelleSpecifique (PRIORITAIRE)
        Optional<LigneFonctionnelleSpecifique> ligneSpecifique =
                ligneFonctionnelleSpecifiqueReader.findByMarcheAndNatureLigneFonctionnelleAndActifTrue(marche, prefiNature);
        if (ligneSpecifique.isPresent()) {
            log.info("Ligne spécifique Préfinancement active trouvée pour le marché '{}' (ID: {})",
                    marche.getNom(), ligneSpecifique.get().getId());
            return new LigneResult(ligneSpecifique.get(), null);
        }

        // ÉTAPE 2 : LigneFonctionnelle générale liée au marché
        Optional<LigneFonctionnelle> ligneGenerale =
                ligneFonctionnelleReader.findActiveLineForMarche(marche.getId(), prefiNature.getId());
        if (ligneGenerale.isPresent()) {
            log.info("Ligne générale Préfinancement active trouvée pour le marché '{}' (ID: {})",
                    marche.getNom(), ligneGenerale.get().getId());
            return new LigneResult(null, ligneGenerale.get());
        }

        log.error("Aucune ligne Préfinancement active pour le marché '{}'", marche.getNom());
        throw new Exception(
                "Impossible de créer un Préfinancement. Le marché '" + marche.getNom() +
                "' n'est associé à aucune ligne fonctionnelle Préfinancement active " +
                "(ni spécifique, ni générale liée à ce marché)."
        );
    }

    /** Résultat de la recherche de ligne : exactement l'une des deux est non-null. */
    private record LigneResult(LigneFonctionnelleSpecifique specifique, LigneFonctionnelle generale) {}







}