package staport.finance.gBanque.impot_taxes.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.impot_taxes.dtos.*;
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

@Service
public class ImpotEtTaxeService {

    private static final Logger log = LogManager.getLogger(ImpotEtTaxeService.class);
    @PersistenceContext
    private EntityManager entityManager;

    private final ImpotEtTaxeRepository impotEtTaxeRepository;
    private final BanqueRepository banqueRepository;
    private final ImpotEtTaxeMapper impotEtTaxeMapper;
    LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

    public ImpotEtTaxeService(ImpotEtTaxeRepository impotEtTaxeRepository,
                              BanqueRepository banqueRepository,
                              ImpotEtTaxeMapper impotEtTaxeMapper) {
        this.impotEtTaxeRepository = impotEtTaxeRepository;
        this.banqueRepository = banqueRepository;
        this.impotEtTaxeMapper = impotEtTaxeMapper;
    }

    @Transactional
    public ImpotEtTaxeResponseDto createImpotEtTaxe(ImpotEtTaxeRequestDto requestDto) {
        log.info("Début de création d'un impôt/taxe avec les données: {}", requestDto);

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque introuvable avec l'id={}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque introuvable");
                });

        ImpotEtTaxe impotEtTaxe = impotEtTaxeMapper.toEntity(requestDto, banque);
        impotEtTaxeRepository.save(impotEtTaxe);

        log.info("Impôt/taxe créé avec succès: id={}", impotEtTaxe.getId());
        return impotEtTaxeMapper.toResponseDto(impotEtTaxe);
    }

    @Transactional(readOnly = true)
    public ImpotEtTaxeResponseDto getById(Long id) {
        log.info("Récupération de l'impôt/taxe id={}", id);
        ImpotEtTaxe impotEtTaxe = impotEtTaxeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Impôt/taxe introuvable"));
        return impotEtTaxeMapper.toResponseDto(impotEtTaxe);
    }

    @Transactional(readOnly = true)
    public EncoursImpotEtTaxeDto searchImpotEtTaxes(ImpotEtTaxeSearchDto searchDto, Pageable pageable) {
        log.info("Recherche d'impôts/taxes - Critères: {}", searchDto);

        Specification<ImpotEtTaxe> spec = ImpotEtTaxeSpecifications.buildSpecification(searchDto);

        Page<ImpotEtTaxeResponseDto> page = impotEtTaxeRepository
                .findAll(spec, pageable)
                .map(impotEtTaxeMapper::toResponseDto);

        BigDecimal total = sumMontant(spec);

        log.info("Recherche terminée: {} éléments trouvés, total={}", page.getTotalElements(), total);

        EncoursImpotEtTaxeDto result = new EncoursImpotEtTaxeDto();
        result.setPage(page);
        result.setTotalDesEncours(total);
        return result;
    }

    @Transactional
    public ImpotEtTaxeResponseDto updateImpotEtTaxe(Long id, ImpotEtTaxeRequestDto requestDto) {
        log.info("Mise à jour de l'impôt/taxe id={}", id);

        ImpotEtTaxe impotEtTaxe = impotEtTaxeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Impôt/taxe introuvable"));

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> new IllegalArgumentException("Banque introuvable"));

        impotEtTaxe.setLibelle(requestDto.getLibelle());
        impotEtTaxe.setMontant(requestDto.getMontant());
        impotEtTaxe.setBanque(banque);
        impotEtTaxe.setMotif(requestDto.getMotif());
        impotEtTaxe.setDateEmission(requestDto.getDateEmission());
        impotEtTaxe.setObservation(requestDto.getObservation());

        impotEtTaxeRepository.save(impotEtTaxe);
        log.info("Impôt/taxe mis à jour avec succès: id={}", id);
        return impotEtTaxeMapper.toResponseDto(impotEtTaxe);
    }

    @Transactional
    public void deleteImpotEtTaxe(Long id) {
        log.info("Suppression de l'impôt/taxe id={}", id);
        if (!impotEtTaxeRepository.existsById(id)) {
            throw new IllegalArgumentException("Impôt/taxe introuvable");
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
        cq.select(cb.sum(root.get("montant")));
        Predicate predicate = spec.toPredicate(root, cq, cb);
        if (predicate != null) {
            cq.where(predicate);
        }
        BigDecimal result = entityManager.createQuery(cq).getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }

    @Transactional
    public ImpotEtTaxeResponseDto toggleStatus(Long id) {
        log.info("Basculement du statut de l'impôt/taxe ID: {}", id);

        ImpotEtTaxe impotEtTaxe = impotEtTaxeRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Impôt/taxe introuvable avec l'id={}", id);
                    return new IllegalArgumentException("Impôt/taxe introuvable");
                });

        Status oldStatus = impotEtTaxe.getStatus();
        Status newStatus = (oldStatus == Status.PAYE) ? Status.PREVU : Status.PAYE;

        impotEtTaxe.setStatus(newStatus);
        ImpotEtTaxe updated = impotEtTaxeRepository.save(impotEtTaxe);

        log.info("Statut basculé pour ID {}: {} -> {}", id, oldStatus, newStatus);
        return impotEtTaxeMapper.toResponseDto(updated);
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
    @Transactional
    public ImpotEtTaxeResponseDto fusionner(ImpotEtTaxeFusionRequest request) {
        List<Long> ids = request.getIds();
        log.info("Fusion TVA demandée — {} entrée(s), libellé='{}'", ids.size(), request.getLibelle());

        // ── 1. Chargement ────────────────────────────────────────────────────
        List<ImpotEtTaxe> sources = impotEtTaxeRepository.findAllById(ids);

        if (sources.size() != ids.size()) {
            List<Long> trouvees = sources.stream().map(ImpotEtTaxe::getId).toList();
            List<Long> manquantes = ids.stream().filter(id -> !trouvees.contains(id)).toList();
            throw new IllegalArgumentException("TVA introuvable(s) : " + manquantes);
        }

        // ── 2. Validations métier ────────────────────────────────────────────
        for (ImpotEtTaxe t : sources) {
            if (!Status.PREVU.equals(t.getStatus())) {
                throw new IllegalArgumentException(
                        "La TVA id=" + t.getId() + " est déjà réglée (PAYE) — fusion impossible.");
            }
        }

        // Même banque
        long nbBanques = sources.stream().map(t -> t.getBanque().getId()).distinct().count();
        if (nbBanques > 1) {
            throw new IllegalArgumentException(
                    "Toutes les TVA doivent appartenir à la même banque.");
        }

        // Même dateEmission
        long nbDates = sources.stream().map(ImpotEtTaxe::getDateEmission).distinct().count();
        if (nbDates > 1) {
            throw new IllegalArgumentException(
                    "Toutes les TVA doivent avoir la même date d'émission.");
        }

        // Même motif
        long nbMotifs = sources.stream().map(ImpotEtTaxe::getMotif).distinct().count();
        if (nbMotifs > 1) {
            throw new IllegalArgumentException(
                    "Toutes les TVA doivent avoir le même motif.");
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
        fusionnee.setLibelle(request.getLibelle());
        fusionnee.setMontant(montantFusionne);
        fusionnee.setBanque(banque);
        fusionnee.setMotif(motif);
        fusionnee.setDateEmission(dateEmission);
        fusionnee.setStatus(Status.PREVU);
        // commission non liée : la TVA fusionnée est détachée des commissions sources
        fusionnee = impotEtTaxeRepository.save(fusionnee);

        log.info("TVA fusionnée créée — id={}, montant={}", fusionnee.getId(), montantFusionne);
        return impotEtTaxeMapper.toResponseDto(fusionnee);
    }

    @Transactional(readOnly = true)
    public EncoursImpotEtTaxeDto getOutstandingImpotEtTaxes(ImpotEtTaxeSearchDto searchDto, Pageable pageable) {
        log.info("Récupération des encours impôts/taxes - Critères: {}", searchDto);

        Specification<ImpotEtTaxe> spec = Specification.where(ImpotEtTaxeSpecifications.outstandingUntil(endOfMonth))
                .and(ImpotEtTaxeSpecifications.hasBanque(searchDto.getBanqueId()))
                .and(ImpotEtTaxeSpecifications.hasLibelle(searchDto.getLibelle()))
                .and(ImpotEtTaxeSpecifications.hasMotif(searchDto.getMotif()))
                .and(ImpotEtTaxeSpecifications.hasMontant(searchDto.getMontant()));

        Page<ImpotEtTaxeResponseDto> pageDto = impotEtTaxeRepository
                .findAll(spec, pageable)
                .map(impotEtTaxeMapper::toResponseDto);

        BigDecimal totalDesEncours = sumMontant(spec);

        EncoursImpotEtTaxeDto result = new EncoursImpotEtTaxeDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        log.info("Encours récupérés: {} éléments, Total: {}", pageDto.getTotalElements(), totalDesEncours);
        return result;
    }
}