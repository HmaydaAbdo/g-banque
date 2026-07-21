package staport.finance.gBanque.logs.services.imp;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.audit.AuditedEntityRegistry;
import staport.finance.gBanque.audit.EnversDiffService;
import staport.finance.gBanque.logs.dtos.LogEntryDetailResponse;
import staport.finance.gBanque.logs.dtos.LogResponse;
import staport.finance.gBanque.logs.dtos.LogSearchCriteria;
import staport.finance.gBanque.logs.entities.Log;
import staport.finance.gBanque.logs.mappers.LogMapper;
import staport.finance.gBanque.logs.repositories.LogRepository;
import staport.finance.gBanque.logs.services.LogService;
import staport.finance.gBanque.logs.specifications.LogSpecification;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LogServiceImpl implements LogService {

    private final LogRepository logRepository;
    private final LogMapper logMapper;
    private final AuditedEntityRegistry auditedEntityRegistry;
    private final EnversDiffService enversDiffService;

    @Autowired
    public LogServiceImpl(LogRepository logRepository, LogMapper logMapper,
                           AuditedEntityRegistry auditedEntityRegistry, EnversDiffService enversDiffService) {
        this.logRepository = logRepository;
        this.logMapper = logMapper;
        this.auditedEntityRegistry = auditedEntityRegistry;
        this.enversDiffService = enversDiffService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LogResponse> getLogs(LogSearchCriteria criteria, Pageable pageable) {
        // Construire la spécification à partir des critères
        Specification<Log> spec = LogSpecification.searchByCriteria(criteria);

        Page<Log> logPage = logRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        // Mapper les entités Log en DTOs LogResponse
        List<LogResponse> logResponses = logPage.getContent().stream()
                .map(logMapper::toDto)
                .collect(Collectors.toList());

        // Retourner une nouvelle Page de LogResponse
        return new PageImpl<>(logResponses, pageable, logPage.getTotalElements());
    }

    @Override
    @Transactional
    public void deleteLogById(Long id) {
        if (!logRepository.existsById(id)) {
            throw new EntityNotFoundException("Log with ID " + id + " not found.");
        }
        logRepository.deleteById(id);
    }

    @Override
    public List<String> getEntityNames() {
        return logRepository.findDistinctEntityNames();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LogEntryDetailResponse> getAuditDetail(Long logId) {
        Log log = logRepository.findById(logId)
                .orElseThrow(() -> new EntityNotFoundException("Log with ID " + logId + " not found."));

        if (!log.getLogDetails().isEmpty()) {
            return log.getLogDetails().stream().map(logMapper::toDetailDto).collect(Collectors.toList());
        }
        if (log.getRevisionNumber() == null) {
            return List.of();
        }
        Class<?> entityClass = auditedEntityRegistry.resolve(log.getEntityName());
        if (entityClass == null) {
            return List.of();
        }
        return enversDiffService.diff(entityClass, log.getEntityId(), log.getRevisionNumber(), log.getOperationType());
    }
}