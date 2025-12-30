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
import staport.finance.gBanque.LigneFonctionnelle.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.repositories.LigneFonctionnelleRepository;
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
import java.util.Optional;

@Service
public class LigneFonctionnelleSpecifiqueService {

    private static final Logger log = LoggerFactory.getLogger(LigneFonctionnelleSpecifiqueService.class);

    private final LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository;
    private final LigneFonctionnelleRepository ligneFonctionnelleRepository;
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
            LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository, LigneFonctionnelleRepository ligneFonctionnelleRepository,
            LigneFonctionnelleSpecifiqueMapper ligneFonctionnelleSpecifiqueMapper,
            BanqueRepository banqueRepository,
            NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository,
            MarcheRepository marcheRepository, PrefinancementRepository prefinancementRepository, PrefinancementMapper prefinancementMapper, DroitConstateRepository droitConstateRepository, DroitConstateMapper droitConstateMapper, FactoringRepository factoringRepository, FactoringMapper factoringMapper) {
        this.ligneFonctionnelleSpecifiqueRepository = ligneFonctionnelleSpecifiqueRepository;
        this.ligneFonctionnelleRepository = ligneFonctionnelleRepository;
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
        log.info("Début de création d'une ligne fonctionnelle spécifique");
        log.debug("Détails de la requête: {}", request);

        // Validation des dates
        validateDateRange(request.getDateDepart(), request.getDateFin());

        // Récupération de la NatureLigneFonctionnelle
        log.debug("Recherche de la NatureLigneFonctionnelle avec ID: {}", request.getNatureLigneFonctionnelleId());
        NatureLigneFonctionnelle nature = natureLigneFonctionnelleRepository.findById(request.getNatureLigneFonctionnelleId())
                .orElseThrow(() -> {
                    log.error("NatureLigneFonctionnelle non trouvée avec l'ID: {}", request.getNatureLigneFonctionnelleId());
                    return new ResourceNotFoundException("NatureLigneFonctionnelle non trouvée avec l'ID: " + request.getNatureLigneFonctionnelleId());
                });

        log.debug("NatureLigneFonctionnelle trouvée: {}", nature.getLabel());

        // Récupération du Marché
        log.debug("Recherche du Marché avec ID: {}", request.getMarcheId());
        Marche marche = marcheRepository.findById(request.getMarcheId())
                .orElseThrow(() -> {
                    log.error("Marché non trouvé avec l'ID: {}", request.getMarcheId());
                    return new ResourceNotFoundException("Marché non trouvé avec l'ID: " + request.getMarcheId());
                });
        log.debug("Marché trouvé: {} (Numéro: {})", marche.getNom(), marche.getNumero());

        // Vérification doublon actif
        boolean isActif = request.getActif() == null || request.getActif();
        if (isActif) {

            log.debug("Vérification des conflits avec les lignes générales");
            checkNoConflictWithGeneralLine(marche, nature);
            log.debug("Vérification des doublons actifs pour Marché '{}' et Nature '{}'", marche.getNom(), nature.getLabel());
            ligneFonctionnelleSpecifiqueRepository.findByMarcheAndNatureLigneFonctionnelleAndActifTrue(marche, nature)
                    .ifPresent(existing -> {
                        log.error("Doublon actif détecté - ID existant: {}", existing.getId());
                        throw new DuplicateEntryException(
                                "Une ligne fonctionnelle ACTIVE de type '" + nature.getLabel() + "' existe déjà pour le marché '" + marche.getNom() + "'."
                        );
                    });
            log.debug("Aucun doublon actif trouvé");
        }

        // Mapping DTO -> Entity
        log.debug("Mapping de la requête vers l'entité");
        LigneFonctionnelleSpecifique entity = ligneFonctionnelleSpecifiqueMapper.toEntity(request, nature, marche);

        log.debug("Sauvegarde de l'entité en base de données");
        LigneFonctionnelleSpecifique savedEntity = ligneFonctionnelleSpecifiqueRepository.save(entity);

        log.info("Ligne fonctionnelle spécifique créée avec succès - ID: {}, Nature: {}, Marché: {}",
                savedEntity.getId(), nature.getLabel(), marche.getNom());

        return ligneFonctionnelleSpecifiqueMapper.toDto(savedEntity);
    }

    @Transactional(readOnly = true)
    public LigneFonctionnelleSpecifiqueResponse getLigneFonctionnelleSpecifiqueById(Long id) {
        log.info("Récupération de la ligne fonctionnelle spécifique avec ID: {}", id);

        LigneFonctionnelleSpecifique entity = ligneFonctionnelleSpecifiqueRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Ligne fonctionnelle spécifique non trouvée avec l'ID: {}", id);
                    return new ResourceNotFoundException("Ligne Fonctionnelle Spécifique non trouvée avec l'ID: " + id);
                });

        log.debug("Ligne trouvée - Nature: {}, Marché: {}, Actif: {}",
                entity.getNatureLigneFonctionnelle().getLabel(),
                entity.getMarche().getNom(),
                entity.isActif());

        return ligneFonctionnelleSpecifiqueMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public Page<LigneFonctionnelleSpecifiqueResponse> getLignesFonctionnellesSpecifiques(
            LigneFonctionnelleSpecifiqueSearchCriteria criteria, Pageable pageable) {

        log.info("Récupération des lignes fonctionnelles spécifiques - Page: {}, Size: {}", pageable.getPageNumber(), pageable.getPageSize());

        log.debug("Critères de recherche: {}", criteria);

        Specification<LigneFonctionnelleSpecifique> spec = LigneFonctionnelleSpecifiqueSpecification.searchByCriteria(criteria);
        Page<LigneFonctionnelleSpecifique> page = ligneFonctionnelleSpecifiqueRepository.findAll(spec, pageable);

        log.info("{} lignes fonctionnelles spécifiques trouvées sur {} au total", page.getNumberOfElements(), page.getTotalElements());

        return page.map(ligneFonctionnelleSpecifiqueMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<LigneFonctionnelleSpecifiqueResponse> getLignesFonctionnellesSpecifiquesByBanqueId(
            Long banqueId, LigneFonctionnelleSpecifiqueSearchCriteria criteria, Pageable pageable) {

        log.info("Récupération des lignes fonctionnelles spécifiques pour la banque ID: {}", banqueId);
        log.debug("Critères: {}, Page: {}, Size: {}", criteria, pageable.getPageNumber(), pageable.getPageSize());

        // 1. Validate that the bank exists
        if (!banqueRepository.existsById(banqueId)) {
            log.error("Banque non trouvée avec l'ID: {}", banqueId);
            throw new ResourceNotFoundException("Banque non trouvée avec l'ID: " + banqueId + ". Impossible de récupérer les lignes fonctionnelles spécifiques.");
        }

        log.debug("Banque ID {} validée", banqueId);

        // 2. Set the banqueId in the criteria
        criteria.setBanqueId(banqueId);

        // 3. Use the existing Specification to find the entities
        Specification<LigneFonctionnelleSpecifique> spec = LigneFonctionnelleSpecifiqueSpecification.searchByCriteria(criteria);
        Page<LigneFonctionnelleSpecifique> page = ligneFonctionnelleSpecifiqueRepository.findAll(spec, pageable);

        log.info("{} lignes fonctionnelles spécifiques trouvées pour la banque ID {} sur {} au total",
                page.getNumberOfElements(), banqueId, page.getTotalElements());

        // 4. Map entities to DTOs and return
        return page.map(ligneFonctionnelleSpecifiqueMapper::toDto);
    }

    @Transactional
    public LigneFonctionnelleSpecifiqueResponse updateLigneFonctionnelleSpecifique(Long id, LigneFonctionnelleSpecifiqueRequest request) {
        log.info("Mise à jour de la ligne fonctionnelle spécifique ID: {}", id);
        log.debug("Nouvelles données: {}", request);

        LigneFonctionnelleSpecifique existingEntity = ligneFonctionnelleSpecifiqueRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Ligne fonctionnelle spécifique non trouvée avec l'ID: {}", id);
                    return new ResourceNotFoundException("Ligne Fonctionnelle Spécifique non trouvée avec l'ID: " + id);
                });

        log.debug("Entité existante récupérée - Nature: {}, Marché: {}, Actif: {}",
                existingEntity.getNatureLigneFonctionnelle().getLabel(),
                existingEntity.getMarche().getNom(),
                existingEntity.isActif());

        // Validation des dates
        OffsetDateTime effectiveDateDepart = request.getDateDepart() != null ? request.getDateDepart() : existingEntity.getDateDepart();
        OffsetDateTime effectiveDateFin = request.getDateFin() != null ? request.getDateFin() : existingEntity.getDateFin();
        validateDateRange(effectiveDateDepart, effectiveDateFin);

        // Mise à jour des relations
        NatureLigneFonctionnelle targetNature = (request.getNatureLigneFonctionnelleId() != null) ?
                natureLigneFonctionnelleRepository.findById(request.getNatureLigneFonctionnelleId())
                        .orElseThrow(() -> {
                            log.error("NatureLigneFonctionnelle non trouvée avec l'ID: {}", request.getNatureLigneFonctionnelleId());
                            return new ResourceNotFoundException("NatureLigneFonctionnelle non trouvée avec l'ID: " + request.getNatureLigneFonctionnelleId());
                        })
                : existingEntity.getNatureLigneFonctionnelle();

        Marche targetMarche = (request.getMarcheId() != null) ?
                marcheRepository.findById(request.getMarcheId())
                        .orElseThrow(() -> {
                            log.error("Marché non trouvé avec l'ID: {}", request.getMarcheId());
                            return new ResourceNotFoundException("Marché non trouvé avec l'ID: " + request.getMarcheId());
                        })
                : existingEntity.getMarche();

        log.debug("Relations cibles - Nature: {}, Marché: {}", targetNature.getLabel(), targetMarche.getNom());

        // Vérification des doublons actifs
        boolean requestedActif = request.getActif() != null ? request.getActif() : existingEntity.isActif();
        if (requestedActif) {
            log.debug("Vérification des conflits avec les lignes générales");
            checkNoConflictWithGeneralLine(targetMarche, targetNature);

            log.debug("Vérification des doublons actifs");
            ligneFonctionnelleSpecifiqueRepository.findByMarcheAndNatureLigneFonctionnelleAndActifTrue(targetMarche, targetNature)
                    .ifPresent(existingActiveDuplicate -> {
                        if (!existingActiveDuplicate.getId().equals(id)) {
                            log.error("Doublon actif détecté - ID en conflit: {}", existingActiveDuplicate.getId());
                            throw new DuplicateEntryException("Impossible de mettre à jour. Une ligne fonctionnelle ACTIVE de ce type existe déjà pour ce marché.");
                        }
                    });
            log.debug("Aucun doublon actif en conflit");
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
        log.debug("Sauvegarde des modifications");
        LigneFonctionnelleSpecifique updatedEntity = ligneFonctionnelleSpecifiqueRepository.save(existingEntity);

        log.info("Ligne fonctionnelle spécifique ID {} mise à jour avec succès", updatedEntity.getId());

        return ligneFonctionnelleSpecifiqueMapper.toDto(updatedEntity);
    }

    @Transactional
    public void deactivateLigneFonctionnelleById(Long ligneId) throws Exception {
        log.info("Désactivation de la ligne fonctionnelle spécifique ID: {}", ligneId);

        LigneFonctionnelleSpecifique ligneToDeactivate = ligneFonctionnelleSpecifiqueRepository.findById(ligneId)
                .orElseThrow(() -> {
                    log.error("Ligne fonctionnelle spécifique non trouvée avec l'ID: {}", ligneId);
                    return new ResourceNotFoundException("Aucune Ligne Fonctionnelle spécifique avec l'ID: " + ligneId);
                });

        if(!ligneToDeactivate.isActif()) {
            log.error("Tentative de désactivation d'une ligne déjà inactive - ID: {}", ligneId);
            throw new Exception("Ligne fonctionnelle spécifique est déjà désactivée");
        }

        log.debug("Changement du statut actif à false");
        ligneToDeactivate.setActif(false);
        ligneFonctionnelleSpecifiqueRepository.save(ligneToDeactivate);

        log.info("Ligne fonctionnelle spécifique ID {} désactivée avec succès", ligneId);
    }

    @Transactional
    public void deleteLigneFonctionnelleSpecifique(Long id) {
        log.info("Suppression de la ligne fonctionnelle spécifique ID: {}", id);

        if (!ligneFonctionnelleSpecifiqueRepository.existsById(id)) {
            log.error("Ligne fonctionnelle spécifique non trouvée avec l'ID: {}", id);
            throw new ResourceNotFoundException("Ligne Fonctionnelle Spécifique non trouvée avec l'ID: " + id);
        }

        ligneFonctionnelleSpecifiqueRepository.deleteById(id);
        log.info("Ligne fonctionnelle spécifique ID {} supprimée avec succès", id);
    }

    private void validateDateRange(OffsetDateTime dateDepart, OffsetDateTime dateFin) {
        log.debug("Validation de la plage de dates - DateDepart: {}, DateFin: {}", dateDepart, dateFin);

        if (dateDepart != null && dateFin != null && dateFin.isBefore(dateDepart)) {
            log.error("Date de fin ({}) antérieure à la date de départ ({})", dateFin, dateDepart);
            throw new IllegalArgumentException("La date de fin (" + dateFin + ") ne peut pas être antérieure à la date de départ (" + dateDepart + ").");
        }

        log.debug("Plage de dates valide");
    }

    @Transactional(readOnly = true)
    public Page<LigneFonctionnelleSpecifiqueResponse> getLinesByMarche(Long marcheId, Pageable pageable) {

        log.info("Récupération de toutes les lignes pour le marché ID: {}", marcheId);

        // 1. Vérifier que le marché existe
        if (!marcheRepository.existsById(marcheId)) {
            log.error("Marché non trouvé avec l'ID: {}", marcheId);
            throw new ResourceNotFoundException("Marché non trouvé avec l'ID: " + marcheId);
        }

        log.debug("Marché ID {} validé", marcheId);

        // 2. Construire la spécification pour récupérer toutes les lignes du marché
        Specification<LigneFonctionnelleSpecifique> spec = (root, query, cb) -> {
            return cb.equal(root.get("marche").get("id"), marcheId);
        };

        // 3. Récupérer les lignes
        Page<LigneFonctionnelleSpecifique> page =
                ligneFonctionnelleSpecifiqueRepository.findAll(spec, pageable);

        log.info("{} lignes trouvées pour le marché ID: {} sur {} au total",
                page.getNumberOfElements(), marcheId, page.getTotalElements());

        // 4. Mapper chaque ligne avec le mapper approprié selon sa nature
        return page.map(entity -> ligneFonctionnelleSpecifiqueMapper.toDto(entity));
    }


    @Transactional(readOnly = true)
    public AgregatedSuiviePrefiLigneResponseDto getSuiviePrefiLigneResponse(Long lineId, Pageable pageable) {
        log.info("Récupération du suivi de préfinancement pour la ligne ID: {}", lineId);

        LigneFonctionnelleSpecifique specificLine = ligneFonctionnelleSpecifiqueRepository.findById(lineId)
                .orElseThrow(() -> {
                    log.error("Ligne fonctionnelle spécifique non trouvée avec l'ID: {}", lineId);
                    return new ResourceNotFoundException("Ligne Fonctionnelle Spécifique non trouvée avec l'ID: " + lineId);
                });

        Marche marche = specificLine.getMarche();

        if (marche == null) {
            log.error("Aucun marché associé à la ligne ID: {}", lineId);
            throw new ResourceNotFoundException("Aucun marché associé à la ligne fonctionnelle avec l'ID: " + lineId);
        }

        log.debug("Marché associé: {} (ID: {})", marche.getNom(), marche.getId());

        List<Prefinancement> allPrefi = prefinancementRepository.findByMarche(marche);
        log.debug("{} préfinancements  trouvés", allPrefi.size());

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (Prefinancement prefi : allPrefi) {
            if (prefi.getType() == Type.REMBOURSEMENT) totalDebit = totalDebit.add(prefi.getMontant());
            else if (prefi.getType() == Type.DEBLOCAGE) totalCredit = totalCredit.add(prefi.getMontant());
        }
        BigDecimal reste = totalCredit.subtract(totalDebit);

        log.debug("Totaux calculés - Débit: {}, Crédit: {}, Reste: {}", totalDebit, totalCredit, reste);

        Specification<Prefinancement> spec = Specification.where(PrefinancementSpecification.byMarcheId(marche.getId()));

        Page<Prefinancement> prefiPage = prefinancementRepository.findAll(spec, pageable);
        log.debug("{} préfinancements récupérés pour la page courante", prefiPage.getNumberOfElements());

        Page<SuiviePrefiLigneResponseDto> pageOfDtos = prefiPage.map(prefinancementMapper::toSuiviePrefiLigneResponseDto);

        AgregatedSuiviePrefiLigneResponseDto aggregatedDto = new AgregatedSuiviePrefiLigneResponseDto();
        aggregatedDto.setBanqueName(specificLine.getMarche().getBanque().getNom());
        aggregatedDto.setMarcheName(marche.getNom());
        aggregatedDto.setMarcheNumero(marche.getNumero());
        aggregatedDto.setTotalDebit(totalDebit);
        aggregatedDto.setTotalCredit(totalCredit);
        aggregatedDto.setReste(reste);
        aggregatedDto.setPage(pageOfDtos);

        log.info("Suivi de préfinancement récupéré avec succès pour la ligne ID: {}", lineId);
        return aggregatedDto;
    }

    @Transactional(readOnly = true)
    public AgregatedSuivieDcLigneResponseDto getSuivieDcLigneResponse(Long lineId, Pageable pageable) {
        log.info("Récupération du suivi de droit constaté pour la ligne ID: {}", lineId);

        LigneFonctionnelleSpecifique specificLine = ligneFonctionnelleSpecifiqueRepository.findById(lineId)
                .orElseThrow(() -> {
                    log.error("Ligne fonctionnelle spécifique non trouvée avec l'ID: {}", lineId);
                    return new ResourceNotFoundException("Ligne Fonctionnelle Spécifique non trouvée avec l'ID: " + lineId);
                });

        Marche marche = specificLine.getMarche();
        if (marche == null) {
            log.error("Aucun marché associé à la ligne ID: {}", lineId);
            throw new ResourceNotFoundException("Aucun marché associé à la ligne fonctionnelle avec l'ID: " + lineId);
        }

        log.debug("Marché associé: {} (ID: {})", marche.getNom(), marche.getId());

        List<DroitConstate> allDc = droitConstateRepository.findByMarche(marche);
        log.debug("{} droits constatés  trouvés", allDc.size());

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (DroitConstate dc : allDc) {
            if (dc.getType() == Type.REMBOURSEMENT) totalDebit = totalDebit.add(dc.getMontant());
            else if (dc.getType() == Type.DEBLOCAGE) totalCredit = totalCredit.add(dc.getMontant());
        }

        BigDecimal reste = totalCredit.subtract(totalDebit);

        log.debug("Totaux calculés - Débit: {}, Crédit: {}, Reste: {}", totalDebit, totalCredit, reste);

        Specification<DroitConstate> spec = Specification.where(DroitConstateSpec.byMarcheId(marche.getId())) ;
        Page<DroitConstate> dcPage = droitConstateRepository.findAll(spec, pageable);

        log.debug("{} droits constatés récupérés pour la page courante", dcPage.getNumberOfElements());

        Page<SuivieDcLigneResponseDto> pageOfDtos = dcPage.map(droitConstateMapper::toSuivieDcLigneResponseDto);

        AgregatedSuivieDcLigneResponseDto aggregatedDto = new AgregatedSuivieDcLigneResponseDto();
        aggregatedDto.setBanqueName(specificLine.getMarche().getBanque().getNom());
        aggregatedDto.setMarcheName(marche.getNom());
        aggregatedDto.setMarcheNumero(marche.getNumero());
        aggregatedDto.setTotalDebit(totalDebit);
        aggregatedDto.setTotalCredit(totalCredit);
        aggregatedDto.setReste(reste);
        aggregatedDto.setPage(pageOfDtos);

        log.info("Suivi de droit constaté récupéré avec succès pour la ligne ID: {}", lineId);
        return aggregatedDto;
    }

    @Transactional(readOnly = true)
    public AgregatedSuivieFactoringLigneResponseDto getSuivieFactoringLigneResponse(Long lineId, Pageable pageable) {
        log.info("Récupération du suivi de factoring pour la ligne ID: {}", lineId);

        LigneFonctionnelleSpecifique specificLine = ligneFonctionnelleSpecifiqueRepository.findById(lineId)
                .orElseThrow(() -> {
                    log.error("Ligne fonctionnelle spécifique non trouvée avec l'ID: {}", lineId);
                    return new ResourceNotFoundException("Ligne Fonctionnelle Spécifique non trouvée avec l'ID: " + lineId);
                });

        Marche marche = specificLine.getMarche();
        if (marche == null) {
            log.error("Aucun marché associé à la ligne ID: {}", lineId);
            throw new ResourceNotFoundException("Aucun marché associé à la ligne fonctionnelle avec l'ID: " + lineId);
        }

        log.debug("Marché associé: {} (ID: {})", marche.getNom(), marche.getId());

        List<Factoring> allPaidFactoring = factoringRepository.findByMarche(marche);
        log.debug("{} factorings payés trouvés", allPaidFactoring.size());

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (Factoring f : allPaidFactoring) {
            if (f.getType() == Type.REMBOURSEMENT) totalDebit = totalDebit.add(f.getMontant());
            else if (f.getType() == Type.DEBLOCAGE) totalCredit = totalCredit.add(f.getMontant());
        }
        BigDecimal reste = totalCredit.subtract(totalDebit);

        log.debug("Totaux calculés - Débit: {}, Crédit: {}, Reste: {}", totalDebit, totalCredit, reste);

        Specification<Factoring> spec = Specification.where(FactoringSpec.byMarcheId(marche.getId()));
        Page<Factoring> factoringPage = factoringRepository.findAll(spec, pageable);

        log.debug("{} factorings récupérés pour la page courante", factoringPage.getNumberOfElements());

        Page<SuivieFactoringLineResponseDto> pageOfDtos = factoringPage.map(factoringMapper::toSuivieFactoringLigneResponseDto);

        AgregatedSuivieFactoringLigneResponseDto aggregatedDto = new AgregatedSuivieFactoringLigneResponseDto();
        aggregatedDto.setBanqueName(specificLine.getMarche().getBanque().getNom());
        aggregatedDto.setMarcheName(marche.getNom());
        aggregatedDto.setMarcheNumero(marche.getNumero());
        aggregatedDto.setTotalDebit(totalDebit);
        aggregatedDto.setTotalCredit(totalCredit);
        aggregatedDto.setReste(reste);
        aggregatedDto.setPage(pageOfDtos);

        log.info("Suivi de factoring récupéré avec succès pour la ligne ID: {}", lineId);
        return aggregatedDto;
    }


    /**
     * Vérifie qu'il n'existe pas de LigneFonctionnelle générale active avec la même nature
     * pour le marché spécifié.
     *
     * Cette règle garantit qu'un marché ne peut avoir qu'UNE ligne active par nature,
     * qu'elle soit générale ou spécifique.
     */
    private void checkNoConflictWithGeneralLine(Marche marche, NatureLigneFonctionnelle nature) {
        Optional<LigneFonctionnelle> existingGeneralLine =
                ligneFonctionnelleRepository.findByMarcheIdAndNatureLigneFonctionnelleIdAndActifTrue(
                        marche.getId(),
                        nature.getId()
                );

        existingGeneralLine.ifPresent(existing -> {
            log.error("Conflit détecté - Une LigneFonctionnelle générale active existe déjà - Marché: '{}', Nature: '{}', ID: {}",
                    marche.getNumero(), nature.getLabel(), existing.getId());
            throw new DuplicateEntryException(
                    "Impossible de créer une ligne spécifique. " +
                            "Le marché '" + marche.getNumero() + "' possède déjà une ligne fonctionnelle GÉNÉRALE active " +
                            "de type '" + nature.getLabel() + "' (ID: " + existing.getId() + "). " +
                            "Un marché ne peut avoir qu'une seule ligne active par nature."
            );
        });

        log.debug("Aucun conflit avec les lignes générales - validation réussie");
    }
}
