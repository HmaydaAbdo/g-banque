package staport.finance.gBanque.impot_taxes.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.cautions.entities.Caution;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.commision.service.ICommissionService;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.impot_taxes.dtos.request.EncoursImpotEtTaxeSearchDto;
import staport.finance.gBanque.impot_taxes.dtos.request.ImpotEtTaxeFusionRequest;
import staport.finance.gBanque.impot_taxes.dtos.request.ImpotEtTaxeRequestDto;
import staport.finance.gBanque.impot_taxes.dtos.request.ImpotEtTaxeSearchDto;
import staport.finance.gBanque.impot_taxes.dtos.response.EncoursImpotEtTaxeDto;
import staport.finance.gBanque.impot_taxes.dtos.response.ImpotEtTaxeResponseDto;
import staport.finance.gBanque.impot_taxes.entities.ImpotEtTaxe;
import staport.finance.gBanque.impot_taxes.enums.Motif;
import staport.finance.gBanque.impot_taxes.mapper.ImpotEtTaxeMapper;
import staport.finance.gBanque.impot_taxes.repositories.ImpotEtTaxeRepository;
import staport.finance.gBanque.impot_taxes.specs.ImpotEtTaxeSpecifications;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Implémentation du service Impôts et Taxes.
 *
 * <p>Encapsulation des domaines (architecture.md) : la résolution de la {@link Banque} passe
 * exclusivement par {@link IBanqueService} — jamais par injection directe de
 * {@code BanqueRepository}. La génération automatique de commission (règle SAHAM BANK) passe par
 * {@link ICommissionService}, injecté en {@code @Lazy} pour casser le cycle de bean bidirectionnel
 * avec {@code CommissionService} (qui injecte {@link IImpotEtTaxeService} en retour) — voir
 * {@code project_gbanque_dette_repo_hors_domaine}.
 *
 * <p>Anti-N+1 : {@code ImpotEtTaxeRepository.findAll} porte l'{@code @EntityGraph}
 * {@code "ImpotEtTaxe.avecBanque"}. Le total d'encours est calculé via une requête {@code SUM}
 * réutilisant le même {@link Specification} que la recherche paginée (performance.md).
 */
@Service
public class ImpotEtTaxeServiceImpl implements IImpotEtTaxeService {

    private static final Logger log = LoggerFactory.getLogger(ImpotEtTaxeServiceImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Nom de banque déclenchant la commission automatique sur impôt/taxe (règle métier SAHAM BANK).
     * Comparaison insensible à la casse — voir {@link #genereCommissionAutomatique(Banque)}.
     */
    private static final String NOM_BANQUE_SAHAM = "SAHAM BANK";

    /** Montant fixe de la commission générée automatiquement sur chaque impôt/taxe SAHAM BANK. */
    private static final BigDecimal MONTANT_COMMISSION_IMPOT_TAXE = new BigDecimal("5.50");

    private final ImpotEtTaxeRepository impotEtTaxeRepository;
    private final IBanqueService banqueService;
    private final ImpotEtTaxeMapper impotEtTaxeMapper;
    /** @Lazy : casse le cycle de bean avec CommissionService, qui injecte IImpotEtTaxeService en retour. */
    private final ICommissionService commissionService;

    public ImpotEtTaxeServiceImpl(ImpotEtTaxeRepository impotEtTaxeRepository,
                              IBanqueService banqueService,
                              ImpotEtTaxeMapper impotEtTaxeMapper,
                              @Lazy ICommissionService commissionService) {
        this.impotEtTaxeRepository = impotEtTaxeRepository;
        this.banqueService = banqueService;
        this.impotEtTaxeMapper = impotEtTaxeMapper;
        this.commissionService = commissionService;
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = ImpotEtTaxe.class)
    public ImpotEtTaxeResponseDto createImpotEtTaxe(ImpotEtTaxeRequestDto requestDto) {
        log.info("Début de création d'un impôt/taxe — banqueId={}, motif={}", requestDto.banqueId(), requestDto.motif());

        Banque banque = banqueService.findOrThrow(requestDto.banqueId());

        ImpotEtTaxe impotEtTaxe = impotEtTaxeMapper.toEntity(requestDto, banque);
        impotEtTaxeRepository.save(impotEtTaxe);

        log.info("Impôt/taxe créé avec succès: id={}", impotEtTaxe.getId());

        // Règle métier SAHAM BANK : tout impôt/taxe émis par SAHAM BANK génère automatiquement
        // une commission fixe, quel que soit le motif.
        if (genereCommissionAutomatique(banque)) {
            genererCommissionSurImpotEtTaxe(impotEtTaxe);
        }

        return impotEtTaxeMapper.toDto(impotEtTaxe);
    }

    /**
     * Indique si la banque déclenche la commission automatique sur impôt/taxe (SAHAM BANK).
     */
    private boolean genereCommissionAutomatique(Banque banque) {
        return banque.getNom() != null
                && banque.getNom().trim().equalsIgnoreCase(NOM_BANQUE_SAHAM);
    }

    /**
     * Génère et persiste la commission liée à l'impôt/taxe SAHAM BANK nouvellement créé.
     *
     * <p>Règle métier : {@code dateEmission} de la commission = {@code dateEmission} de l'impôt/taxe.
     * Le libellé et l'observation reprennent le motif de l'impôt/taxe pour la traçabilité.
     * Déclenchée uniquement à la création (pas de synchronisation lors d'une modification ou
     * suppression ultérieure de l'impôt/taxe).
     */
    private void genererCommissionSurImpotEtTaxe(ImpotEtTaxe impotEtTaxe) {
        String suffixeMotif = impotEtTaxe.getMotif() != null ? impotEtTaxe.getMotif().getMotif() : "";
        String libelleObservation = "Commission sur impôt et taxe — " + suffixeMotif;

        commissionService.creerCommissionSurImpotEtTaxe(impotEtTaxe.getId(), impotEtTaxe.getBanque().getId(),
                MONTANT_COMMISSION_IMPOT_TAXE, impotEtTaxe.getDateEmission(), libelleObservation, libelleObservation);

        log.info("Commission auto-générée sur impôt/taxe SAHAM BANK — impotEtTaxe id={}", impotEtTaxe.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public ImpotEtTaxeResponseDto getById(Long id) {
        log.debug("Récupération de l'impôt/taxe id={}", id);
        ImpotEtTaxe impotEtTaxe = impotEtTaxeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Impôt/taxe introuvable avec l'id: " + id));
        return impotEtTaxeMapper.toDto(impotEtTaxe);
    }

    @Override
    @Transactional(readOnly = true)
    public EncoursImpotEtTaxeDto searchImpotEtTaxes(ImpotEtTaxeSearchDto searchDto, Pageable pageable) {
        log.info("Recherche d'impôts/taxes — banqueId={}, motif={}, statut={}",
                searchDto.banqueId(), searchDto.motif(), searchDto.status());

        Specification<ImpotEtTaxe> spec = ImpotEtTaxeSpecifications.searchByCriteria(searchDto);

        Page<ImpotEtTaxeResponseDto> page = impotEtTaxeRepository
                .findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")))
                .map(impotEtTaxeMapper::toDto);

        BigDecimal total = sumMontant(spec);

        log.info("Recherche terminée: {} élément(s) trouvé(s)", page.getTotalElements());

        return new EncoursImpotEtTaxeDto(total, page);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = ImpotEtTaxe.class)
    public ImpotEtTaxeResponseDto updateImpotEtTaxe(Long id, ImpotEtTaxeRequestDto requestDto) {
        log.info("Mise à jour de l'impôt/taxe id={}", id);

        ImpotEtTaxe impotEtTaxe = impotEtTaxeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Impôt/taxe introuvable avec l'id: " + id));

        if (impotEtTaxe.getStatus() == Status.PAYE) {
            log.warn("Tentative de modification d'un impôt/taxe déjà payé — id: {}", id);
            throw new ValidationException("Modification interdite : l'impôt/taxe id " + id + " est déjà payé.");
        }

        Banque banque = banqueService.findOrThrow(requestDto.banqueId());

        impotEtTaxe.setLibelle(requestDto.libelle());
        impotEtTaxe.setMontant(requestDto.montant());
        impotEtTaxe.setBanque(banque);
        impotEtTaxe.setMotif(requestDto.motif());
        impotEtTaxe.setDateEmission(requestDto.dateEmission());
        impotEtTaxe.setObservation(requestDto.observation());

        impotEtTaxeRepository.save(impotEtTaxe);
        log.info("Impôt/taxe mis à jour avec succès: id={}", id);
        return impotEtTaxeMapper.toDto(impotEtTaxe);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = ImpotEtTaxe.class)
    public void deleteImpotEtTaxe(Long id) {
        log.info("Suppression de l'impôt/taxe id={}", id);
        ImpotEtTaxe impotEtTaxe = impotEtTaxeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Impôt/taxe introuvable avec l'id: " + id));

        if (impotEtTaxe.getStatus() == Status.PAYE) {
            log.warn("Tentative de suppression d'un impôt/taxe déjà payé — id: {}", id);
            throw new ValidationException("Suppression interdite : l'impôt/taxe id " + id + " est déjà payé.");
        }

        impotEtTaxeRepository.deleteById(id);
        log.info("Impôt/taxe supprimé avec succès: id={}", id);
    }

    // ── Aggregate helper ──────────────────────────────────────────────────────
    /**
     * Émet un seul {@code SELECT SUM(i.montant) FROM ImpotEtTaxe i WHERE ...}
     * sans charger les entités en mémoire.
     */
    private BigDecimal sumMontant(Specification<ImpotEtTaxe> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> cq = cb.createQuery(BigDecimal.class);
        Root<ImpotEtTaxe> root = cq.from(ImpotEtTaxe.class);
        cq.select(cb.coalesce(cb.sum(root.get("montant")), BigDecimal.ZERO));
        Predicate predicate = spec.toPredicate(root, cq, cb);
        if (predicate != null) {
            cq.where(predicate);
        }
        return entityManager.createQuery(cq).getSingleResult();
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = ImpotEtTaxe.class)
    public ImpotEtTaxeResponseDto toggleStatus(Long id) {
        log.info("Basculement du statut de l'impôt/taxe id={}", id);

        ImpotEtTaxe impotEtTaxe = impotEtTaxeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Impôt/taxe introuvable avec l'id: " + id));

        Status oldStatus = impotEtTaxe.getStatus();
        Status newStatus = (oldStatus == Status.PAYE) ? Status.PREVU : Status.PAYE;

        impotEtTaxe.setStatus(newStatus);
        ImpotEtTaxe updated = impotEtTaxeRepository.save(impotEtTaxe);

        log.info("Statut basculé pour id={}: {} -> {}", id, oldStatus, newStatus);
        return impotEtTaxeMapper.toDto(updated);
    }

    // ── Fusion ────────────────────────────────────────────────────────────────
    /**
     * Fusionne plusieurs TVA (impôts/taxes) en une seule.
     *
     * <p>Contraintes validées :
     * <ul>
     *   <li>Minimum 2 IDs</li>
     *   <li>Toutes {@code status = PREVU}</li>
     *   <li>Même banque, même {@code dateEmission} et même {@code motif}</li>
     * </ul>
     *
     * <p>Opérations atomiques :
     * <ol>
     *   <li>Suppression des TVA sources</li>
     *   <li>Création de la TVA fusionnée (somme des montants)</li>
     * </ol>
     */
    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = ImpotEtTaxe.class)
    public ImpotEtTaxeResponseDto fusionner(ImpotEtTaxeFusionRequest request) {
        List<Long> ids = request.ids();
        log.info("Fusion TVA demandée — {} entrée(s)", ids.size());

        // ── 1. Chargement ────────────────────────────────────────────────────
        List<ImpotEtTaxe> sources = impotEtTaxeRepository.findAllById(ids);

        if (sources.size() != ids.size()) {
            List<Long> trouvees = sources.stream().map(ImpotEtTaxe::getId).toList();
            List<Long> manquantes = ids.stream().filter(id -> !trouvees.contains(id)).toList();
            throw new ResourceNotFoundException("TVA introuvable(s) : " + manquantes);
        }

        // ── 2. Validations métier ────────────────────────────────────────────
        for (ImpotEtTaxe t : sources) {
            if (!Status.PREVU.equals(t.getStatus())) {
                throw new ValidationException(
                        "La TVA id=" + t.getId() + " est déjà réglée (PAYE) — fusion impossible.");
            }
        }

        // Même banque
        long nbBanques = sources.stream().map(t -> t.getBanque().getId()).distinct().count();
        if (nbBanques > 1) {
            throw new ValidationException("Toutes les TVA doivent appartenir à la même banque.");
        }

        // Même dateEmission
        long nbDates = sources.stream().map(ImpotEtTaxe::getDateEmission).distinct().count();
        if (nbDates > 1) {
            throw new ValidationException("Toutes les TVA doivent avoir la même date d'émission.");
        }

        // Même motif
        long nbMotifs = sources.stream().map(ImpotEtTaxe::getMotif).distinct().count();
        if (nbMotifs > 1) {
            throw new ValidationException("Toutes les TVA doivent avoir le même motif.");
        }

        Banque     banque        = sources.get(0).getBanque();
        LocalDate  dateEmission  = sources.get(0).getDateEmission();
        Motif      motif         = sources.get(0).getMotif();

        // ── 3. Calcul du montant fusionné ────────────────────────────────────
        BigDecimal montantFusionne = sources.stream()
                .map(ImpotEtTaxe::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ── 4. Suppression des TVA sources ───────────────────────────────────
        impotEtTaxeRepository.deleteAllById(ids);
        log.info("TVA sources supprimées : {}", ids);

        // ── 5. Création de la TVA fusionnée ──────────────────────────────────
        ImpotEtTaxe fusionnee = new ImpotEtTaxe();
        fusionnee.setLibelle(request.libelle());
        fusionnee.setObservation(request.libelle());
        fusionnee.setMontant(montantFusionne);
        fusionnee.setBanque(banque);
        fusionnee.setMotif(motif);
        fusionnee.setDateEmission(dateEmission);
        fusionnee.setStatus(Status.PREVU);
        // commission non liée : la TVA fusionnée est détachée des commissions sources
        fusionnee = impotEtTaxeRepository.save(fusionnee);

        log.info("TVA fusionnée créée — id={}", fusionnee.getId());
        return impotEtTaxeMapper.toDto(fusionnee);
    }

    @Override
    @Transactional(readOnly = true)
    public EncoursImpotEtTaxeDto getOutstandingImpotEtTaxes(EncoursImpotEtTaxeSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Récupération des encours impôts/taxes — banqueId={}", banqueId);

        LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        Specification<ImpotEtTaxe> spec = Specification.where(ImpotEtTaxeSpecifications.outstandingUntil(endOfMonth))
                .and(ImpotEtTaxeSpecifications.hasBanque(banqueId))
                .and(ImpotEtTaxeSpecifications.hasLibelle(searchDto.libelle()))
                .and(ImpotEtTaxeSpecifications.hasMotif(searchDto.motif()))
                .and(ImpotEtTaxeSpecifications.hasMontant(searchDto.montant()));

        Page<ImpotEtTaxeResponseDto> pageDto = impotEtTaxeRepository
                .findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")))
                .map(impotEtTaxeMapper::toDto);

        BigDecimal totalDesEncours = sumMontant(spec);

        log.info("Encours récupérés: {} élément(s), banqueId={}", pageDto.getTotalElements(), banqueId);
        return new EncoursImpotEtTaxeDto(totalDesEncours, pageDto);
    }

    // ── Façade cross-domaine (ex. cautions) ────────────────────────────────────

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = ImpotEtTaxe.class)
    public ImpotEtTaxeResponseDto creerTvaSurCommission(Long banqueId, BigDecimal montant, Motif motif,
            LocalDate dateEmission, String libelle, String observation,
            Long commissionId, Long cautionId) {

        Banque banque = banqueService.findOrThrow(banqueId);

        ImpotEtTaxe tva = new ImpotEtTaxe();
        tva.setBanque(banque);
        tva.setMontant(montant);
        tva.setMotif(motif);
        tva.setDateEmission(dateEmission);
        tva.setLibelle(libelle);
        tva.setObservation(observation);
        tva.setStatus(Status.PREVU);
        // Références par proxy — évite de charger les entités complètes pour ces seuls FK.
        tva.setCommission(entityManager.getReference(Commission.class, commissionId));
        if (cautionId != null) {
            tva.setCaution(entityManager.getReference(Caution.class, cautionId));
        }

        ImpotEtTaxe sauvegardee = impotEtTaxeRepository.save(tva);
        log.info("TVA créée sur commission externe — id={}, motif={}", sauvegardee.getId(), motif);

        return impotEtTaxeMapper.toDto(sauvegardee);
    }

    @Override
    @Transactional
    public boolean deleteByCommissionIdIfExists(Long commissionId) {
        return impotEtTaxeRepository.findByCommission_Id(commissionId)
                .map(tva -> {
                    impotEtTaxeRepository.delete(tva);
                    log.info("TVA supprimée — liée à la commission id={}", commissionId);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional
    public void updateMontantByCommissionId(Long commissionId, BigDecimal nouveauMontant) {
        impotEtTaxeRepository.findByCommission_Id(commissionId).ifPresent(tva -> {
            log.info("Mise à jour du montant TVA liée — commission id={}", commissionId);
            tva.setMontant(nouveauMontant);
            impotEtTaxeRepository.save(tva);
        });
    }

    @Override
    @Transactional
    public void deleteByCommissionIdIn(List<Long> commissionIds) {
        impotEtTaxeRepository.deleteByCommissionIdIn(commissionIds);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsImpotTaxeGenere(Long banqueId, String libelle, LocalDate debut, LocalDate fin) {
        return impotEtTaxeRepository.existsByBanqueIdAndLibelleAndDateEmissionBetween(banqueId, libelle, debut, fin);
    }
}
