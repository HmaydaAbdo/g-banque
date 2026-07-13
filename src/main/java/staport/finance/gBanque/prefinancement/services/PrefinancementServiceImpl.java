package staport.finance.gBanque.prefinancement.services;

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
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.service.IMarcheService;
import staport.finance.gBanque.prefinancement.dtos.request.EncoursPrefinancementSearchDto;
import staport.finance.gBanque.prefinancement.dtos.request.PrefinancementRequestDTO;
import staport.finance.gBanque.prefinancement.dtos.request.PrefinancementSearchDTO;
import staport.finance.gBanque.prefinancement.dtos.response.EncoursPrefinancementDto;
import staport.finance.gBanque.prefinancement.dtos.response.PrefinancementResponseDTO;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.mapper.PrefinancementMapper;
import staport.finance.gBanque.prefinancement.repository.PrefinancementRepository;
import staport.finance.gBanque.prefinancement.spec.PrefinancementSpecification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

/**
 * Implémentation du service Préfinancement.
 *
 * <p>Encapsulation des domaines (architecture.md) : la résolution du {@link Marche} passe par
 * {@link IMarcheService}, la résolution de la nature de ligne fonctionnelle "PREFINANCEMENT" par
 * {@link INatureLigneFonctionnelleService} — jamais par injection directe d'un {@code Repository}
 * étranger. {@code LigneFonctionnelleReader}/{@code LigneFonctionnelleSpecifiqueReader} restent
 * inchangés (déjà des façades de lecture cross-domaine conformes).
 *
 * <p>Pas de risque de cycle de beans : {@code ILigneFonctionnelleService}/
 * {@code ILigneFonctionnelleSpecifiqueService} (injectés par {@code NatureLigneFonctionnelleService})
 * n'injectent que {@link PrefinancementReader} (bean distinct), jamais {@link IPrefinancementService}.
 *
 * <p>Anti-N+1 : {@code PrefinancementRepository.findAll} porte l'{@code @EntityGraph}
 * {@code "Prefinancement.avecAssociations"} (marché + banque du marché).
 *
 * <p>Les totaux d'encours (déblocage/remboursement) sont calculés via une requête {@code SUM}
 * construite avec le même {@link Specification} que la recherche paginée (pas de chargement de
 * la liste complète en mémoire pour sommer côté Java — conventions/performance.md).
 * {@code endOfMonth} est recalculé à chaque appel (et non figé à la construction du bean, ancien
 * bug corrigé — cf. modules_sweep).
 */
@Service
public class PrefinancementServiceImpl implements IPrefinancementService {

    private static final Logger log = LoggerFactory.getLogger(PrefinancementServiceImpl.class);
    private static final String PREFINANCEMENT_NATURE_CODE = "PREFINANCEMENT";

    private final PrefinancementRepository prefinancementRepository;
    private final IMarcheService marcheService;
    private final PrefinancementMapper prefinancementMapper;
    private final LigneFonctionnelleSpecifiqueReader ligneFonctionnelleSpecifiqueReader;
    private final INatureLigneFonctionnelleService natureLigneFonctionnelleService;
    private final LigneFonctionnelleReader ligneFonctionnelleReader;

    @PersistenceContext
    private EntityManager entityManager;

    public PrefinancementServiceImpl(
            PrefinancementRepository prefinancementRepository,
            IMarcheService marcheService,
            PrefinancementMapper prefinancementMapper,
            LigneFonctionnelleSpecifiqueReader ligneFonctionnelleSpecifiqueReader,
            INatureLigneFonctionnelleService natureLigneFonctionnelleService,
            LigneFonctionnelleReader ligneFonctionnelleReader) {
        this.prefinancementRepository = prefinancementRepository;
        this.marcheService = marcheService;
        this.prefinancementMapper = prefinancementMapper;
        this.ligneFonctionnelleSpecifiqueReader = ligneFonctionnelleSpecifiqueReader;
        this.natureLigneFonctionnelleService = natureLigneFonctionnelleService;
        this.ligneFonctionnelleReader = ligneFonctionnelleReader;
        log.info("PrefinancementServiceImpl initialisé avec succès");
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = Prefinancement.class)
    public PrefinancementResponseDTO createPrefinancement(PrefinancementRequestDTO requestDTO) throws Exception {
        log.info("Début de création d'un préfinancement pour le marché ID: {}", requestDTO.marcheId());

        Marche marche = marcheService.findOrThrow(requestDTO.marcheId());
        log.debug("Marché trouvé - Nom: {}, Numéro: {}", marche.getNom(), marche.getNumero());

        NatureLigneFonctionnelle prefiNature = getPrefinancementNature();
        LigneResult ligneResult = findActivePrefinancementLine(marche, prefiNature);

        Prefinancement prefinancement = prefinancementMapper.toEntity(requestDTO, marche);
        prefinancement.setLigneFonctionnelleSpecifique(ligneResult.specifique());
        prefinancement.setLigneFonctionnelle(ligneResult.generale());

        prefinancement = prefinancementRepository.save(prefinancement);
        log.info("Préfinancement créé avec succès - ID: {}", prefinancement.getId());

        return prefinancementMapper.toDto(prefinancement);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Prefinancement.class)
    public PrefinancementResponseDTO updatePrefinancement(Long id, PrefinancementRequestDTO requestDTO) throws Exception {
        log.info("Début de mise à jour du préfinancement ID: {}", id);

        Prefinancement existingPrefinancement = prefinancementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Préfinancement non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Prefinancement not found with ID: " + id);
                });

        Marche marche = marcheService.findOrThrow(requestDTO.marcheId());
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
        existingPrefinancement.setDateEmission(requestDTO.dateEmission());
        existingPrefinancement.setMontant(requestDTO.montant());
        existingPrefinancement.setType(requestDTO.type());
        existingPrefinancement.setObservation(marche.getNumero());

        Prefinancement updatedPrefinancement = prefinancementRepository.save(existingPrefinancement);
        log.info("Préfinancement mis à jour avec succès - ID: {}", updatedPrefinancement.getId());

        return prefinancementMapper.toDto(updatedPrefinancement);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = Prefinancement.class)
    public void deletePrefinancement(Long id) {
        log.info("Début de suppression du préfinancement ID: {}", id);

        if (!prefinancementRepository.existsById(id)) {
            log.error("Impossible de supprimer - Préfinancement non trouvé avec l'ID: {}", id);
            throw new ResourceNotFoundException("Prefinancement not found with ID: " + id);
        }

        prefinancementRepository.deleteById(id);
        log.info("Préfinancement supprimé avec succès - ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public PrefinancementResponseDTO findById(Long id) {
        Prefinancement prefinancement = prefinancementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Préfinancement non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Prefinancement not found with ID: " + id);
                });

        return prefinancementMapper.toDto(prefinancement);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PrefinancementResponseDTO> findAll(PrefinancementSearchDTO searchDTO, Pageable pageable) {
        log.info("Recherche de préfinancement avec critères - Marché ID: {}, Banque ID: {}, Type: {}, Statut: {}",
                searchDTO.marcheId(), searchDTO.banqueId(), searchDTO.type(), searchDTO.status());

        Specification<Prefinancement> spec = PrefinancementSpecification.searchByCriteria(searchDTO);

        Page<Prefinancement> prefinancementPage = prefinancementRepository.findAll(spec,
                pageable.getSort().isSorted() ? pageable
                        : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.info("Recherche terminée - {} enregistrement(s) trouvé(s) sur {} page(s)",
                prefinancementPage.getTotalElements(), prefinancementPage.getTotalPages());

        return prefinancementPage.map(prefinancementMapper::toDto);
    }

    @Override
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

        return prefinancementMapper.toDto(updatedPrefinancement);
    }

    @Override
    @Transactional(readOnly = true)
    public EncoursPrefinancementDto getRemboursementEncoursForBanque(EncoursPrefinancementSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Recherche des encours de remboursement pour la banque ID: {}", banqueId);

        LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        Specification<Prefinancement> spec = PrefinancementSpecification.byBanqueId(banqueId)
                .and(PrefinancementSpecification.outstandingRemboursementUntil(endOfMonth))
                .and(PrefinancementSpecification.byMarcheName(searchDto.marcheName()))
                .and(PrefinancementSpecification.byMarcheNumero(searchDto.marcheNumero()))
                .and(PrefinancementSpecification.hasMontant(searchDto.montant()));

        Page<Prefinancement> prefinancementPage = prefinancementRepository.findAll(spec,
                pageable.getSort().isSorted() ? pageable
                        : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        BigDecimal totalDesEncours = sumMontant(spec);
        log.info("Total des encours de remboursement calculé: {} pour la banque ID: {}", totalDesEncours, banqueId);

        Page<PrefinancementResponseDTO> pageDto = prefinancementPage.map(prefinancementMapper::toDto);

        return new EncoursPrefinancementDto(totalDesEncours, pageDto);
    }

    @Override
    @Transactional(readOnly = true)
    public EncoursPrefinancementDto getDeblocageEncoursForBanque(EncoursPrefinancementSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Recherche des encours de déblocage pour la banque ID: {}", banqueId);

        LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        Specification<Prefinancement> spec = PrefinancementSpecification.byBanqueId(banqueId)
                .and(PrefinancementSpecification.outstandingDeblocageUntil(endOfMonth))
                .and(PrefinancementSpecification.byMarcheName(searchDto.marcheName()))
                .and(PrefinancementSpecification.byMarcheNumero(searchDto.marcheNumero()))
                .and(PrefinancementSpecification.hasMontant(searchDto.montant()));

        Page<Prefinancement> prefinancementPage = prefinancementRepository.findAll(spec,
                pageable.getSort().isSorted() ? pageable
                        : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        BigDecimal totalDesEncours = sumMontant(spec);
        log.info("Total des encours de déblocage calculé: {} pour la banque ID: {}", totalDesEncours, banqueId);

        Page<PrefinancementResponseDTO> pageDto = prefinancementPage.map(prefinancementMapper::toDto);

        return new EncoursPrefinancementDto(totalDesEncours, pageDto);
    }

    /**
     * Somme des montants correspondant à la {@link Specification} donnée, calculée en base
     * (agrégat SQL) — jamais en chargeant les entités en mémoire pour sommer côté Java
     * (conventions/performance.md).
     */
    private BigDecimal sumMontant(Specification<Prefinancement> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> query = cb.createQuery(BigDecimal.class);
        Root<Prefinancement> root = query.from(Prefinancement.class);
        query.select(cb.coalesce(cb.sum(root.get("montant")), BigDecimal.ZERO));
        var predicate = spec.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }
        return entityManager.createQuery(query).getSingleResult();
    }

    private NatureLigneFonctionnelle getPrefinancementNature() {
        log.debug("Recherche de la nature de ligne fonctionnelle Préfinancement (code={})", PREFINANCEMENT_NATURE_CODE);
        return natureLigneFonctionnelleService.findByCode(PREFINANCEMENT_NATURE_CODE)
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

        Optional<LigneFonctionnelleSpecifique> ligneSpecifique =
                ligneFonctionnelleSpecifiqueReader.findByMarcheAndNatureLigneFonctionnelleAndActifTrue(marche, prefiNature);
        if (ligneSpecifique.isPresent()) {
            log.info("Ligne spécifique Préfinancement active trouvée pour le marché '{}' (ID: {})",
                    marche.getNom(), ligneSpecifique.get().getId());
            return new LigneResult(ligneSpecifique.get(), null);
        }

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
