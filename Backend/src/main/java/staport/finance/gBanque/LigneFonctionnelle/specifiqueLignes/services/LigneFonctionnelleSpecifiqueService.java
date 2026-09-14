package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.services;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.reports.service.StaportPdfReportService;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.enums.PorteeNature;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.services.LigneFonctionnelleReader;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.*;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.mappers.LigneFonctionnelleSpecifiqueMapper;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.repositories.LigneFonctionnelleSpecifiqueRepository;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.specifications.LigneFonctionnelleSpecifiqueSpecification;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.droitConstate.dtos.response.AgregatedSuivieDcLigneResponseDto;
import staport.finance.gBanque.droitConstate.dtos.response.SuivieDcLigneResponseDto;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.droitConstate.mapper.DroitConstateMapper;
import staport.finance.gBanque.droitConstate.service.DroitConstateReader;
import staport.finance.gBanque.droitConstate.spec.DroitConstateSpec;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.factoring.dtos.response.AgregatedSuivieFactoringLigneResponseDto;
import staport.finance.gBanque.factoring.dtos.response.SuivieFactoringLineResponseDto;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.factoring.mapper.FactoringMapper;
import staport.finance.gBanque.factoring.service.FactoringReader;
import staport.finance.gBanque.factoring.spec.FactoringSpec;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.service.IMarcheService;
import staport.finance.gBanque.prefinancement.dtos.response.AgregatedSuiviePrefiLigneResponseDto;
import staport.finance.gBanque.prefinancement.dtos.response.SuiviePrefiLigneResponseDto;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Type;
import staport.finance.gBanque.prefinancement.mapper.PrefinancementMapper;
import staport.finance.gBanque.prefinancement.services.PrefinancementReader;
import staport.finance.gBanque.prefinancement.spec.PrefinancementSpecification;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LigneFonctionnelleSpecifiqueService implements ILigneFonctionnelleSpecifiqueService {

    private static final Logger log = LoggerFactory.getLogger(LigneFonctionnelleSpecifiqueService.class);

    private final LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository;
    private final LigneFonctionnelleReader ligneFonctionnelleReader;
    private final LigneFonctionnelleSpecifiqueMapper ligneFonctionnelleSpecifiqueMapper;
    private final IMarcheService marcheService;

    private final PrefinancementReader prefinancementReader;
    private final PrefinancementMapper prefinancementMapper;

    private final DroitConstateReader droitConstateReader;
    private final DroitConstateMapper droitConstateMapper;

    private final FactoringReader factoringReader;
    private final FactoringMapper factoringMapper;
    private final StaportPdfReportService staportPdfReportService;


    public LigneFonctionnelleSpecifiqueService(
            LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository, LigneFonctionnelleReader ligneFonctionnelleReader,
            LigneFonctionnelleSpecifiqueMapper ligneFonctionnelleSpecifiqueMapper,
            IMarcheService marcheService, PrefinancementReader prefinancementReader, PrefinancementMapper prefinancementMapper, DroitConstateReader droitConstateReader, DroitConstateMapper droitConstateMapper, FactoringReader factoringReader, FactoringMapper factoringMapper, StaportPdfReportService staportPdfReportService) {
        this.ligneFonctionnelleSpecifiqueRepository = ligneFonctionnelleSpecifiqueRepository;
        this.ligneFonctionnelleReader = ligneFonctionnelleReader;
        this.ligneFonctionnelleSpecifiqueMapper = ligneFonctionnelleSpecifiqueMapper;
        this.marcheService = marcheService;
        this.prefinancementReader = prefinancementReader;
        this.prefinancementMapper = prefinancementMapper;
        this.droitConstateReader = droitConstateReader;
        this.droitConstateMapper = droitConstateMapper;
        this.factoringReader = factoringReader;
        this.factoringMapper = factoringMapper;
        this.staportPdfReportService = staportPdfReportService;
    }
    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = LigneFonctionnelleSpecifique.class)
    public LigneFonctionnelleSpecifiqueResponse createLigneFonctionnelleSpecifique(
            LigneFonctionnelleSpecifiqueRequest request, NatureLigneFonctionnelle nature) {
        log.info("Début de création d'une ligne fonctionnelle spécifique");
        log.debug("Détails de la requête: {}", request);

        // Validation des dates
        validateDateRange(request.dateDepart(), request.dateFin());

        log.debug("NatureLigneFonctionnelle trouvée: {}", nature.getLabel());

        // Validation portée : une ligne spécifique n'accepte que SPECIFIQUE ou GENERALE_ET_SPECIFIQUE
        validatePorteeForLigneSpecifique(nature);

        // Récupération du Marché
        log.debug("Recherche du Marché avec ID: {}", request.marcheId());
        Marche marche = marcheService.findOrThrow(request.marcheId());
        log.debug("Marché trouvé: {} (Numéro: {})", marche.getNom(), marche.getNumero());

        // Vérification doublon actif
        boolean isActif = request.actif() == null || request.actif();
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

    @Override
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

    @Override
    @Transactional(readOnly = true)
    public Page<LigneFonctionnelleSpecifiqueResponse> getLignesFonctionnellesSpecifiques(
            LigneFonctionnelleSpecifiqueSearchCriteria criteria, Pageable pageable) {

        log.info("Récupération des lignes fonctionnelles spécifiques - Page: {}, Size: {}", pageable.getPageNumber(), pageable.getPageSize());

        log.debug("Critères de recherche: {}", criteria);

        Specification<LigneFonctionnelleSpecifique> spec = LigneFonctionnelleSpecifiqueSpecification.searchByCriteria(criteria);
        Page<LigneFonctionnelleSpecifique> page = ligneFonctionnelleSpecifiqueRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.info("{} lignes fonctionnelles spécifiques trouvées sur {} au total", page.getNumberOfElements(), page.getTotalElements());

        return page.map(ligneFonctionnelleSpecifiqueMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LigneFonctionnelleSpecifiqueResponse> getLignesFonctionnellesSpecifiquesByBanqueId(
            Long banqueId, LigneFonctionnelleSpecifiqueSearchCriteria criteria, Pageable pageable) {

        log.info("Récupération des lignes fonctionnelles spécifiques pour la banque ID: {}", banqueId);
        log.debug("Critères: {}, Page: {}, Size: {}", criteria, pageable.getPageNumber(), pageable.getPageSize());

        // Existence de la banque validée par le Controller (IBanqueService.findOrThrow) avant l'appel.

        // 2. Set the banqueId in the criteria
        criteria = criteria.withBanqueId(banqueId);

        // 3. Use the existing Specification to find the entities
        Specification<LigneFonctionnelleSpecifique> spec = LigneFonctionnelleSpecifiqueSpecification.searchByCriteria(criteria);
        Page<LigneFonctionnelleSpecifique> page = ligneFonctionnelleSpecifiqueRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.info("{} lignes fonctionnelles spécifiques trouvées pour la banque ID {} sur {} au total",
                page.getNumberOfElements(), banqueId, page.getTotalElements());

        // 4. Map entities to DTOs and return
        return page.map(ligneFonctionnelleSpecifiqueMapper::toDto);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = LigneFonctionnelleSpecifique.class)
    public LigneFonctionnelleSpecifiqueResponse updateLigneFonctionnelleSpecifique(
            Long id, LigneFonctionnelleSpecifiqueRequest request, NatureLigneFonctionnelle nature) {
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
        LocalDate effectiveDateDepart = request.dateDepart() != null ? request.dateDepart() : existingEntity.getDateDepart();
        LocalDate effectiveDateFin = request.dateFin() != null ? request.dateFin() : existingEntity.getDateFin();
        validateDateRange(effectiveDateDepart, effectiveDateFin);

        // Mise à jour des relations (nature résolue par le Controller si natureLigneFonctionnelleId() fourni)
        NatureLigneFonctionnelle targetNature = (request.natureLigneFonctionnelleId() != null) ?
                nature
                : existingEntity.getNatureLigneFonctionnelle();

        Marche targetMarche = (request.marcheId() != null) ?
                marcheService.findOrThrow(request.marcheId())
                : existingEntity.getMarche();

        log.debug("Relations cibles - Nature: {}, Marché: {}", targetNature.getLabel(), targetMarche.getNom());

        // Validation portée sur la nature cible (update)
        validatePorteeForLigneSpecifique(targetNature);

        // Vérification des doublons actifs
        boolean requestedActif = request.actif() != null ? request.actif() : existingEntity.isActif();
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
        if (request.plafond() != null) existingEntity.setPlafond(request.plafond());
        if (request.taux() != null) existingEntity.setTaux(request.taux());
        if (request.dateDepart() != null) existingEntity.setDateDepart(request.dateDepart());
        if (request.dateFin() != null) existingEntity.setDateFin(request.dateFin());
        if (request.actif() != null) existingEntity.setActif(request.actif());

        // Mise à jour des relations
        existingEntity.setNatureLigneFonctionnelle(targetNature);
        existingEntity.setMarche(targetMarche);

        // Sauvegarde
        log.debug("Sauvegarde des modifications");
        LigneFonctionnelleSpecifique updatedEntity = ligneFonctionnelleSpecifiqueRepository.save(existingEntity);

        log.info("Ligne fonctionnelle spécifique ID {} mise à jour avec succès", updatedEntity.getId());

        return ligneFonctionnelleSpecifiqueMapper.toDto(updatedEntity);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = LigneFonctionnelleSpecifique.class)
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

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = LigneFonctionnelleSpecifique.class)
    public void deleteLigneFonctionnelleSpecifique(Long id) {
        log.info("Suppression de la ligne fonctionnelle spécifique ID: {}", id);

        if (!ligneFonctionnelleSpecifiqueRepository.existsById(id)) {
            log.error("Ligne fonctionnelle spécifique non trouvée avec l'ID: {}", id);
            throw new ResourceNotFoundException("Ligne Fonctionnelle Spécifique non trouvée avec l'ID: " + id);
        }

        ligneFonctionnelleSpecifiqueRepository.deleteById(id);
        log.info("Ligne fonctionnelle spécifique ID {} supprimée avec succès", id);
    }

    private void validateDateRange(LocalDate dateDepart, LocalDate dateFin) {
        log.debug("Validation de la plage de dates - DateDepart: {}, DateFin: {}", dateDepart, dateFin);

        if (dateDepart != null && dateFin != null && dateFin.isBefore(dateDepart)) {
            log.error("Date de fin ({}) antérieure à la date de départ ({})", dateFin, dateDepart);
            throw new IllegalArgumentException("La date de fin (" + dateFin + ") ne peut pas être antérieure à la date de départ (" + dateDepart + ").");
        }

        log.debug("Plage de dates valide");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LigneFonctionnelleSpecifiqueResponse> getLinesByMarche(Long marcheId, Pageable pageable) {

        log.info("Récupération de toutes les lignes pour le marché ID: {}", marcheId);

        // 1. Vérifier que le marché existe
        if (!marcheService.existsById(marcheId)) {
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
                ligneFonctionnelleSpecifiqueRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.info("{} lignes trouvées pour le marché ID: {} sur {} au total",
                page.getNumberOfElements(), marcheId, page.getTotalElements());

        // 4. Mapper chaque ligne avec le mapper approprié selon sa nature
        return page.map(entity -> ligneFonctionnelleSpecifiqueMapper.toDto(entity));
    }


    @Override
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
        // Un marché archivé est exclu définitivement du suivi (cf. Marche.actif), même règle que
        // le calcul d'encours (LigneFonctionnelleSpecifiqueMapper.computeEncoursAndSolde).
        if (!marche.isActif()) {
            log.error("Le marché associé à la ligne ID {} est archivé", lineId);
            throw new ResourceNotFoundException("Le marché associé à la ligne fonctionnelle avec l'ID " + lineId + " est archivé.");
        }

        log.debug("Marché associé: {} (ID: {})", marche.getNom(), marche.getId());

        List<Prefinancement> allPrefi = prefinancementReader.findByMarche(marche);
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

        Page<Prefinancement> prefiPage = prefinancementReader.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));
        log.debug("{} préfinancements récupérés pour la page courante", prefiPage.getNumberOfElements());

        Page<SuiviePrefiLigneResponseDto> pageOfDtos = prefiPage.map(prefinancementMapper::toSuiviePrefiLigneResponseDto);

        AgregatedSuiviePrefiLigneResponseDto aggregatedDto = new AgregatedSuiviePrefiLigneResponseDto(
                specificLine.getMarche().getBanque().getNom(), marche.getNom(), marche.getNumero(),
                totalDebit, totalCredit, reste, pageOfDtos);

        log.info("Suivi de préfinancement récupéré avec succès pour la ligne ID: {}", lineId);
        return aggregatedDto;
    }

    @Override
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
        // Un marché archivé est exclu définitivement du suivi (cf. Marche.actif), même règle que
        // le calcul d'encours (LigneFonctionnelleSpecifiqueMapper.computeEncoursAndSolde).
        if (!marche.isActif()) {
            log.error("Le marché associé à la ligne ID {} est archivé", lineId);
            throw new ResourceNotFoundException("Le marché associé à la ligne fonctionnelle avec l'ID " + lineId + " est archivé.");
        }

        log.debug("Marché associé: {} (ID: {})", marche.getNom(), marche.getId());

        List<DroitConstate> allDc = droitConstateReader.findByMarche(marche);
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
        Page<DroitConstate> dcPage = droitConstateReader.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.debug("{} droits constatés récupérés pour la page courante", dcPage.getNumberOfElements());

        Page<SuivieDcLigneResponseDto> pageOfDtos = dcPage.map(droitConstateMapper::toSuivieDcLigneResponseDto);

        AgregatedSuivieDcLigneResponseDto aggregatedDto = new AgregatedSuivieDcLigneResponseDto(
                specificLine.getMarche().getBanque().getNom(), marche.getNom(), marche.getNumero(),
                totalDebit, totalCredit, reste, pageOfDtos);

        log.info("Suivi de droit constaté récupéré avec succès pour la ligne ID: {}", lineId);
        return aggregatedDto;
    }

    @Override
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
        // Un marché archivé est exclu définitivement du suivi (cf. Marche.actif), même règle que
        // le calcul d'encours (LigneFonctionnelleSpecifiqueMapper.computeEncoursAndSolde).
        if (!marche.isActif()) {
            log.error("Le marché associé à la ligne ID {} est archivé", lineId);
            throw new ResourceNotFoundException("Le marché associé à la ligne fonctionnelle avec l'ID " + lineId + " est archivé.");
        }

        log.debug("Marché associé: {} (ID: {})", marche.getNom(), marche.getId());

        List<Factoring> allPaidFactoring = factoringReader.findByMarche(marche);
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
        Page<Factoring> factoringPage = factoringReader.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.debug("{} factorings récupérés pour la page courante", factoringPage.getNumberOfElements());

        Page<SuivieFactoringLineResponseDto> pageOfDtos = factoringPage.map(factoringMapper::toSuivieFactoringLigneResponseDto);

        AgregatedSuivieFactoringLigneResponseDto aggregatedDto = new AgregatedSuivieFactoringLigneResponseDto(
                specificLine.getMarche().getBanque().getNom(), marche.getNom(), marche.getNumero(),
                totalDebit, totalCredit, reste, pageOfDtos);

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
    /**
     * Règle Métier — Portée : vérifie qu'une nature est autorisée pour les lignes spécifiques.
     * Portées valides : SPECIFIQUE ou GENERALE_ET_SPECIFIQUE.
     */
    private void validatePorteeForLigneSpecifique(NatureLigneFonctionnelle nature) {
        if (nature.getPortee() == PorteeNature.GENERALE) {
            log.warn("Portée invalide pour ligne spécifique — nature '{}' a portée GENERALE", nature.getLabel());
            throw new IllegalArgumentException(
                    "La nature '" + nature.getLabel() + "' est réservée aux lignes générales et ne peut pas être utilisée dans une ligne spécifique."
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByNatureId(Long natureId) {
        return ligneFonctionnelleSpecifiqueRepository.existsByNatureLigneFonctionnelleId(natureId);
    }

    private void checkNoConflictWithGeneralLine(Marche marche, NatureLigneFonctionnelle nature) {
        Optional<LigneFonctionnelle> existingGeneralLine =
                ligneFonctionnelleReader.findActiveLineForMarche(
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

    // ── Export PDF ───────────────────────────────────────────────────────────

    /**
     * Exporte en PDF l'intégralité des lignes fonctionnelles spécifiques correspondant aux filtres
     * fournis (mêmes critères que {@link #getLignesFonctionnellesSpecifiques}) — sans pagination.
     * Réutilisé par les écrans encours ({@code criteria.actif() == true}) et archivées
     * ({@code criteria.actif() == false}) : seul le titre du rapport varie selon ce critère.
     * <p>
     * Passe par {@link LigneFonctionnelleSpecifiqueMapper#toDto} pour le calcul encours/solde/expired
     * — jamais recalculé ici (pas de duplication de logique métier, cf. CLAUDE.md).
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] exportPdf(LigneFonctionnelleSpecifiqueSearchCriteria criteria) {
        log.info("Export PDF des lignes fonctionnelles spécifiques — critères: {}", criteria);

        Specification<LigneFonctionnelleSpecifique> spec = LigneFonctionnelleSpecifiqueSpecification.searchByCriteria(criteria);
        List<LigneFonctionnelleSpecifique> entities = ligneFonctionnelleSpecifiqueRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "id"));
        List<LigneFonctionnelleSpecifiqueResponse> lignes = entities.stream()
                .map(ligneFonctionnelleSpecifiqueMapper::toDto)
                .collect(Collectors.toList());

        String title = "Lignes Fonctionnelles Spécifiques" + statusSuffix(criteria);
        BigDecimal totalPlafond = lignes.stream()
                .map(LigneFonctionnelleSpecifiqueResponse::plafond)
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

                    float[] colWidths = {14f, 12f, 12f, 10f, 10f, 10f, 8f, 10f, 10f, 8f};
                    PdfPTable table = new PdfPTable(colWidths);
                    table.setWidthPercentage(100f);
                    table.setSpacingBefore(8f);
                    table.setHeaderRows(1);

                    for (String h : new String[]{
                            "Banque", "Nature", "Marché", "Plafond (MAD)", "Encours (MAD)",
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
                        LigneFonctionnelleSpecifiqueResponse l = lignes.get(i);
                        Color rowBg = i % 2 == 0 ? Color.WHITE : rowAlt;

                        addPdfCell(table, l.banqueNom(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                        addPdfCell(table, l.natureLigneFonctionnelleLabel(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                        addPdfCell(table, l.marcheNumero(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                        addPdfCell(table, formatMontantPdf(l.plafond()), cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);
                        addPdfCell(table, formatMontantPdf(l.encours()), cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);
                        addPdfCell(table, formatMontantPdf(l.solde()), cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);
                        addPdfCell(table, formatMontantPdf(l.taux()), cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);
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
                            new Phrase(formatMontantPdf(totalPlafond), totalFont));
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
                    throw new RuntimeException("Erreur lors de la génération du contenu du PDF des lignes fonctionnelles spécifiques.", e);
                }
            });
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des lignes fonctionnelles spécifiques.", e);
        }
    }

    private String statusSuffix(LigneFonctionnelleSpecifiqueSearchCriteria criteria) {
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

    private String formatMontantPdf(BigDecimal montant) {
        if (montant == null) return "—";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        return formatter.format(montant);
    }
}
