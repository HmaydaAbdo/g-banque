package staport.finance.gBanque.operationErrone.services;

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
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.operationErrone.dtos.*;
import staport.finance.gBanque.operationErrone.entities.OperationErrone;
import staport.finance.gBanque.operationErrone.mappers.OperationErroneMapper;
import staport.finance.gBanque.operationErrone.repositories.OperationErroneRepository;
import staport.finance.gBanque.operationErrone.specs.OperationErroneSpec;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Implémentation du service Opérations Erronées.
 *
 * <p>Encapsulation des domaines (architecture.md) : la résolution de la {@link Banque} passe
 * exclusivement par {@link IBanqueService} — jamais par injection directe de
 * {@code BanqueRepository}.
 *
 * <p>Suppression bloquée si déjà PAYE (règle transversale, {@code conventions/architecture.md},
 * "Règle 5") — {@code deleteOperationErrone} lève {@link ValidationException} dans ce cas.
 *
 * <p>Bug latent corrigé (14/07/2026) : {@code endOfMonth} était auparavant un champ calculé une
 * seule fois à la construction du bean singleton — figé à la date de démarrage du serveur au lieu
 * d'être recalculé à chaque appel. Recalculé maintenant localement dans
 * {@link #getOutstandingOperationErrone}, même correctif que celui appliqué à
 * {@code ImpayeeServiceImpl}/{@code RestitutionServiceImpl} lors des sweeps précédents.
 */
@Service
public class OperationErroneServiceImpl implements IOperationErroneService {

    private static final Logger log = LoggerFactory.getLogger(OperationErroneServiceImpl.class);
    private final OperationErroneRepository operationErroneRepository;
    private final IBanqueService banqueService;
    private final OperationErroneMapper operationErroneMapper;

    public OperationErroneServiceImpl(OperationErroneRepository operationErroneRepository,
                                       IBanqueService banqueService,
                                       OperationErroneMapper operationErroneMapper) {
        this.operationErroneRepository = operationErroneRepository;
        this.banqueService = banqueService;
        this.operationErroneMapper = operationErroneMapper;
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = OperationErrone.class)
    public OperationErroneResponseDto createOperationErrone(OperationErroneRequestDto requestDto) {
        log.info("Création d'une opération erronée - Banque ID: {}, Libellé: '{}', Montant: {}, Motif: {}",
                requestDto.getBanqueId(), requestDto.getLibelle(), requestDto.getMontant(), requestDto.getMotif());

        Banque banque = banqueService.findOrThrow(requestDto.getBanqueId());

        OperationErrone operationErrone = operationErroneMapper.toEntity(requestDto, banque);
        OperationErrone saved = operationErroneRepository.save(operationErrone);

        log.info("Opération erronée créée avec succès - ID: {}, Statut: {}, Motif: {}",
                saved.getId(), saved.getStatus(), saved.getMotif());
        return operationErroneMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OperationErroneResponseDto getById(Long id) {
        log.info("Récupération de l'opération erronée avec l'ID: {}", id);

        OperationErrone operationErrone = operationErroneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opération erronée non trouvée avec l'ID: " + id));

        return operationErroneMapper.toResponseDto(operationErrone);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OperationErroneResponseDto> searchOperationErrone(OperationErroneSearchDto searchDto, Pageable pageable) {
        log.info("Recherche d'opérations erronées - Critères: {}, Page: {}, Taille: {}",
                searchDto, pageable.getPageNumber(), pageable.getPageSize());

        Page<OperationErroneResponseDto> result = operationErroneRepository.findAll(
                        OperationErroneSpec.buildSpecification(searchDto), pageable)
                .map(operationErroneMapper::toResponseDto);

        log.info("Recherche terminée - {} opération(s) erronée(s) trouvée(s) sur {} au total",
                result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = OperationErrone.class)
    public OperationErroneResponseDto updateOperationErrone(Long id, OperationErroneRequestDto requestDto) {
        log.info("Mise à jour de l'opération erronée ID: {} - Nouveau libellé: '{}', Motif: {}",
                id, requestDto.getLibelle(), requestDto.getMotif());

        OperationErrone operationErrone = operationErroneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opération erronée non trouvée avec l'ID: " + id));

        Banque banque = banqueService.findOrThrow(requestDto.getBanqueId());

        operationErrone.setLibelle(requestDto.getLibelle());
        operationErrone.setMontant(requestDto.getMontant());
        operationErrone.setBanque(banque);
        operationErrone.setDateEmission(requestDto.getDateEmission());
        operationErrone.setMotif(requestDto.getMotif());

        OperationErrone updated = operationErroneRepository.save(operationErrone);
        log.info("Opération erronée ID: {} mise à jour avec succès", id);
        return operationErroneMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = OperationErrone.class)
    public void deleteOperationErrone(Long id) {
        log.info("Suppression de l'opération erronée avec l'ID: {}", id);

        OperationErrone operationErrone = operationErroneRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Opération erronée non trouvée avec l'ID: {}", id);
                    return new ResourceNotFoundException("Opération erronée non trouvée avec l'ID: " + id);
                });

        if (operationErrone.getStatus() != Status.PREVU) {
            log.warn("Tentative de suppression d'une opération erronée déjà réglée — id: {}", id);
            throw new ValidationException("L'opération erronée id: " + id + " est déjà réglée et ne peut plus être supprimée.");
        }

        operationErroneRepository.deleteById(id);
        log.info("Opération erronée ID: {} supprimée avec succès", id);
    }

    @Override
    @Transactional(readOnly = true)
    public EncoursOperationErroneDto getOutstandingOperationErrone(OperationErroneSearchDto searchDto, Pageable pageable) {
        log.info("Récupération des opérations erronées en cours - Critères: {}", searchDto);

        LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        Specification<OperationErrone> spec = Specification.where(OperationErroneSpec.outstandingUntil(endOfMonth))
                .and(OperationErroneSpec.hasBanque(searchDto.getBanqueId()))
                .and(OperationErroneSpec.hasLibelle(searchDto.getLibelle()))
                .and(OperationErroneSpec.hasMontant(searchDto.getMontant()))
                .and(OperationErroneSpec.hasMotif(searchDto.getMotif()));

        Page<OperationErrone> page = operationErroneRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));
        BigDecimal total = operationErroneRepository.findAll(spec).stream()
                .map(OperationErrone::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Encours calculé: {}, Nombre d'opérations erronées: {}", total, page.getTotalElements());

        EncoursOperationErroneDto result = new EncoursOperationErroneDto();
        result.setPage(page.map(operationErroneMapper::toResponseDto));
        result.setTotalDesEncours(total);
        return result;
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = OperationErrone.class)
    public OperationErroneResponseDto toggleStatus(Long id) {
        log.info("Basculement du statut de l'opération erronée ID: {}", id);

        OperationErrone operationErrone = operationErroneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opération erronée non trouvée avec l'ID: " + id));

        Status oldStatus = operationErrone.getStatus();
        Status newStatus = oldStatus == Status.PAYE ? Status.PREVU : Status.PAYE;

        operationErrone.setStatus(newStatus);
        OperationErrone updated = operationErroneRepository.save(operationErrone);

        log.info("Statut de l'opération erronée ID: {} basculé de {} vers {}", id, oldStatus, newStatus);
        return operationErroneMapper.toResponseDto(updated);
    }
}
