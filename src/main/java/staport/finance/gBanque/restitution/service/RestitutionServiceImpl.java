package staport.finance.gBanque.restitution.service;

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
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.restitution.dtos.EncoursRestitution;
import staport.finance.gBanque.restitution.dtos.RestitutionRequestDto;
import staport.finance.gBanque.restitution.dtos.RestitutionResponseDto;
import staport.finance.gBanque.restitution.dtos.RestitutionSearchDto;
import staport.finance.gBanque.restitution.entities.Restitution;
import staport.finance.gBanque.restitution.mapper.RestitutionMapper;
import staport.finance.gBanque.restitution.repository.RestitutionRepository;
import staport.finance.gBanque.restitution.specs.RestitutionSpec;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Implémentation du service Restitution.
 *
 * <p>Encapsulation des domaines (architecture.md) : la résolution de la {@link Banque} passe
 * exclusivement par {@link IBanqueService} — jamais par injection directe de
 * {@code BanqueRepository}.
 *
 * <p>Suppression bloquée si déjà PAYE (précisé par HMAYDA le 14/07/2026 : une restitution réglée ne
 * peut plus être supprimée, même règle qu'Impayées/Impôts et Taxes) — {@code deleteRestitution} lève
 * {@link ValidationException} dans ce cas.
 *
 * <p>Bug latent corrigé (14/07/2026) : {@code endOfMonth} était auparavant un champ calculé une
 * seule fois à la construction du bean singleton — figé à la date de démarrage du serveur au lieu
 * d'être recalculé à chaque appel. Recalculé maintenant localement dans
 * {@link #getOutstandingRestitutions}, même correctif que celui appliqué à {@code ImpayeeServiceImpl}
 * lors du sweep Impayées.
 */
@Service
public class RestitutionServiceImpl implements IRestitutionService {

    private static final Logger log = LogManager.getLogger(RestitutionServiceImpl.class);
    private final RestitutionRepository restitutionRepository;
    private final IBanqueService banqueService;
    private final RestitutionMapper restitutionMapper;

    public RestitutionServiceImpl(RestitutionRepository restitutionRepository, IBanqueService banqueService, RestitutionMapper restitutionMapper) {
        this.restitutionRepository = restitutionRepository;
        this.banqueService = banqueService;
        this.restitutionMapper = restitutionMapper;
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = Restitution.class)
    public RestitutionResponseDto createRestitution(RestitutionRequestDto requestDto) {
        log.info("Début de création d'une restitution avec les données suivantes: {}", requestDto);

        Banque banque = banqueService.findOrThrow(requestDto.getBanqueId());

        Restitution restitution = restitutionMapper.toEntity(requestDto, banque);
        restitutionRepository.save(restitution);

        log.info("Restitution créée avec succès avec l'id={}", restitution.getId());
        return restitutionMapper.toResponseDto(restitution);
    }

    @Override
    @Transactional(readOnly = true)
    public RestitutionResponseDto getById(Long id) {
        log.info("Récupération de la restitution avec l'id={}", id);

        Restitution restitution = restitutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restitution introuvable avec l'id: " + id));

        log.info("Restitution récupérée avec succès: id={}", id);
        return restitutionMapper.toResponseDto(restitution);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RestitutionResponseDto> searchRestitutions(RestitutionSearchDto searchDto, Pageable pageable) {
        log.info("Recherche de restitutions avec les critères: {}", searchDto);

        Page<RestitutionResponseDto> result = restitutionRepository.findAll(
                        RestitutionSpec.buildSpecification(searchDto),
                        pageable
                )
                .map(restitutionMapper::toResponseDto);

        log.info("Recherche terminée: {} restitutions trouvées", result.getTotalElements());
        return result;
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Restitution.class)
    public RestitutionResponseDto updateRestitution(Long id, RestitutionRequestDto requestDto) {
        log.info("Début de mise à jour de la restitution id={} avec les données: {}", id, requestDto);

        Restitution restitution = restitutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restitution introuvable avec l'id: " + id));

        Banque banque = banqueService.findOrThrow(requestDto.getBanqueId());

        restitution.setLibelle(requestDto.getLibelle());
        restitution.setObservation(requestDto.getObservation());
        restitution.setMontant(requestDto.getMontant());
        restitution.setBanque(banque);
        restitution.setDateEmission(requestDto.getDateEmission());

        restitutionRepository.save(restitution);
        log.info("Restitution mise à jour avec succès: id={}", restitution.getId());
        return restitutionMapper.toResponseDto(restitution);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = Restitution.class)
    public void deleteRestitution(Long id) {
        log.info("Début de suppression de la restitution avec l'id={}", id);

        Restitution restitution = restitutionRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Restitution introuvable avec l'id={}", id);
                    return new ResourceNotFoundException("Restitution introuvable avec l'id: " + id);
                });

        if (restitution.getStatus() != Status.PREVU) {
            log.warn("Tentative de suppression d'une restitution déjà réglée — id={}", id);
            throw new ValidationException("La restitution id: " + id + " est déjà réglée et ne peut plus être supprimée.");
        }

        restitutionRepository.deleteById(id);
        log.info("Restitution supprimée avec succès: id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public EncoursRestitution getOutstandingRestitutions(RestitutionSearchDto searchDto, Pageable pageable) {
        log.info("Récupération des encours de restitutions avec les critères: {}", searchDto);

        LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        Specification<Restitution> spec = Specification.where(RestitutionSpec.outstandingUntil(endOfMonth))
                .and(RestitutionSpec.hasBanque(searchDto.getBanqueId()))
                .and(RestitutionSpec.hasLibelle(searchDto.getLibelle()))
                .and(RestitutionSpec.hasMontant(searchDto.getMontant()))
                .and(RestitutionSpec.hasStatus(searchDto.getStatus()));

        Page<Restitution> restitutionPage = restitutionRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        BigDecimal totalDesEncours = restitutionRepository.findAll(spec).stream()
                .map(Restitution::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Page<RestitutionResponseDto> pageDto = restitutionPage.map(restitutionMapper::toResponseDto);

        EncoursRestitution result = new EncoursRestitution();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        log.info("Encours de restitutions récupérés: {} restitutions trouvées, total des encours={}",
                pageDto.getTotalElements(), totalDesEncours);

        return result;
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Restitution.class)
    public RestitutionResponseDto toggleStatus(Long id) {
        log.info("Basculement du statut de la restitution ID: {}", id);

        Restitution restitution = restitutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restitution introuvable avec l'id: " + id));

        Status oldStatus = restitution.getStatus();
        Status newStatus = oldStatus == Status.PAYE ? Status.PREVU : Status.PAYE;

        restitution.setStatus(newStatus);
        Restitution updated = restitutionRepository.save(restitution);

        log.info("Statut de la restitution ID: {} basculé de {} vers {}", id, oldStatus, newStatus);
        return restitutionMapper.toResponseDto(updated);
    }
}
