package staport.finance.gBanque.droitConstate.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
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
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.INatureLigneFonctionnelleService;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.services.LigneFonctionnelleReader;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.services.LigneFonctionnelleSpecifiqueReader;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.droitConstate.dtos.request.DroitConstateRequestDto;
import staport.finance.gBanque.droitConstate.dtos.request.DroitConstateSearchDto;
import staport.finance.gBanque.droitConstate.dtos.request.EncoursDroitConstateSearchDto;
import staport.finance.gBanque.droitConstate.dtos.response.DroitConstateResponseDto;
import staport.finance.gBanque.droitConstate.dtos.response.EncoursDroitConstateDto;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.droitConstate.mapper.DroitConstateMapper;
import staport.finance.gBanque.droitConstate.repositories.DroitConstateRepository;
import staport.finance.gBanque.droitConstate.spec.DroitConstateSpec;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.service.IMarcheService;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

/**
 * Implémentation du service Droit Constaté.
 *
 * <p>Encapsulation des domaines (architecture.md) : la résolution du {@link Marche} passe par
 * {@link IMarcheService}, celle de la {@link Banque} (champ direct, indépendant de
 * {@code marche.banque}) par {@link IBanqueService}, et la résolution de la nature de ligne
 * fonctionnelle "DROIT_CONSTATE" par {@link INatureLigneFonctionnelleService} — jamais par
 * injection directe d'un {@code Repository} étranger. {@code LigneFonctionnelleReader}/
 * {@code LigneFonctionnelleSpecifiqueReader} restent inchangés (déjà des façades de lecture
 * cross-domaine conformes).
 *
 * <p>Anti-N+1 : {@code DroitConstateRepository.findAll} porte l'{@code @EntityGraph}
 * {@code "DroitConstate.avecAssociations"} (marché + banque directe).
 *
 * <p>Les totaux d'encours (déblocage/remboursement) sont calculés via une requête {@code SUM}
 * construite avec le même {@link Specification} que la recherche paginée (pas de chargement de
 * la liste complète en mémoire pour sommer côté Java — conventions/performance.md).
 * {@code endOfMonth} est recalculé à chaque appel (et non figé à la construction du bean).
 */
@Service
public class DroitConstateServiceImpl implements IDroitConstateService {

    private static final Logger log = LoggerFactory.getLogger(DroitConstateServiceImpl.class);
    private static final String DROIT_CONSTATE_NATURE_CODE = "DROIT_CONSTATE";

    private final DroitConstateRepository droitConstateRepository;
    private final DroitConstateMapper droitConstateMapper;
    private final IMarcheService marcheService;
    private final IBanqueService banqueService;
    private final INatureLigneFonctionnelleService natureLigneFonctionnelleService;
    private final LigneFonctionnelleSpecifiqueReader ligneFonctionnelleSpecifiqueReader;
    private final LigneFonctionnelleReader ligneFonctionnelleReader;

    @PersistenceContext
    private EntityManager entityManager;

    public DroitConstateServiceImpl(DroitConstateRepository droitConstateRepository,
                                     DroitConstateMapper droitConstateMapper,
                                     IMarcheService marcheService,
                                     IBanqueService banqueService,
                                     INatureLigneFonctionnelleService natureLigneFonctionnelleService,
                                     LigneFonctionnelleSpecifiqueReader ligneFonctionnelleSpecifiqueReader,
                                     LigneFonctionnelleReader ligneFonctionnelleReader) {
        this.droitConstateRepository = droitConstateRepository;
        this.droitConstateMapper = droitConstateMapper;
        this.marcheService = marcheService;
        this.banqueService = banqueService;
        this.natureLigneFonctionnelleService = natureLigneFonctionnelleService;
        this.ligneFonctionnelleSpecifiqueReader = ligneFonctionnelleSpecifiqueReader;
        this.ligneFonctionnelleReader = ligneFonctionnelleReader;
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = DroitConstate.class)
    public DroitConstateResponseDto createDroitConstate(DroitConstateRequestDto requestDTO) throws Exception {
        log.info("Début de la création d'un droit constaté pour le marché ID: {}", requestDTO.marcheId());

        Marche marche = marcheService.findOrThrow(requestDTO.marcheId());
        log.debug("Marché récupéré: {} (Numéro: {})", marche.getNom(), marche.getNumero());

        NatureLigneFonctionnelle dcNature = getDroitConstateNature();
        LigneResult ligneResult = findActiveDroitConstateLine(marche, dcNature);

        Banque banque = banqueService.findOrThrow(requestDTO.banqueId());
        log.debug("Banque résolue pour le DC: {} (ID: {})", banque.getNom(), banque.getId());

        DroitConstate droitConstate = droitConstateMapper.toEntity(requestDTO, marche, banque);
        droitConstate.setLigneFonctionnelleSpecifique(ligneResult.specifique());
        droitConstate.setLigneFonctionnelle(ligneResult.generale());

        droitConstate = droitConstateRepository.save(droitConstate);
        log.info("Droit constaté créé avec succès - ID: {}", droitConstate.getId());

        return droitConstateMapper.toDto(droitConstate);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = DroitConstate.class)
    public DroitConstateResponseDto updateDroitConstate(Long id, DroitConstateRequestDto requestDTO) throws Exception {
        log.info("Début de la modification du droit constaté ID: {}", id);

        DroitConstate existingDroitConstate = droitConstateRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Droit constaté non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Droit constaté not found with ID: " + id);
                });

        Marche marche = marcheService.findOrThrow(requestDTO.marcheId());
        log.debug("Marché récupéré pour la mise à jour: {}", marche.getNom());

        if (!existingDroitConstate.getMarche().getId().equals(marche.getId())) {
            log.warn("Changement de marché détecté (ancien: {}, nouveau: {}), re-validation nécessaire",
                    existingDroitConstate.getMarche().getNom(), marche.getNom());

            LigneResult ligneResult = findActiveDroitConstateLine(marche, getDroitConstateNature());
            existingDroitConstate.setLigneFonctionnelleSpecifique(ligneResult.specifique());
            existingDroitConstate.setLigneFonctionnelle(ligneResult.generale());
        }

        Banque banque = banqueService.findOrThrow(requestDTO.banqueId());
        log.debug("Banque résolue pour la mise à jour du DC: {} (ID: {})", banque.getNom(), banque.getId());

        existingDroitConstate.setMarche(marche);
        existingDroitConstate.setBanque(banque);
        existingDroitConstate.setDateEmission(requestDTO.dateEmission());
        existingDroitConstate.setMontant(requestDTO.montant());
        existingDroitConstate.setType(requestDTO.type());
        existingDroitConstate.setObservation(marche.getNumero());

        DroitConstate updatedDroitConstate = droitConstateRepository.save(existingDroitConstate);
        log.info("Droit constaté modifié avec succès - ID: {}", id);

        return droitConstateMapper.toDto(updatedDroitConstate);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = DroitConstate.class)
    public void deleteDroitConstate(Long id) {
        log.info("Tentative de suppression du droit constaté ID: {}", id);

        if (!droitConstateRepository.existsById(id)) {
            log.error("Impossible de supprimer - Droit constaté non trouvé avec l'ID: {}", id);
            throw new ResourceNotFoundException("Droit constate not found with ID: " + id);
        }

        droitConstateRepository.deleteById(id);
        log.info("Droit constaté ID: {} supprimé avec succès", id);
    }

    @Override
    @Transactional(readOnly = true)
    public DroitConstateResponseDto findById(Long id) {
        DroitConstate droitConstate = droitConstateRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Droit constaté non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Droit constaté not found with ID: " + id);
                });

        return droitConstateMapper.toDto(droitConstate);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DroitConstateResponseDto> findAll(DroitConstateSearchDto searchDTO, Pageable pageable) {
        log.info("Recherche de droits constatés avec critères: {}", searchDTO);

        Specification<DroitConstate> spec = DroitConstateSpec.searchByCriteria(searchDTO);
        Page<DroitConstate> droitConstatePage = droitConstateRepository.findAll(spec,
                pageable.getSort().isSorted() ? pageable
                        : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.info("Recherche terminée - {} résultat(s) trouvé(s) sur {} élément(s) au total",
                droitConstatePage.getNumberOfElements(), droitConstatePage.getTotalElements());

        return droitConstatePage.map(droitConstateMapper::toDto);
    }

    @Override
    @Transactional
    public DroitConstateResponseDto toggleStatus(Long id) {
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

        log.info("Statut du droit constaté ID: {} modifié avec succès de {} vers {}", id, existingStatus, newStatus);

        return droitConstateMapper.toDto(updatedDroitConstate);
    }

    @Override
    @Transactional(readOnly = true)
    public EncoursDroitConstateDto getRemboursementEncoursForBanque(EncoursDroitConstateSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Récupération des encours de remboursement pour la banque ID: {}", banqueId);

        LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        Specification<DroitConstate> spec = DroitConstateSpec.byBanqueId(banqueId)
                .and(DroitConstateSpec.outstandingRemboursementUntil(endOfMonth))
                .and(DroitConstateSpec.byMarcheName(searchDto.marcheName()))
                .and(DroitConstateSpec.byMarcheNumero(searchDto.marcheNumero()))
                .and(DroitConstateSpec.hasMontant(searchDto.montant()));

        Page<DroitConstate> droitConstatePage = droitConstateRepository.findAll(spec,
                pageable.getSort().isSorted() ? pageable
                        : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        BigDecimal totalDesEncours = sumMontant(spec);
        log.info("Total des encours de remboursement calculé: {} pour la banque ID: {}", totalDesEncours, banqueId);

        Page<DroitConstateResponseDto> pageDto = droitConstatePage.map(droitConstateMapper::toDto);

        return new EncoursDroitConstateDto(totalDesEncours, pageDto);
    }

    @Override
    @Transactional(readOnly = true)
    public EncoursDroitConstateDto getDeblocageEncoursForBanque(EncoursDroitConstateSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Récupération des encours de déblocage pour la banque ID: {}", banqueId);

        LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        Specification<DroitConstate> spec = DroitConstateSpec.byBanqueId(banqueId)
                .and(DroitConstateSpec.outstandingDeblocageUntil(endOfMonth))
                .and(DroitConstateSpec.byMarcheName(searchDto.marcheName()))
                .and(DroitConstateSpec.byMarcheNumero(searchDto.marcheNumero()))
                .and(DroitConstateSpec.hasMontant(searchDto.montant()));

        Page<DroitConstate> droitConstatePage = droitConstateRepository.findAll(spec,
                pageable.getSort().isSorted() ? pageable
                        : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        BigDecimal totalDesEncours = sumMontant(spec);
        log.info("Total des encours de déblocage calculé: {} pour la banque ID: {}", totalDesEncours, banqueId);

        Page<DroitConstateResponseDto> pageDto = droitConstatePage.map(droitConstateMapper::toDto);

        return new EncoursDroitConstateDto(totalDesEncours, pageDto);
    }

    /**
     * Somme des montants correspondant à la {@link Specification} donnée, calculée en base
     * (agrégat SQL) — jamais en chargeant les entités en mémoire pour sommer côté Java
     * (conventions/performance.md).
     */
    private BigDecimal sumMontant(Specification<DroitConstate> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> query = cb.createQuery(BigDecimal.class);
        Root<DroitConstate> root = query.from(DroitConstate.class);
        query.select(cb.coalesce(cb.sum(root.get("montant")), BigDecimal.ZERO));
        var predicate = spec.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }
        return entityManager.createQuery(query).getSingleResult();
    }

    private NatureLigneFonctionnelle getDroitConstateNature() {
        log.debug("Recherche de la nature de ligne fonctionnelle Droit Constaté (code={})", DROIT_CONSTATE_NATURE_CODE);
        return natureLigneFonctionnelleService.findByCode(DROIT_CONSTATE_NATURE_CODE)
                .orElseThrow(() -> {
                    log.error("Nature de ligne fonctionnelle 'DROIT_CONSTATE' introuvable en base");
                    return new ResourceNotFoundException(
                            "Nature de ligne fonctionnelle DROIT_CONSTATE introuvable. " +
                            "Vérifiez la table nature_ligne_fonctionnelle."
                    );
                });
    }

    /**
     * Recherche et retourne la ligne fonctionnelle 'Droit constaté' active pour un marché.
     * Priorité : spécifique > générale liée au marché.
     * Lève une exception si aucune ligne n'est trouvée.
     */
    private LigneResult findActiveDroitConstateLine(Marche marche, NatureLigneFonctionnelle dcNature) throws Exception {
        log.debug("Recherche d'une ligne 'Droit constaté' active pour le marché '{}' (ID: {})", marche.getNom(), marche.getId());

        Optional<LigneFonctionnelleSpecifique> ligneSpecifique =
                ligneFonctionnelleSpecifiqueReader.findByMarcheAndNatureLigneFonctionnelleAndActifTrue(marche, dcNature);
        if (ligneSpecifique.isPresent()) {
            log.info("Ligne spécifique 'Droit constaté' active trouvée pour le marché '{}' (ID: {})",
                    marche.getNom(), ligneSpecifique.get().getId());
            return new LigneResult(ligneSpecifique.get(), null);
        }

        Optional<LigneFonctionnelle> ligneGenerale =
                ligneFonctionnelleReader.findActiveLineForMarche(marche.getId(), dcNature.getId());
        if (ligneGenerale.isPresent()) {
            log.info("Ligne générale 'Droit constaté' active trouvée pour le marché '{}' (ID: {})",
                    marche.getNom(), ligneGenerale.get().getId());
            return new LigneResult(null, ligneGenerale.get());
        }

        log.error("Aucune ligne 'Droit constaté' active pour le marché '{}'", marche.getNom());
        throw new Exception(
                "Impossible de créer un droit constaté. Le marché '" + marche.getNom() +
                "' n'est associé à aucune ligne fonctionnelle 'Droit Constaté' active " +
                "(ni spécifique, ni générale liée à ce marché)."
        );
    }

    /** Résultat de la recherche de ligne : exactement l'une des deux est non-null. */
    private record LigneResult(LigneFonctionnelleSpecifique specifique, LigneFonctionnelle generale) {}
}
