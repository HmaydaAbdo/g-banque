package staport.finance.gBanque.factoring.service;

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
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.INatureLigneFonctionnelleService;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.services.LigneFonctionnelleReader;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.services.LigneFonctionnelleSpecifiqueReader;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.factoring.dtos.request.EncoursFactoringSearchDto;
import staport.finance.gBanque.factoring.dtos.request.FactoringRequestDto;
import staport.finance.gBanque.factoring.dtos.request.FactoringSearchDto;
import staport.finance.gBanque.factoring.dtos.response.EncoursFactoringDto;
import staport.finance.gBanque.factoring.dtos.response.FactoringResponseDto;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.factoring.mapper.FactoringMapper;
import staport.finance.gBanque.factoring.repository.FactoringRepository;
import staport.finance.gBanque.factoring.spec.FactoringSpec;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.service.IMarcheService;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

/**
 * Implémentation du service Factoring.
 *
 * <p>Encapsulation des domaines (architecture.md) : la résolution du {@link Marche} passe par
 * {@link IMarcheService}, la résolution de la nature de ligne fonctionnelle "FACTORING" par
 * {@link INatureLigneFonctionnelleService} — jamais par injection directe d'un {@code Repository}
 * étranger. {@code LigneFonctionnelleReader}/{@code LigneFonctionnelleSpecifiqueReader} restent
 * inchangés (déjà des façades de lecture cross-domaine conformes).
 *
 * <p>Anti-N+1 : {@code FactoringRepository.findAll} porte l'{@code @EntityGraph}
 * {@code "Factoring.avecAssociations"} (marché + banque du marché).
 *
 * <p>Les totaux d'encours (déblocage/remboursement) sont calculés via une requête {@code SUM}
 * construite avec le même {@link Specification} que la recherche paginée (pas de chargement de
 * la liste complète en mémoire pour sommer côté Java — conventions/performance.md).
 * {@code endOfMonth} est recalculé à chaque appel (et non figé à la construction du bean).
 */
@Service
public class FactoringServiceImpl implements IFactoringService {

    private static final Logger log = LoggerFactory.getLogger(FactoringServiceImpl.class);
    private static final String FACTORING_NATURE_CODE = "FACTORING";

    private final FactoringRepository factoringRepository;
    private final IMarcheService marcheService;
    private final FactoringMapper factoringMapper;
    private final LigneFonctionnelleSpecifiqueReader ligneFonctionnelleSpecifiqueReader;
    private final LigneFonctionnelleReader ligneFonctionnelleReader;
    private final INatureLigneFonctionnelleService natureLigneFonctionnelleService;

    @PersistenceContext
    private EntityManager entityManager;

    public FactoringServiceImpl(FactoringRepository factoringRepository,
                                 IMarcheService marcheService,
                                 FactoringMapper factoringMapper,
                                 LigneFonctionnelleSpecifiqueReader ligneFonctionnelleSpecifiqueReader,
                                 LigneFonctionnelleReader ligneFonctionnelleReader,
                                 INatureLigneFonctionnelleService natureLigneFonctionnelleService) {
        this.factoringRepository = factoringRepository;
        this.marcheService = marcheService;
        this.factoringMapper = factoringMapper;
        this.ligneFonctionnelleSpecifiqueReader = ligneFonctionnelleSpecifiqueReader;
        this.ligneFonctionnelleReader = ligneFonctionnelleReader;
        this.natureLigneFonctionnelleService = natureLigneFonctionnelleService;
        log.info("FactoringServiceImpl initialisé avec succès");
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = Factoring.class)
    public FactoringResponseDto createFactoring(FactoringRequestDto requestDTO) throws Exception {
        log.info("Début de la création d'un factoring pour le marché ID: {}", requestDTO.marcheId());

        Marche marche = marcheService.findOrThrow(requestDTO.marcheId());
        log.debug("Marché récupéré: {} (Numéro: {})", marche.getNom(), marche.getNumero());

        NatureLigneFonctionnelle factoringNature = getFactoringNature();
        LigneResult ligneResult = findActiveFactoringLine(marche, factoringNature);

        Factoring factoring = factoringMapper.toEntity(requestDTO, marche);
        factoring.setLigneFonctionnelleSpecifique(ligneResult.specifique());
        factoring.setLigneFonctionnelle(ligneResult.generale());
        factoring = factoringRepository.save(factoring);

        log.info("Factoring créé avec succès - ID: {}", factoring.getId());

        return factoringMapper.toDto(factoring);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Factoring.class)
    public FactoringResponseDto updateFactoring(Long id, FactoringRequestDto requestDTO) throws Exception {
        log.info("Début de la modification du factoring ID: {}", id);

        Factoring existingFactoring = factoringRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Factoring non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Factoring not found with ID: " + id);
                });

        Marche marche = marcheService.findOrThrow(requestDTO.marcheId());
        log.debug("Marché récupéré pour la mise à jour: {}", marche.getNom());

        if (!existingFactoring.getMarche().getId().equals(marche.getId())) {
            log.warn("Changement de marché détecté (ancien: {}, nouveau: {}), re-validation nécessaire",
                    existingFactoring.getMarche().getNom(), marche.getNom());

            NatureLigneFonctionnelle factoringNature = getFactoringNature();
            LigneResult ligneResult = findActiveFactoringLine(marche, factoringNature);
            existingFactoring.setLigneFonctionnelleSpecifique(ligneResult.specifique());
            existingFactoring.setLigneFonctionnelle(ligneResult.generale());
        }

        existingFactoring.setMarche(marche);
        existingFactoring.setDateEmission(requestDTO.dateEmission());
        existingFactoring.setMontant(requestDTO.montant());
        existingFactoring.setType(requestDTO.type());
        existingFactoring.setObservation(marche.getNumero());

        Factoring updatedFactoring = factoringRepository.save(existingFactoring);
        log.info("Factoring modifié avec succès - ID: {}", id);

        return factoringMapper.toDto(updatedFactoring);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = Factoring.class)
    public void deleteFactoring(Long id) {
        log.info("Tentative de suppression du factoring ID: {}", id);

        if (!factoringRepository.existsById(id)) {
            log.error("Impossible de supprimer - Factoring non trouvé avec l'ID: {}", id);
            throw new ResourceNotFoundException("Factoring not found with ID: " + id);
        }

        factoringRepository.deleteById(id);
        log.info("Factoring ID: {} supprimé avec succès", id);
    }

    @Override
    @Transactional(readOnly = true)
    public FactoringResponseDto findById(Long id) {
        Factoring factoring = factoringRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Factoring non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Factoring not found with ID: " + id);
                });

        return factoringMapper.toDto(factoring);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FactoringResponseDto> findAll(FactoringSearchDto searchDTO, Pageable pageable) {
        log.info("Recherche de factorings avec critères: {}", searchDTO);

        Specification<Factoring> spec = FactoringSpec.searchByCriteria(searchDTO);
        Page<Factoring> factoringPage = factoringRepository.findAll(spec,
                pageable.getSort().isSorted() ? pageable
                        : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.info("Recherche terminée - {} résultat(s) trouvé(s) sur {} élément(s) au total",
                factoringPage.getNumberOfElements(), factoringPage.getTotalElements());

        return factoringPage.map(factoringMapper::toDto);
    }

    @Override
    @Transactional
    public FactoringResponseDto toggleStatus(Long id) {
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

        log.info("Statut du factoring ID: {} modifié avec succès de {} vers {}", id, existingStatus, newStatus);

        return factoringMapper.toDto(updatedFactoring);
    }

    @Override
    @Transactional(readOnly = true)
    public EncoursFactoringDto getRemboursementEncoursForBanque(EncoursFactoringSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Récupération des encours de remboursement factoring pour la banque ID: {}", banqueId);

        LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        Specification<Factoring> spec = FactoringSpec.byBanqueId(banqueId)
                .and(FactoringSpec.outstandingRemboursementUntil(endOfMonth))
                .and(FactoringSpec.byMarcheName(searchDto.marcheName()))
                .and(FactoringSpec.byMarcheNumero(searchDto.marcheNumero()))
                .and(FactoringSpec.hasMontant(searchDto.montant()));

        Page<Factoring> factoringPage = factoringRepository.findAll(spec,
                pageable.getSort().isSorted() ? pageable
                        : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        BigDecimal totalDesEncours = sumMontant(spec);
        log.info("Total des encours de remboursement factoring calculé: {} pour la banque ID: {}", totalDesEncours, banqueId);

        Page<FactoringResponseDto> pageDto = factoringPage.map(factoringMapper::toDto);

        return new EncoursFactoringDto(totalDesEncours, pageDto);
    }

    @Override
    @Transactional(readOnly = true)
    public EncoursFactoringDto getDeblocageEncoursForBanque(EncoursFactoringSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Récupération des encours de déblocage factoring pour la banque ID: {}", banqueId);

        LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        Specification<Factoring> spec = FactoringSpec.byBanqueId(banqueId)
                .and(FactoringSpec.outstandingDeblocageUntil(endOfMonth))
                .and(FactoringSpec.byMarcheName(searchDto.marcheName()))
                .and(FactoringSpec.byMarcheNumero(searchDto.marcheNumero()))
                .and(FactoringSpec.hasMontant(searchDto.montant()));

        Page<Factoring> factoringPage = factoringRepository.findAll(spec,
                pageable.getSort().isSorted() ? pageable
                        : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        BigDecimal totalDesEncours = sumMontant(spec);
        log.info("Total des encours de déblocage factoring calculé: {} pour la banque ID: {}", totalDesEncours, banqueId);

        Page<FactoringResponseDto> pageDto = factoringPage.map(factoringMapper::toDto);

        return new EncoursFactoringDto(totalDesEncours, pageDto);
    }

    /**
     * Somme des montants correspondant à la {@link Specification} donnée, calculée en base
     * (agrégat SQL) — jamais en chargeant les entités en mémoire pour sommer côté Java
     * (conventions/performance.md).
     */
    private BigDecimal sumMontant(Specification<Factoring> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> query = cb.createQuery(BigDecimal.class);
        Root<Factoring> root = query.from(Factoring.class);
        query.select(cb.coalesce(cb.sum(root.get("montant")), BigDecimal.ZERO));
        var predicate = spec.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }
        return entityManager.createQuery(query).getSingleResult();
    }

    private NatureLigneFonctionnelle getFactoringNature() {
        log.debug("Recherche de la nature de ligne fonctionnelle Factoring (code={})", FACTORING_NATURE_CODE);
        return natureLigneFonctionnelleService.findByCode(FACTORING_NATURE_CODE)
                .orElseThrow(() -> {
                    log.error("Nature de ligne fonctionnelle 'FACTORING' introuvable en base");
                    return new ResourceNotFoundException(
                            "Nature de ligne fonctionnelle FACTORING introuvable. " +
                            "Vérifiez la table nature_ligne_fonctionnelle."
                    );
                });
    }

    /**
     * Recherche et retourne la ligne fonctionnelle Factoring active pour un marché.
     * Priorité : spécifique > générale liée au marché.
     * Lève une exception si aucune ligne n'est trouvée.
     */
    private LigneResult findActiveFactoringLine(Marche marche, NatureLigneFonctionnelle factoringNature) throws Exception {
        log.debug("Recherche d'une ligne Factoring active pour le marché '{}' (ID: {})", marche.getNom(), marche.getId());

        Optional<LigneFonctionnelleSpecifique> ligneSpecifique =
                ligneFonctionnelleSpecifiqueReader.findByMarcheAndNatureLigneFonctionnelleAndActifTrue(marche, factoringNature);
        if (ligneSpecifique.isPresent()) {
            log.info("Ligne spécifique Factoring active trouvée pour le marché '{}' (ID: {})",
                    marche.getNom(), ligneSpecifique.get().getId());
            return new LigneResult(ligneSpecifique.get(), null);
        }

        Optional<LigneFonctionnelle> ligneGenerale =
                ligneFonctionnelleReader.findActiveLineForMarche(marche.getId(), factoringNature.getId());
        if (ligneGenerale.isPresent()) {
            log.info("Ligne générale Factoring active trouvée pour le marché '{}' (ID: {})",
                    marche.getNom(), ligneGenerale.get().getId());
            return new LigneResult(null, ligneGenerale.get());
        }

        log.error("Aucune ligne Factoring active pour le marché '{}'", marche.getNom());
        throw new Exception(
                "Impossible de créer un factoring. Le marché '" + marche.getNom() +
                "' n'est associé à aucune ligne fonctionnelle Factoring active " +
                "(ni spécifique, ni générale liée à ce marché)."
        );
    }

    /** Résultat de la recherche de ligne : exactement l'une des deux est non-null. */
    private record LigneResult(LigneFonctionnelleSpecifique specifique, LigneFonctionnelle generale) {}
}
