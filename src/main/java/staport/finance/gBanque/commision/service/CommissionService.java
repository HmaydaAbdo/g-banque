package staport.finance.gBanque.commision.service;


import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.factory.annotation.Value;
import staport.finance.gBanque.commision.dtos.CommissionFusionRequest;
import staport.finance.gBanque.commision.dtos.CommissionFusionResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.commision.dtos.*;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.commision.mapper.CommissionMapper;
import staport.finance.gBanque.commision.repository.CommisionRepository;
import staport.finance.gBanque.commision.specs.CommissionSpecifications;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.impot_taxes.repositories.ImpotEtTaxeRepository;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;


@Service
public class CommissionService {

    private static final Logger log = LogManager.getLogger(CommissionService.class);
    /** Taux TVA légal applicable aux commissions sur cautions bancaires (10%). */
    private static final BigDecimal TAUX_TVA_COMMISSION_CAUTION = new BigDecimal("0.10");

    @Value("${commission.virement.bmci.montant-fixe}")
    private BigDecimal montantCommissionBmci;

    @PersistenceContext
    private EntityManager entityManager;


    private final CommisionRepository    commissionRepository;
    private final BanqueRepository       banqueRepository;
    private final CommissionMapper       commissionMapper;
    private final ImpotEtTaxeRepository  impotEtTaxeRepository;

    LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

    public CommissionService(CommisionRepository commissionRepository,
                             BanqueRepository banqueRepository,
                             CommissionMapper commissionMapper,
                             ImpotEtTaxeRepository impotEtTaxeRepository) {
        this.commissionRepository  = commissionRepository;
        this.banqueRepository      = banqueRepository;
        this.commissionMapper      = commissionMapper;
        this.impotEtTaxeRepository = impotEtTaxeRepository;
    }

    // --- Create ---
    @Transactional
    public CommissionResponseDto createCommission(CommissionRequestDto requestDto) {
        log.info("Creating commission for banqueId={} libelle={}", requestDto.getBanqueId(), requestDto.getLibelle());

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque not found with id={}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque not found");
                });

        Commission commission = commissionMapper.toEntity(requestDto, banque);
        commissionRepository.save(commission);

        log.info("Commission created with id={}", commission.getId());
        return commissionMapper.toResponseDto(commission);
    }

    // --- Get by ID ---
    @Transactional(readOnly = true)
    public CommissionResponseDto getById(Long id) {
        log.info("Fetching commission with id={}", id);

        Commission commission = commissionRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Commission not found with id={}", id);
                    return new IllegalArgumentException("Commission not found");
                });

        return commissionMapper.toResponseDto(commission);
    }

    // --- Search with pagination + aggregate total ---
    @Transactional(readOnly = true)
    public EncoursCommissionDto searchCommissions(CommissionSearchDto searchDto, Pageable pageable) {
        log.info("Searching commissions with criteria: {}", searchDto);

        Specification<Commission> spec = CommissionSpecifications.buildSpecification(searchDto);

        Pageable stablePageable = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));

        Page<CommissionResponseDto> page = commissionRepository
                .findAll(spec, stablePageable)
                .map(commissionMapper::toResponseDto);

        BigDecimal total = sumMontant(spec);

        EncoursCommissionDto result = new EncoursCommissionDto();
        result.setPage(page);
        result.setTotalDesEncours(total);
        return result;
    }

    // --- Update ---
    @Transactional
    public CommissionResponseDto updateCommission(Long id, CommissionRequestDto requestDto) {
        log.info("Updating commission id={} with libelle={}", id, requestDto.getLibelle());

        Commission commission = commissionRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Commission not found with id={}", id);
                    return new IllegalArgumentException("Commission not found");
                });

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque not found with id={}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque not found");
                });

        commission.setLibelle(requestDto.getLibelle());
        commission.setMontant(requestDto.getMontant());
        commission.setBanque(banque);
        commission.setOperation(requestDto.getOperation());
        commission.setDateEmission(requestDto.getDateEmission());

        commissionRepository.save(commission);
        log.info("Commission updated successfully id={}", commission.getId());

        // Cascade TVA : uniquement pour les commissions sur cautions
        if (TransactionSource.CAUTION.equals(commission.getOperation())) {
            impotEtTaxeRepository.findByCommission_Id(id).ifPresent(tva -> {
                BigDecimal nouveauMontantTva = requestDto.getMontant()
                        .multiply(TAUX_TVA_COMMISSION_CAUTION)
                        .setScale(2, RoundingMode.HALF_UP);
                log.info("Mise à jour TVA liée — commission id={}, ancien montant TVA={}, nouveau montant TVA={}",
                        id, tva.getMontant(), nouveauMontantTva);
                tva.setMontant(nouveauMontantTva);
                impotEtTaxeRepository.save(tva);
            });
        }

        return commissionMapper.toResponseDto(commission);
    }


    // --- Delete ---
    @Transactional
    public void deleteCommission(Long id) {
        log.info("Deleting commission with id={}", id);

        Commission commission = commissionRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Commission not found with id={}", id);
                    return new IllegalArgumentException("Commission not found");
                });

        // Cascade TVA : uniquement pour les commissions sur cautions
        if (TransactionSource.CAUTION.equals(commission.getOperation())) {
            impotEtTaxeRepository.findByCommission_Id(id).ifPresent(tva -> {
                log.info("Suppression TVA liée — commission id={}, montant TVA={}",
                        id, tva.getMontant());
                impotEtTaxeRepository.delete(tva);
            });
        }

        commissionRepository.delete(commission);
        log.info("Commission deleted successfully id={}", id);
    }
    /**
     * Retrieves a paginated list of outstanding commissions and calculates their total amount.
     * Outstanding commissions are those with a 'PREVU' status and an emission date in the past.
     *
     * @param pageable Pagination and sorting information.
     * @param searchDto Optional search criteria (e.g., for filtering by bank, libelle, etc.).
     * @return An EncoursCommissionDto containing the page of commissions and the total outstanding amount.
     */
    @Transactional(readOnly = true)
    public EncoursCommissionDto getOutstandingCommissions(CommissionSearchDto searchDto, Pageable pageable) {
        // Build the specification to filter the records
        Specification<Commission> spec = Specification.where(CommissionSpecifications.outstandingUntil(endOfMonth))
                .and(CommissionSpecifications.hasBanque(searchDto.getBanqueId()))
                .and(CommissionSpecifications.hasLibelle(searchDto.getLibelle()))
                .and(CommissionSpecifications.hasOperation(searchDto.getOperation()))
                .and(CommissionSpecifications.hasMontant(searchDto.getMontant()));

        // Garantir un tri stable par id pour éviter les doublons inter-pages
        Pageable stablePageable = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));

        Page<CommissionResponseDto> pageDto = commissionRepository
                .findAll(spec, stablePageable)
                .map(commissionMapper::toResponseDto);

        BigDecimal totalDesEncours = sumMontant(spec);

        EncoursCommissionDto result = new EncoursCommissionDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        return result;
    }


    /**
     * Supprime toutes les commissions liées à un contrat de leasing.
     * Utilise le numéro de contrat pour identifier les commissions via leur libellé.
     *
     * @param numeroContrat Le numéro du contrat de leasing
     */
    @Transactional
    public void deleteCommissionsByContratLeasing(String numeroContrat) {
        log.info("Début de la suppression des commissions pour le contrat N° : {}", numeroContrat);

        if (numeroContrat == null || numeroContrat.trim().isEmpty()) {
            log.warn("Tentative de suppression de commissions avec un numéro de contrat vide");
            return;
        }

        // Pattern de recherche : toutes les commissions dont le libellé contient le numéro du contrat
        String libellePattern = "Commission Prelevement Leasing contrat N°" + numeroContrat + "%";

        log.debug("Pattern de recherche : {}", libellePattern);

        // Rechercher toutes les commissions correspondantes (sans filtre de date)
        List<Commission> commissions = commissionRepository.findByLibelleLike(libellePattern);

        if (commissions.isEmpty()) {
            log.info("Aucune commission trouvée pour le contrat N° : {}", numeroContrat);
            return;
        }

        int nombreCommissions = commissions.size();
        log.debug("Nombre de commissions trouvées : {}", nombreCommissions);

        // Supprimer toutes les commissions
        commissionRepository.deleteAll(commissions);

        log.info("Suppression terminée : {} commission(s) supprimée(s) pour le contrat N° : {}",
                nombreCommissions, numeroContrat);

    }

    // ── Fusion ────────────────────────────────────────────────────────────────
    /**
     * Fusionne plusieurs commissions sur cautions en une seule.
     *
     * <p>Contraintes validées :
     * <ul>
     *   <li>Minimum 2 IDs</li>
     *   <li>Toutes {@code operation = CAUTION}, {@code status = PREVU}</li>
     *   <li>Même banque et même {@code dateEmission} pour toutes</li>
     * </ul>
     *
     * <p>Opérations atomiques (tout ou rien) :
     * <ol>
     *   <li>Suppression des TVA liées aux commissions sources (si {@code genererTva = true})</li>
     *   <li>Suppression des commissions sources</li>
     *   <li>Création de la commission fusionnée</li>
     *   <li>Création de la TVA fusionnée (10 %) si {@code genererTva = true}</li>
     * </ol>
     */
    @Transactional
    public CommissionFusionResponse fusionner(CommissionFusionRequest request) {
        List<Long> ids = request.getIds();
        log.info("Fusion demandée — {} commission(s), libellé='{}'", ids.size(), request.getLibelle());

        // ── 1. Chargement ────────────────────────────────────────────────────
        List<Commission> sources = commissionRepository.findAllById(ids);

        if (sources.size() != ids.size()) {
            List<Long> trouvees  = sources.stream().map(Commission::getId).toList();
            List<Long> manquantes = ids.stream().filter(id -> !trouvees.contains(id)).toList();
            throw new IllegalArgumentException("Commission(s) introuvable(s) : " + manquantes);
        }

        // ── 2. Validations métier ────────────────────────────────────────────
        for (Commission c : sources) {
            if (!TransactionSource.CAUTION.equals(c.getOperation())) {
                throw new IllegalArgumentException(
                        "La commission id=" + c.getId() + " n'est pas de type CAUTION.");
            }
            if (!Status.PREVU.equals(c.getStatus())) {
                throw new IllegalArgumentException(
                        "La commission id=" + c.getId() + " est déjà réglée (PAYE) — fusion impossible.");
            }
        }

        // Même banque
        long nbBanques = sources.stream().map(c -> c.getBanque().getId()).distinct().count();
        if (nbBanques > 1) {
            throw new IllegalArgumentException(
                    "Toutes les commissions doivent appartenir à la même banque.");
        }

        // Fusion interdite pour BMCI (TVA séparée — la fusion briserait la cohérence comptable)
        String nomBanque = sources.get(0).getBanque().getNom();
        if (nomBanque != null && nomBanque.toUpperCase().contains("BMCI")) {
            throw new IllegalArgumentException(
                    "La fusion n'est pas autorisée pour les commissions BMCI (TVA séparée).");
        }



        Banque banque          = sources.get(0).getBanque();
        LocalDate dateEmission = sources.get(0).getDateEmission();

        // ── 3. Calcul du montant fusionné (TTC = somme HT × 1,10) ────────────
        BigDecimal montantHt = sources.stream()
                .map(Commission::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal montantFusionne = montantHt
                .multiply(BigDecimal.ONE.add(TAUX_TVA_COMMISSION_CAUTION))
                .setScale(2, RoundingMode.HALF_UP);

        // ── 4. Suppression des TVA liées aux sources (toujours) ──────────────
        impotEtTaxeRepository.deleteByCommissionIdIn(ids);
        log.info("TVA sources supprimées pour les commissions {}", ids);

        // ── 5. Suppression des commissions sources ───────────────────────────
        commissionRepository.deleteAllById(ids);
        log.info("Commissions sources supprimées : {}", ids);

        // ── 6. Création de la commission fusionnée ────────────────────────────
        Commission fusionnee = new Commission();
        fusionnee.setLibelle(request.getLibelle());
        fusionnee.setMontant(montantFusionne);
        fusionnee.setBanque(banque);
        fusionnee.setOperation(TransactionSource.CAUTION);
        fusionnee.setDateEmission(dateEmission);
        fusionnee.setStatus(Status.PREVU);
        fusionnee = commissionRepository.save(fusionnee);
        log.info("Commission fusionnée créée — id={}, montant={}", fusionnee.getId(), montantFusionne);

        // Aucune TVA créée après fusion
        return new CommissionFusionResponse(commissionMapper.toResponseDto(fusionnee));
    }

    // ── Aggregate helper ──────────────────────────────────────────────────────
    /**
     * Émet un seul {@code SELECT SUM(c.montant) FROM Commission c WHERE ...}
     * sans charger les entités en mémoire.
     */
    private BigDecimal sumMontant(Specification<Commission> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> cq = cb.createQuery(BigDecimal.class);
        Root<Commission> root = cq.from(Commission.class);
        cq.select(cb.sum(root.get("montant")));
        Predicate predicate = spec.toPredicate(root, cq, cb);
        if (predicate != null) {
            cq.where(predicate);
        }
        BigDecimal result = entityManager.createQuery(cq).getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }

    @Transactional
    public void toggleCommisionStatus(Long commissionId) {

        Commission commission = commissionRepository.findById(commissionId)
                .orElseThrow(() -> {
                    log.error("Commission avec l'id {} introuvable", commissionId);
                    return new ResourceNotFoundException(
                            "Commission avec l'id " + commissionId + " introuvable"
                    );
                });

        log.info("Commission avec l'id {} trouvée. Statut actuel : {}", commissionId, commission.getStatus());
        commission.setStatus(commission.getStatus() == Status.PAYE ? Status.PREVU : Status.PAYE);

        log.info("Statut de la commission avec l'id {} mis à jour. Nouveau statut : {}", commissionId, commission.getStatus());

        commissionRepository.save(commission);
    }


    @Transactional
    public void generateCommissionIfNeeded(Virement virement) {

        if (!isBmci(virement)) {
            return;
        }

        if (virement.getStatut() != StatutMoyenPaiement.ENCOURS) {
            return;
        }

        String libelle = buildLibelle(virement);

        if (commissionRepository.existsByOperationAndLibelle(
                TransactionSource.Virement,
                libelle)) {

            log.debug("Commission already exists for virement {}", virement.getNumero());
            return;
        }

        Commission commission = new Commission();

        commission.setMontant(montantCommissionBmci);
        commission.setBanque(virement.getBanqueEmettrice());
        commission.setOperation(TransactionSource.Virement);
        commission.setDateEmission(virement.getDateEmission());
        commission.setLibelle(libelle);
        commission.setStatus(Status.PREVU);

        commissionRepository.save(commission);

        log.info("Commission created for BMCI virement {}", virement.getNumero());
    }

    private String buildLibelle(Virement virement) {
        return "Commission virement N° " + virement.getNumero();
    }

    private boolean isBmci(Virement virement) {
        return virement.getBanqueEmettrice() != null
                && virement.getBanqueEmettrice().getNom() != null
                && "BMCI".equalsIgnoreCase(
                virement.getBanqueEmettrice().getNom().trim()
        );
    }

}
