package staport.finance.gBanque.audit;

import jakarta.persistence.EntityManager;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.logs.entities.Log;
import staport.finance.gBanque.logs.repositories.LogRepository;

import java.util.List;

/**
 * Écrit la ligne {@code Log} dans sa propre transaction.
 *
 * <p>Appelé depuis {@code GenericAuditAspect} après le commit de la
 * transaction métier d'origine (via {@code TransactionSynchronization.afterCommit}),
 * moment où aucune transaction n'est active — cet appel doit passer par un
 * bean Spring proxié (@Transactional) pour en ouvrir une nouvelle proprement
 * (l'auto-invocation depuis le même bean n'aurait pas déclenché le proxy).
 */
@Component
public class AuditLogWriter {

    private final LogRepository logRepository;
    private final EntityManager entityManager;

    public AuditLogWriter(LogRepository logRepository, EntityManager entityManager) {
        this.logRepository = logRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public void recordLog(String username, Class<?> entityClass, Long entityId, String operation) {
        Integer revisionNumber = resolveLatestRevision(entityClass, entityId);
        Log log = new Log(username, entityClass.getSimpleName(), entityId);
        log.setOperationType(operation);
        log.setRevisionNumber(revisionNumber);
        logRepository.save(log);
    }

    private Integer resolveLatestRevision(Class<?> entityClass, Long entityId) {
        try {
            AuditReader reader = AuditReaderFactory.get(entityManager);
            List<Number> revisions = reader.getRevisions(entityClass, entityId);
            return revisions.isEmpty() ? null : revisions.get(revisions.size() - 1).intValue();
        } catch (Exception e) {
            return null;
        }
    }
}
