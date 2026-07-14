package staport.finance.gBanque.impayees.service;


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
import staport.finance.gBanque.impayees.dtos.*;
import staport.finance.gBanque.impayees.entities.Impayee;
import staport.finance.gBanque.impayees.mapper.ImpayeeMapper;
import staport.finance.gBanque.impayees.repository.ImpayeeRepository;
import staport.finance.gBanque.impayees.specs.ImpayeeSpecifications;
import staport.finance.gBanque.prefinancement.enums.Status;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Implémentation du service Impayées.
 *
 * <p>Encapsulation des domaines (architecture.md) : la résolution de la {@link Banque} passe
 * exclusivement par {@link IBanqueService} — jamais par injection directe de
 * {@code BanqueRepository}.
 *
 * <p>Bug latent corrigé (14/07/2026) : {@code endOfMonth} était auparavant un champ calculé une
 * seule fois à la construction du bean singleton — figé à la date de démarrage du serveur au lieu
 * d'être recalculé à chaque appel. Recalculé maintenant localement dans
 * {@link #getOutstandingImpayees}, même correctif que celui appliqué à
 * {@code EcheanceCMTService} lors du sweep CMT.
 */
@Service
public class ImpayeeServiceImpl implements IImpayeeService {

    private static final Logger log = LogManager.getLogger(ImpayeeServiceImpl.class);
    private final ImpayeeRepository impayeeRepository;
    private final IBanqueService banqueService;
    private final ImpayeeMapper impayeeMapper;

    public ImpayeeServiceImpl(ImpayeeRepository impayeeRepository, IBanqueService banqueService, ImpayeeMapper impayeeMapper) {
        this.impayeeRepository = impayeeRepository;
        this.banqueService = banqueService;
        this.impayeeMapper = impayeeMapper;
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = Impayee.class)
    public ImpayeeResponseDto createImpayee(ImpayeeRequestDto requestDto) {
        log.info("Début de création d'une impayée avec les données: {}", requestDto);

        Banque banque = banqueService.findOrThrow(requestDto.getBanqueId());

        Impayee impayee = impayeeMapper.toEntity(requestDto, banque);
        impayeeRepository.save(impayee);

        log.info("Impayée créée avec succès: id={}", impayee.getId());
        return impayeeMapper.toResponseDto(impayee);
    }

    @Override
    @Transactional(readOnly = true)
    public ImpayeeResponseDto getById(Long id) {
        log.info("Récupération de l'impayée id={}", id);
        Impayee impayee = impayeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Impayée introuvable avec l'ID: " + id));
        return impayeeMapper.toResponseDto(impayee);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ImpayeeResponseDto> searchImpayees(ImpayeeSearchDto searchDto, Pageable pageable) {
        log.info("Recherche d'impayées - Critères: {}", searchDto);
        Page<ImpayeeResponseDto> result = impayeeRepository.findAll(ImpayeeSpecifications.buildSpecification(searchDto), pageable)
                .map(impayeeMapper::toResponseDto);
        log.info("Recherche terminée: {} éléments trouvés", result.getTotalElements());
        return result;
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Impayee.class)
    public ImpayeeResponseDto updateImpayee(Long id, ImpayeeRequestDto requestDto) {
        log.info("Mise à jour de l'impayée id={}", id);

        Impayee impayee = impayeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Impayée introuvable avec l'ID: " + id));

        Banque banque = banqueService.findOrThrow(requestDto.getBanqueId());

        impayee.setLibelle(requestDto.getLibelle());
        impayee.setMontant(requestDto.getMontant());
        impayee.setBanque(banque);
        impayee.setMotif(requestDto.getMotif());
        impayee.setDateEmission(requestDto.getDateEmission());
        impayee.setObservation(requestDto.getObservation());

        impayeeRepository.save(impayee);
        log.info("Impayée mise à jour avec succès: id={}", id);
        return impayeeMapper.toResponseDto(impayee);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = Impayee.class)
    public void deleteImpayee(Long id) {
        log.info("Suppression de l'impayée id={}", id);

        Impayee impayee = impayeeRepository.findById(id).orElseThrow(() -> {
            log.warn("Impayée introuvable — id={}", id);
            return new ResourceNotFoundException("Impayée introuvable avec l'ID: " + id);
        });

        if (impayee.getStatus() != Status.PREVU) {
            log.warn("Tentative de suppression d'une impayée déjà réglée — id={}", id);
            throw new ValidationException("L'impayée id: " + id + " est déjà réglée et ne peut plus être supprimée.");
        }

        impayeeRepository.deleteById(id);
        log.info("Impayée supprimée avec succès: id={}", id);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Impayee.class)
    public ImpayeeResponseDto toggleStatus(Long id) {
        log.info("Basculement du statut de l'impayée ID: {}", id);

        Impayee impayee = impayeeRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Impayée introuvable — id={}", id);
                    return new ResourceNotFoundException("Impayée introuvable avec l'ID: " + id);
                });

        Status oldStatus = impayee.getStatus();
        Status newStatus = (oldStatus == Status.PAYE) ? Status.PREVU : Status.PAYE;

        impayee.setStatus(newStatus);
        Impayee updated = impayeeRepository.save(impayee);

        log.info("Statut basculé pour ID {}: {} -> {}", id, oldStatus, newStatus);
        return impayeeMapper.toResponseDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public EncoursImpayeeDto getOutstandingImpayees(ImpayeeSearchDto searchDto, Pageable pageable) {
        log.info("Récupération des encours impayées - Critères: {}", searchDto);

        LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        Specification<Impayee> spec = Specification.where(ImpayeeSpecifications.outstandingUntil(endOfMonth, searchDto.getType()))
                .and(ImpayeeSpecifications.hasBanque(searchDto.getBanqueId()))
                .and(ImpayeeSpecifications.hasLibelle(searchDto.getLibelle()))
                .and(ImpayeeSpecifications.hasMotif(searchDto.getMotif()))
                .and(ImpayeeSpecifications.hasMontant(searchDto.getMontant()));

        Page<Impayee> impayeePage = impayeeRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        BigDecimal totalDesEncours = impayeeRepository.findAll(spec).stream()
                .map(Impayee::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Page<ImpayeeResponseDto> pageDto = impayeePage.map(impayeeMapper::toResponseDto);

        EncoursImpayeeDto result = new EncoursImpayeeDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        log.info("Encours récupérés: {} éléments, Total: {}", pageDto.getTotalElements(), totalDesEncours);
        return result;
    }
}
