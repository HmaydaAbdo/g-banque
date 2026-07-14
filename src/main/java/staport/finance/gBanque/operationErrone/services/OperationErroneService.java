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
import staport.finance.gBanque.operationErrone.dtos.*;
import staport.finance.gBanque.operationErrone.entities.OperationErrone;
import staport.finance.gBanque.operationErrone.mappers.OperationErroneMapper;
import staport.finance.gBanque.operationErrone.repositories.OperationErroneRepository;
import staport.finance.gBanque.operationErrone.specs.OperationErroneSpec;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Service
public class OperationErroneService {

    private static final Logger log = LoggerFactory.getLogger(OperationErroneService.class);
    private final OperationErroneRepository operationErroneRepository;
    private final IBanqueService banqueService;
    private final OperationErroneMapper operationErroneMapper;
    LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());


    public OperationErroneService(OperationErroneRepository operationErroneRepository,
                                  IBanqueService banqueService,
                                  OperationErroneMapper operationErroneMapper) {
        this.operationErroneRepository = operationErroneRepository;
        this.banqueService = banqueService;
        this.operationErroneMapper = operationErroneMapper;
        log.info("Initialisation du service OperationErrone");
    }

    @Transactional
    public OperationErroneResponseDto createOperationErrone(OperationErroneRequestDto requestDto) {
        log.info("Création d'une opération erronée - Banque ID: {}, Libellé: '{}', Montant: {}, Motif: {}",
                requestDto.getBanqueId(), requestDto.getLibelle(), requestDto.getMontant(), requestDto.getMotif());

        Banque banque = banqueService.findOrThrow(requestDto.getBanqueId());

        log.debug("Banque trouvée: {} (ID: {})", banque.getNom(), banque.getId());
        OperationErrone operationErrone = operationErroneMapper.toEntity(requestDto, banque);
        OperationErrone saved = operationErroneRepository.save(operationErrone);

        log.info("Opération erronée créée avec succès - ID: {}, Statut: {}, Motif: {}",
                saved.getId(), saved.getStatus(), saved.getMotif());
        return operationErroneMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public OperationErroneResponseDto getById(Long id) {
        log.info("Récupération de l'opération erronée avec l'ID: {}", id);

        OperationErrone operationErrone = operationErroneRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Opération erronée non trouvée avec l'ID: {}", id);
                    return new IllegalArgumentException("Opération erronée non trouvée avec l'ID: " + id);
                });

        log.debug("Opération erronée trouvée - Libellé: '{}', Montant: {}, Motif: {}",
                operationErrone.getLibelle(), operationErrone.getMontant(), operationErrone.getMotif());
        return operationErroneMapper.toResponseDto(operationErrone);
    }

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

    @Transactional
    public OperationErroneResponseDto updateOperationErrone(Long id, OperationErroneRequestDto requestDto) {
        log.info("Mise à jour de l'opération erronée ID: {} - Nouveau libellé: '{}', Motif: {}",
                id, requestDto.getLibelle(), requestDto.getMotif());

        OperationErrone operationErrone = operationErroneRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Opération erronée non trouvée avec l'ID: {}", id);
                    return new IllegalArgumentException("Opération erronée non trouvée avec l'ID: " + id);
                });

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

    @Transactional
    public void deleteOperationErrone(Long id) {
        log.info("Suppression de l'opération erronée avec l'ID: {}", id);

        if (!operationErroneRepository.existsById(id)) {
            log.error("Opération erronée non trouvée avec l'ID: {}", id);
            throw new IllegalArgumentException("Opération erronée non trouvée avec l'ID: " + id);
        }

        operationErroneRepository.deleteById(id);
        log.info("Opération erronée ID: {} supprimée avec succès", id);
    }

    @Transactional(readOnly = true)
    public EncoursOperationErroneDto getOutstandingOperationErrone(OperationErroneSearchDto searchDto, Pageable pageable) {
        log.info("Récupération des opérations erronées en cours - Critères: {}", searchDto);

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

    @Transactional
    public OperationErroneResponseDto toggleStatus(Long id) {
        log.info("Basculement du statut de l'opération erronée ID: {}", id);

        OperationErrone operationErrone = operationErroneRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Opération erronée non trouvée avec l'ID: {}", id);
                    return new IllegalArgumentException("Opération erronée non trouvée avec l'ID: " + id);
                });

        Status oldStatus = operationErrone.getStatus();
        Status newStatus = oldStatus == Status.PAYE ? Status.PREVU : Status.PAYE;

        operationErrone.setStatus(newStatus);
        OperationErrone updated = operationErroneRepository.save(operationErrone);

        log.info("Statut de l'opération erronée ID: {} basculé de {} vers {}", id, oldStatus, newStatus);
        return operationErroneMapper.toResponseDto(updated);
    }
}