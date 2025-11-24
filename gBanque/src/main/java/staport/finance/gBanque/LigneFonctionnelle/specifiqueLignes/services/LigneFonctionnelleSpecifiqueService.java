package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories.NatureLigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.*;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.mappers.LigneFonctionnelleSpecifiqueMapper;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.repositories.LigneFonctionnelleSpecifiqueRepository;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.specifications.LigneFonctionnelleSpecifiqueSpecification;
import staport.finance.gBanque.droitConstate.dtos.AgregatedSuivieDcLigneResponseDto;
import staport.finance.gBanque.droitConstate.dtos.SuivieDcLigneResponseDto;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.droitConstate.mapper.DroitConstateMapper;
import staport.finance.gBanque.droitConstate.repositories.DroitConstateRepository;
import staport.finance.gBanque.droitConstate.spec.DroitConstateSpec;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.factoring.dtos.AgregatedSuivieFactoringLigneResponseDto;
import staport.finance.gBanque.factoring.dtos.SuivieFactoringLineResponseDto;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.factoring.mapper.FactoringMapper;
import staport.finance.gBanque.factoring.repository.FactoringRepository;
import staport.finance.gBanque.factoring.spec.FactoringSpec;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.repositories.MarcheRepository;
import staport.finance.gBanque.prefinancement.dtos.AgregatedSuiviePrefiLigneResponseDto;
import staport.finance.gBanque.prefinancement.dtos.SuiviePrefiLigneResponseDto;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;
import staport.finance.gBanque.prefinancement.mapper.PrefinancementMapper;
import staport.finance.gBanque.prefinancement.repository.PrefinancementRepository;
import staport.finance.gBanque.prefinancement.spec.PrefinancementSpecification;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class LigneFonctionnelleSpecifiqueService {

    private static final Logger log = LoggerFactory.getLogger(LigneFonctionnelleSpecifiqueService.class);

    private final LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository;
    private final LigneFonctionnelleSpecifiqueMapper ligneFonctionnelleSpecifiqueMapper;
    private final BanqueRepository banqueRepository;
    private final NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository;
    private final MarcheRepository marcheRepository;

    private final PrefinancementRepository prefinancementRepository;
    private final PrefinancementMapper prefinancementMapper;

    private final DroitConstateRepository droitConstateRepository;
    private final DroitConstateMapper droitConstateMapper;

    private final FactoringRepository factoringRepository;
    private final FactoringMapper factoringMapper;


    public LigneFonctionnelleSpecifiqueService(
            LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository,
            LigneFonctionnelleSpecifiqueMapper ligneFonctionnelleSpecifiqueMapper,
            BanqueRepository banqueRepository,
            NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository,
            MarcheRepository marcheRepository, PrefinancementRepository prefinancementRepository, PrefinancementMapper prefinancementMapper, DroitConstateRepository droitConstateRepository, DroitConstateMapper droitConstateMapper, FactoringRepository factoringRepository, FactoringMapper factoringMapper) {
        this.ligneFonctionnelleSpecifiqueRepository = ligneFonctionnelleSpecifiqueRepository;
        this.ligneFonctionnelleSpecifiqueMapper = ligneFonctionnelleSpecifiqueMapper;
        this.banqueRepository = banqueRepository;
        this.natureLigneFonctionnelleRepository = natureLigneFonctionnelleRepository;
        this.marcheRepository = marcheRepository;
        this.prefinancementRepository = prefinancementRepository;
        this.prefinancementMapper = prefinancementMapper;
        this.droitConstateRepository = droitConstateRepository;
        this.droitConstateMapper = droitConstateMapper;
        this.factoringRepository = factoringRepository;
        this.factoringMapper = factoringMapper;
    }
    @Transactional
    public LigneFonctionnelleSpecifiqueResponse createLigneFonctionnelleSpecifique(LigneFonctionnelleSpecifiqueRequest request) {
        log.debug("[CREATE] Début de la création de ligne fonctionnelle spécifique avec la requête: {}", request);

        // Validation des dates
        validateDateRange(request.getDateDepart(), request.getDateFin());
        log.debug("[CREATE] Dates validées: dateDepart={}, dateFin={}", request.getDateDepart(), request.getDateFin());

        // Récupération de la NatureLigneFonctionnelle
        NatureLigneFonctionnelle nature = natureLigneFonctionnelleRepository.findById(request.getNatureLigneFonctionnelleId())
                .orElseThrow(() -> {
                    log.error("[CREATE] NatureLigneFonctionnelle non trouvée pour ID: {}", request.getNatureLigneFonctionnelleId());
                    return new ResourceNotFoundException("NatureLigneFonctionnelle non trouvée avec l'ID: " + request.getNatureLigneFonctionnelleId());
                });
        log.debug("[CREATE] NatureLigneFonctionnelle trouvée: {}", nature.getLabel());

        // Récupération du Marché
        Marche marche = marcheRepository.findById(request.getMarcheId())
                .orElseThrow(() -> {
                    log.error("[CREATE] Marché non trouvé pour ID: {}", request.getMarcheId());
                    return new ResourceNotFoundException("Marché non trouvé avec l'ID: " + request.getMarcheId());
                });
        log.debug("[CREATE] Marché trouvé: {}", marche.getNom());

        // Vérification doublon actif
        boolean isActif = request.getActif() == null || request.getActif();
        if (isActif) {
            ligneFonctionnelleSpecifiqueRepository.findByMarcheAndNatureLigneFonctionnelleAndActifTrue(marche, nature)
                    .ifPresent(existing -> {
                        log.warn("[CREATE] Doublon actif détecté pour Marche '{}' et Nature '{}'", marche.getNom(), nature.getLabel());
                        throw new DuplicateEntryException(
                                "Une ligne fonctionnelle ACTIVE de type '" + nature.getLabel() + "' existe déjà pour le marché '" + marche.getNom() + "'."
                        );
                    });
            log.debug("[CREATE] Aucun doublon actif trouvé, création possible");
        }

        // Mapping DTO -> Entity
        LigneFonctionnelleSpecifique entity = ligneFonctionnelleSpecifiqueMapper.toEntity(request, nature, marche);
        LigneFonctionnelleSpecifique savedEntity = ligneFonctionnelleSpecifiqueRepository.save(entity);
        log.info("[CREATE] Ligne fonctionnelle spécifique créée avec succès, ID: {}", savedEntity.getId());

        return ligneFonctionnelleSpecifiqueMapper.toDto(savedEntity);
    }

    @Transactional(readOnly = true)
    public LigneFonctionnelleSpecifiqueResponse getLigneFonctionnelleSpecifiqueById(Long id) {
        log.debug("[GET BY ID] Début de la récupération de ligne fonctionnelle spécifique pour ID: {}", id);

        LigneFonctionnelleSpecifique entity = ligneFonctionnelleSpecifiqueRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("[GET BY ID] Ligne fonctionnelle spécifique non trouvée pour ID: {}", id);
                    return new ResourceNotFoundException("Ligne Fonctionnelle Spécifique non trouvée avec l'ID: " + id);
                });

        log.debug("[GET BY ID] Ligne fonctionnelle spécifique trouvée: ID={}, Nature={}, Marché={}",
                entity.getId(),
                entity.getNatureLigneFonctionnelle().getLabel(),
                entity.getMarche().getNom());

        return ligneFonctionnelleSpecifiqueMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public Page<LigneFonctionnelleSpecifiqueResponse> getLignesFonctionnellesSpecifiques(
            LigneFonctionnelleSpecifiqueSearchCriteria criteria, Pageable pageable) {
        log.debug("[GET ALL] Récupération des lignes fonctionnelles spécifiques avec critères: {} et Pageable: {}", criteria, pageable);

        Specification<LigneFonctionnelleSpecifique> spec = LigneFonctionnelleSpecifiqueSpecification.searchByCriteria(criteria);
        Page<LigneFonctionnelleSpecifique> page = ligneFonctionnelleSpecifiqueRepository.findAll(spec, pageable);

        log.info("[GET ALL] {} lignes fonctionnelles spécifiques trouvées (Page {}/{})",
                page.getTotalElements(), page.getNumber() + 1, page.getTotalPages());


        return page.map(ligneFonctionnelleSpecifiqueMapper::toDto);
    }
    @Transactional(readOnly = true)
    public Page<LigneFonctionnelleSpecifiqueResponse> getLignesFonctionnellesSpecifiquesByBanqueId(
            Long banqueId, LigneFonctionnelleSpecifiqueSearchCriteria criteria, Pageable pageable) {

        log.debug("[GET BY BANQUE] Début de récupération paginée pour Banque ID: {} avec critères: {} et pageable: {}", banqueId, criteria, pageable);

        // 1. Validate that the bank exists
        if (!banqueRepository.existsById(banqueId)) {
            log.warn("[GET BY BANQUE] Banque non trouvée avec l'ID: {}", banqueId);
            throw new ResourceNotFoundException("Banque non trouvée avec l'ID: " + banqueId + ". Impossible de récupérer les lignes fonctionnelles spécifiques.");
        }
        log.debug("[GET BY BANQUE] Banque ID {} existante confirmée", banqueId);

        // 2. Set the banqueId in the criteria
        criteria.setBanqueId(banqueId);
        log.debug("[GET BY BANQUE] Critères mis à jour avec banqueId: {}", criteria);

        // 3. Use the existing Specification to find the entities
        Specification<LigneFonctionnelleSpecifique> spec = LigneFonctionnelleSpecifiqueSpecification.searchByCriteria(criteria);
        Page<LigneFonctionnelleSpecifique> page = ligneFonctionnelleSpecifiqueRepository.findAll(spec, pageable);

        log.info("[GET BY BANQUE] {} lignes fonctionnelles spécifiques trouvées pour la banque ID {} (Page {}/{})",
                page.getTotalElements(), banqueId, page.getNumber() + 1, page.getTotalPages());

        page.forEach(entity ->
                log.debug("[GET BY BANQUE] Ligne ID={}, Nature={}, Marché={}, Actif={}",
                        entity.getId(),
                        entity.getNatureLigneFonctionnelle().getLabel(),
                        entity.getMarche().getNom(),
                        entity.isActif())
        );

        // 4. Map entities to DTOs and return
        return page.map(ligneFonctionnelleSpecifiqueMapper::toDto);
    }

    @Transactional
    public LigneFonctionnelleSpecifiqueResponse updateLigneFonctionnelleSpecifique(Long id, LigneFonctionnelleSpecifiqueRequest request) {
        log.debug("[UPDATE] Début de mise à jour de LigneFonctionnelleSpécifique ID: {} avec request: {}", id, request);

        LigneFonctionnelleSpecifique existingEntity = ligneFonctionnelleSpecifiqueRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("[UPDATE] Ligne Fonctionnelle Spécifique non trouvée avec ID: {}", id);
                    return new ResourceNotFoundException("Ligne Fonctionnelle Spécifique non trouvée avec l'ID: " + id);
                });
        log.debug("[UPDATE] Entité existante récupérée: ID={}, Nature={}, Marché={}, Actif={}",
                existingEntity.getId(),
                existingEntity.getNatureLigneFonctionnelle().getLabel(),
                existingEntity.getMarche().getNom(),
                existingEntity.isActif());

        // Validation des dates
        OffsetDateTime effectiveDateDepart = request.getDateDepart() != null ? request.getDateDepart() : existingEntity.getDateDepart();
        OffsetDateTime effectiveDateFin = request.getDateFin() != null ? request.getDateFin() : existingEntity.getDateFin();
        validateDateRange(effectiveDateDepart, effectiveDateFin);
        log.debug("[UPDATE] Validation des dates effective: dateDepart={}, dateFin={}", effectiveDateDepart, effectiveDateFin);

        // Mise à jour des relations
        NatureLigneFonctionnelle targetNature = (request.getNatureLigneFonctionnelleId() != null) ?
                natureLigneFonctionnelleRepository.findById(request.getNatureLigneFonctionnelleId())
                        .orElseThrow(() -> {
                            log.error("[UPDATE] NatureLigneFonctionnelle non trouvée pour ID: {}", request.getNatureLigneFonctionnelleId());
                            return new ResourceNotFoundException("NatureLigneFonctionnelle non trouvée avec l'ID: " + request.getNatureLigneFonctionnelleId());
                        })
                : existingEntity.getNatureLigneFonctionnelle();

        Marche targetMarche = (request.getMarcheId() != null) ?
                marcheRepository.findById(request.getMarcheId())
                        .orElseThrow(() -> {
                            log.error("[UPDATE] Marché non trouvé pour ID: {}", request.getMarcheId());
                            return new ResourceNotFoundException("Marché non trouvé avec l'ID: " + request.getMarcheId());
                        })
                : existingEntity.getMarche();

        log.debug("[UPDATE] Relations mises à jour: Nature={}, Marché={}", targetNature.getLabel(), targetMarche.getNom());

        // Vérification des doublons actifs
        boolean requestedActif = request.getActif() != null ? request.getActif() : existingEntity.isActif();
        if (requestedActif) {
            ligneFonctionnelleSpecifiqueRepository.findByMarcheAndNatureLigneFonctionnelleAndActifTrue(targetMarche, targetNature)
                    .ifPresent(existingActiveDuplicate -> {
                        if (!existingActiveDuplicate.getId().equals(id)) {
                            log.warn("[UPDATE] Doublon actif détecté pour Marche '{}' et Nature '{}', conflit avec ID: {}",
                                    targetMarche.getNom(), targetNature.getLabel(), existingActiveDuplicate.getId());
                            throw new DuplicateEntryException("Impossible de mettre à jour. Une ligne fonctionnelle ACTIVE de ce type existe déjà pour ce marché.");
                        }
                    });
            log.debug("[UPDATE] Aucun doublon actif détecté ou doublon existant correspond à la même entité");
        }

        // Mise à jour partielle des champs
        if (request.getPlafond() != null) existingEntity.setPlafond(request.getPlafond());
        if (request.getTaux() != null) existingEntity.setTaux(request.getTaux());
        if (request.getDateDepart() != null) existingEntity.setDateDepart(request.getDateDepart());
        if (request.getDateFin() != null) existingEntity.setDateFin(request.getDateFin());
        if (request.getActif() != null) existingEntity.setActif(request.getActif());

        // Mise à jour des relations
        existingEntity.setNatureLigneFonctionnelle(targetNature);
        existingEntity.setMarche(targetMarche);

        // Sauvegarde
        LigneFonctionnelleSpecifique updatedEntity = ligneFonctionnelleSpecifiqueRepository.save(existingEntity);
        log.info("[UPDATE] Ligne fonctionnelle spécifique ID {} mise à jour avec succès", updatedEntity.getId());

        return ligneFonctionnelleSpecifiqueMapper.toDto(updatedEntity);
    }

    @Transactional
    public void deactivateLigneFonctionnelleById(Long ligneId) throws Exception {
        log.debug("[DEACTIVATE] Tentative de désactivation de la ligne fonctionnelle spécifique ID: {}", ligneId);

        LigneFonctionnelleSpecifique ligneToDeactivate = ligneFonctionnelleSpecifiqueRepository.findById(ligneId)
                .orElseThrow(() -> {
                    log.error("[DEACTIVATE] Ligne fonctionnelle spécifique non trouvée pour ID: {}", ligneId);
                    return new ResourceNotFoundException("Aucune Ligne Fonctionnelle spécifique avec l'ID: " + ligneId);
                });
        if(!ligneToDeactivate.isActif()) {
            log.error("[DEACTIVATE] Ligne fonctionnelle spécifique est déja désactivée");
            throw new Exception("[DEACTIVATE] Ligne fonctionnelle spécifique est déja désactivée");
        }
        ligneToDeactivate.setActif(false);
        ligneFonctionnelleSpecifiqueRepository.save(ligneToDeactivate);
        log.info("[DEACTIVATE] Ligne fonctionnelle spécifique ID {} désactivée avec succès", ligneToDeactivate.getId());
    }

    @Transactional
    public void deleteLigneFonctionnelleSpecifique(Long id) {
        log.debug("[DELETE] Requête de suppression de ligne fonctionnelle spécifique ID: {}", id);

        if (!ligneFonctionnelleSpecifiqueRepository.existsById(id)) {
            log.warn("[DELETE] Ligne fonctionnelle spécifique non trouvée pour ID: {}", id);
            throw new ResourceNotFoundException("Ligne Fonctionnelle Spécifique non trouvée avec l'ID: " + id);
        }

        ligneFonctionnelleSpecifiqueRepository.deleteById(id);
        log.info("[DELETE] Ligne fonctionnelle spécifique ID {} supprimée avec succès", id);
    }

    private void validateDateRange(OffsetDateTime dateDepart, OffsetDateTime dateFin) {
        if (dateDepart != null && dateFin != null && dateFin.isBefore(dateDepart)) {
            log.warn("[VALIDATE DATE] dateFin ({}) est antérieure à dateDepart ({})", dateFin, dateDepart);
            throw new IllegalArgumentException("La date de fin (" + dateFin + ") ne peut pas être antérieure à la date de départ (" + dateDepart + ").");
        }
        log.debug("[VALIDATE DATE] Plage de dates valide: dateDepart={}, dateFin={}", dateDepart, dateFin);
    }

    @Transactional(readOnly = true)
    public Page<SpecificLigneResposneDto> getPrefiLines(SpecifiqueLineSearchDto searchCriteria, Pageable pageable) {
        log.debug("[PREFI] Début récupération paginée des lignes de préfinancement avec critères: {}", searchCriteria);

        NatureLigneFonctionnelle prefiNature = natureLigneFonctionnelleRepository.findByLabel("Préfinancement")
                .orElseThrow(() -> {
                    log.error("[PREFI] Aucune nature de ligne fonctionnelle trouvée avec le label : Préfinancement");
                    return new ResourceNotFoundException("Aucune nature de ligne fonctionnelle trouvée avec le label : Préfinancement");
                });

        Specification<LigneFonctionnelleSpecifique> natureSpec = (root, query, cb) -> cb.equal(root.get("natureLigneFonctionnelle"), prefiNature);
        Specification<LigneFonctionnelleSpecifique> searchSpec = LigneFonctionnelleSpecifiqueSpecification.searchForPrefi(searchCriteria);
        Specification<LigneFonctionnelleSpecifique> finalSpec = Specification.where(natureSpec).and(searchSpec);

        Page<LigneFonctionnelleSpecifique> page = ligneFonctionnelleSpecifiqueRepository.findAll(finalSpec, pageable);

        log.info("[PREFI] {} lignes de préfinancement trouvées. PageSize={}, TotalElements={}",
                page.getNumberOfElements(), page.getSize(), page.getTotalElements());



        return page.map(ligneFonctionnelleSpecifiqueMapper::toPrefinancementResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<SpecificLigneResposneDto> getDCLines(SpecifiqueLineSearchDto searchCriteria, Pageable pageable) {
        log.debug("[DC] Début récupération paginée des lignes de droit constaté avec critères: {}", searchCriteria);

        NatureLigneFonctionnelle dcLine = natureLigneFonctionnelleRepository.findByLabel("Droit constaté")
                .orElseThrow(() -> {
                    log.error("[DC] Aucune nature de ligne fonctionnelle trouvée avec le label : Droit constaté");
                    return new ResourceNotFoundException("Aucune nature de ligne fonctionnelle trouvée avec le label : Droit constaté");
                });

        Specification<LigneFonctionnelleSpecifique> natureSpec = (root, query, cb) -> cb.equal(root.get("natureLigneFonctionnelle"), dcLine);
        Specification<LigneFonctionnelleSpecifique> searchSpec = LigneFonctionnelleSpecifiqueSpecification.searchForPrefi(searchCriteria);
        Specification<LigneFonctionnelleSpecifique> finalSpec = Specification.where(natureSpec).and(searchSpec);

        Page<LigneFonctionnelleSpecifique> page = ligneFonctionnelleSpecifiqueRepository.findAll(finalSpec, pageable);

        log.info("[DC] {} lignes de droit constaté trouvées. PageSize={}, TotalElements={}",
                page.getNumberOfElements(), page.getSize(), page.getTotalElements());

        page.forEach(entity ->
                log.debug("[DC] Ligne ID={}, Marche={}, Actif={}",
                        entity.getId(), entity.getMarche().getNom(), entity.isActif())
        );

        return page.map(ligneFonctionnelleSpecifiqueMapper::toDCResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<SpecificLigneResposneDto> getFactoringLines(SpecifiqueLineSearchDto searchCriteria, Pageable pageable) {
        log.debug("[FACTORING] Début récupération paginée des lignes de factoring avec critères: {}", searchCriteria);

        NatureLigneFonctionnelle factoringLine = natureLigneFonctionnelleRepository.findByLabel("Factoring")
                .orElseThrow(() -> {
                    log.error("[FACTORING] Aucune nature de ligne fonctionnelle trouvée avec le label : Factoring");
                    return new ResourceNotFoundException("Aucune nature de ligne fonctionnelle trouvée avec le label : Factoring");
                });

        Specification<LigneFonctionnelleSpecifique> natureSpec = (root, query, cb) -> cb.equal(root.get("natureLigneFonctionnelle"), factoringLine);
        Specification<LigneFonctionnelleSpecifique> searchSpec = LigneFonctionnelleSpecifiqueSpecification.searchForPrefi(searchCriteria);
        Specification<LigneFonctionnelleSpecifique> finalSpec = Specification.where(natureSpec).and(searchSpec);

        Page<LigneFonctionnelleSpecifique> page = ligneFonctionnelleSpecifiqueRepository.findAll(finalSpec, pageable);

        log.info("[FACTORING] {} lignes de factoring trouvées. PageSize={}, TotalElements={}",
                page.getNumberOfElements(), page.getSize(), page.getTotalElements());

        page.forEach(entity ->
                log.debug("[FACTORING] Ligne ID={}, Marche={}, Actif={}",
                        entity.getId(), entity.getMarche().getNom(), entity.isActif())
        );

        return page.map(ligneFonctionnelleSpecifiqueMapper::toFactoringResponseDto);
    }

    @Transactional(readOnly = true)
    public AgregatedSuiviePrefiLigneResponseDto getSuiviePrefiLigneResponse(Long lineId, Pageable pageable) {
        log.debug("[PREFI SUIVIE] Début récupération agrégée pour ligne ID: {}", lineId);

        LigneFonctionnelleSpecifique specificLine = ligneFonctionnelleSpecifiqueRepository.findById(lineId)
                .orElseThrow(() -> {
                    log.error("[PREFI SUIVIE] Ligne non trouvée pour ID: {}", lineId);
                    return new ResourceNotFoundException("Ligne Fonctionnelle Spécifique non trouvée avec l'ID: " + lineId);
                });

        Marche marche = specificLine.getMarche();
        if (marche == null) {
            log.warn("[PREFI SUIVIE] Ligne ID {} non liée à aucun marché", lineId);
            throw new ResourceNotFoundException("Aucun marché associé à la ligne fonctionnelle avec l'ID: " + lineId);
        }

        List<Prefinancement> allPaidPrefi = prefinancementRepository.findByMarcheAndStatus(marche, Status.PAYE);
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (Prefinancement prefi : allPaidPrefi) {
            if (prefi.getType() == Type.REMBOURSEMENT) totalDebit = totalDebit.add(prefi.getMontant());
            else if (prefi.getType() == Type.DEBLOCAGE) totalCredit = totalCredit.add(prefi.getMontant());
        }
        BigDecimal reste = totalCredit.subtract(totalDebit);
        log.debug("[PREFI SUIVIE] Totaux calculés: totalDebit={}, totalCredit={}, reste={}", totalDebit, totalCredit, reste);

        Specification<Prefinancement> spec = Specification.where(PrefinancementSpecification.byMarcheId(marche.getId()))
                .and(PrefinancementSpecification.byStatus(Status.PAYE));

        Page<Prefinancement> prefiPage = prefinancementRepository.findAll(spec, pageable);
        log.info("[PREFI SUIVIE] {} enregistrements préfinancement payés récupérés", prefiPage.getNumberOfElements());

        Page<SuiviePrefiLigneResponseDto> pageOfDtos = prefiPage.map(prefinancementMapper::toSuiviePrefiLigneResponseDto);

        AgregatedSuiviePrefiLigneResponseDto aggregatedDto = new AgregatedSuiviePrefiLigneResponseDto();
        aggregatedDto.setBanqueName(specificLine.getMarche().getBanque().getNom());
        aggregatedDto.setMarcheName(marche.getNom());
        aggregatedDto.setMarcheNumero(marche.getNumero());
        aggregatedDto.setTotalDebit(totalDebit);
        aggregatedDto.setTotalCredit(totalCredit);
        aggregatedDto.setReste(reste);
        aggregatedDto.setPage(pageOfDtos);

        log.info("[PREFI SUIVIE] Agrégation complétée pour ligne ID: {}", lineId);
        return aggregatedDto;
    }

    @Transactional(readOnly = true)
    public AgregatedSuivieDcLigneResponseDto getSuivieDcLigneResponse(Long lineId, Pageable pageable) {
        log.debug("[DC SUIVIE] Début récupération agrégée pour ligne ID: {}", lineId);

        LigneFonctionnelleSpecifique specificLine = ligneFonctionnelleSpecifiqueRepository.findById(lineId)
                .orElseThrow(() -> {
                    log.error("[DC SUIVIE] Ligne non trouvée pour ID: {}", lineId);
                    return new ResourceNotFoundException("Ligne Fonctionnelle Spécifique non trouvée avec l'ID: " + lineId);
                });

        Marche marche = specificLine.getMarche();
        if (marche == null) {
            log.warn("[DC SUIVIE] Ligne ID {} non liée à aucun marché", lineId);
            throw new ResourceNotFoundException("Aucun marché associé à la ligne fonctionnelle avec l'ID: " + lineId);
        }

        List<DroitConstate> allPaidDc = droitConstateRepository.findByMarcheAndStatus(marche, Status.PAYE);
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (DroitConstate dc : allPaidDc) {
            if (dc.getType() == Type.REMBOURSEMENT) totalDebit = totalDebit.add(dc.getMontant());
            else if (dc.getType() == Type.DEBLOCAGE) totalCredit = totalCredit.add(dc.getMontant());
        }
        BigDecimal reste = totalCredit.subtract(totalDebit);
        log.debug("[DC SUIVIE] Totaux calculés: totalDebit={}, totalCredit={}, reste={}", totalDebit, totalCredit, reste);

        Specification<DroitConstate> spec = Specification.where(DroitConstateSpec.byMarcheId(marche.getId()))
                .and(DroitConstateSpec.byStatus(Status.PAYE));
        Page<DroitConstate> dcPage = droitConstateRepository.findAll(spec, pageable);
        log.info("[DC SUIVIE] {} enregistrements DC payés récupérés", dcPage.getNumberOfElements());

        Page<SuivieDcLigneResponseDto> pageOfDtos = dcPage.map(droitConstateMapper::toSuivieDcLigneResponseDto);

        AgregatedSuivieDcLigneResponseDto aggregatedDto = new AgregatedSuivieDcLigneResponseDto();
        aggregatedDto.setBanqueName(specificLine.getMarche().getBanque().getNom());
        aggregatedDto.setMarcheName(marche.getNom());
        aggregatedDto.setMarcheNumero(marche.getNumero());
        aggregatedDto.setTotalDebit(totalDebit);
        aggregatedDto.setTotalCredit(totalCredit);
        aggregatedDto.setReste(reste);
        aggregatedDto.setPage(pageOfDtos);

        log.info("[DC SUIVIE] Agrégation complétée pour ligne ID: {}", lineId);
        return aggregatedDto;
    }

    @Transactional(readOnly = true)
    public AgregatedSuivieFactoringLigneResponseDto getSuivieFactoringLigneResponse(Long lineId, Pageable pageable) {
        log.debug("[FACTORING SUIVIE] Début récupération agrégée pour ligne ID: {}", lineId);

        LigneFonctionnelleSpecifique specificLine = ligneFonctionnelleSpecifiqueRepository.findById(lineId)
                .orElseThrow(() -> {
                    log.error("[FACTORING SUIVIE] Ligne non trouvée pour ID: {}", lineId);
                    return new ResourceNotFoundException("Ligne Fonctionnelle Spécifique non trouvée avec l'ID: " + lineId);
                });

        Marche marche = specificLine.getMarche();
        if (marche == null) {
            log.warn("[FACTORING SUIVIE] Ligne ID {} non liée à aucun marché", lineId);
            throw new ResourceNotFoundException("Aucun marché associé à la ligne fonctionnelle avec l'ID: " + lineId);
        }

        List<Factoring> allPaidFactoring = factoringRepository.findByMarcheAndStatus(marche, Status.PAYE);
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (Factoring f : allPaidFactoring) {
            if (f.getType() == Type.REMBOURSEMENT) totalDebit = totalDebit.add(f.getMontant());
            else if (f.getType() == Type.DEBLOCAGE) totalCredit = totalCredit.add(f.getMontant());
        }
        BigDecimal reste = totalCredit.subtract(totalDebit);
        log.debug("[FACTORING SUIVIE] Totaux calculés: totalDebit={}, totalCredit={}, reste={}", totalDebit, totalCredit, reste);

        Specification<Factoring> spec = Specification.where(FactoringSpec.byMarcheId(marche.getId()))
                .and(FactoringSpec.byStatus(Status.PAYE));

        Page<Factoring> factoringPage = factoringRepository.findAll(spec, pageable);
        log.info("[FACTORING SUIVIE] {} enregistrements factoring payés récupérés", factoringPage.getNumberOfElements());

        Page<SuivieFactoringLineResponseDto> pageOfDtos = factoringPage.map(factoringMapper::toSuivieFactoringLigneResponseDto);

        AgregatedSuivieFactoringLigneResponseDto aggregatedDto = new AgregatedSuivieFactoringLigneResponseDto();
        aggregatedDto.setBanqueName(specificLine.getMarche().getBanque().getNom());
        aggregatedDto.setMarcheName(marche.getNom());
        aggregatedDto.setMarcheNumero(marche.getNumero());
        aggregatedDto.setTotalDebit(totalDebit);
        aggregatedDto.setTotalCredit(totalCredit);
        aggregatedDto.setReste(reste);
        aggregatedDto.setPage(pageOfDtos);

        log.info("[FACTORING SUIVIE] Agrégation complétée pour ligne ID: {}", lineId);
        log.info("result after agrégation : {} ",aggregatedDto.toString());
        return aggregatedDto;
    }

    }
