package staport.finance.gBanque.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;

/**
 * Aspect générique unique alimentant la table {@code Log} pour toute méthode
 * de Service annotée {@link AuditableAction} — voir Backend/CLAUDE.md,
 * section "Audit". Remplace les anciens aspects dédiés par entité au fur et
 * à mesure de la migration module par module (coexistence assumée pendant la
 * transition — voir TODO.md).
 *
 * <p>N'écrit jamais le détail champ-par-champ ({@code LogEntryDetail}) — il
 * est lu à la demande via {@link EnversDiffService}, à partir du numéro de
 * révision Envers capturé ici après le commit de la transaction métier.
 *
 * <p>Robuste à l'ordre des proxies AOP (@Transactional vs cet aspect) :
 * {@code TransactionSynchronization.afterCommit} ne dépend pas de qui
 * enveloppe qui — si une transaction est encore active au retour de
 * {@code proceed()}, on diffère ; sinon (transaction déjà committée), on
 * écrit immédiatement.
 */
@Aspect
@Component
public class GenericAuditAspect {

    private final AuditLogWriter auditLogWriter;

    public GenericAuditAspect(AuditLogWriter auditLogWriter) {
        this.auditLogWriter = auditLogWriter;
    }

    @Around("@annotation(auditableAction)")
    public Object around(ProceedingJoinPoint pjp, AuditableAction auditableAction) throws Throwable {
        Object result = pjp.proceed();

        Long entityId = auditableAction.idAccessor().isEmpty()
                ? extractEntityId(result, pjp.getArgs())
                : extractIdViaAccessor(result, auditableAction.idAccessor());
        if (entityId == null) {
            // On ne bloque jamais l'opération métier pour un souci d'audit —
            // si l'ID n'est pas extractible, on logue l'absence et on continue.
            return result;
        }

        String username = CurrentUserResolver.resolveUsername();
        Class<?> entityClass = auditableAction.entity();
        String operation = auditableAction.operation().name();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    auditLogWriter.recordLog(username, entityClass, entityId, operation);
                }
            });
        } else {
            auditLogWriter.recordLog(username, entityClass, entityId, operation);
        }

        return result;
    }

    /**
     * Invoque explicitement l'accesseur nommé par {@code AuditableAction#idAccessor()}
     * sur la valeur de retour — utilisé lorsque l'entité auditée n'est pas celle dont
     * l'ID figure en premier dans le résultat (ex. DTO de transaction exposant à la fois
     * l'ID de la transaction et celui de l'échéance source).
     */
    private Long extractIdViaAccessor(Object result, String accessorName) {
        if (result == null) {
            return null;
        }
        try {
            Method accessor = result.getClass().getMethod(accessorName);
            Object value = accessor.invoke(result);
            return value instanceof Long ? (Long) value : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Long extractEntityId(Object result, Object[] args) {
        Long fromResult = extractIdFromResult(result);
        if (fromResult != null) {
            return fromResult;
        }
        for (Object arg : args) {
            if (arg instanceof Long longArg) {
                return longArg;
            }
        }
        return null;
    }

    /**
     * Essaie {@code result.id()} (record) puis {@code result.getId()} (bean) ;
     * si {@code result} est lui-même un {@code Long} (ex. méthode "upsert" qui
     * renvoie l'ID de la ligne touchée), l'utilise directement.
     */
    private Long extractIdFromResult(Object result) {
        if (result == null) {
            return null;
        }
        if (result instanceof Long longResult) {
            return longResult;
        }
        try {
            Method idMethod;
            try {
                idMethod = result.getClass().getMethod("id");
            } catch (NoSuchMethodException e) {
                idMethod = result.getClass().getMethod("getId");
            }
            Object value = idMethod.invoke(result);
            return value instanceof Long ? (Long) value : null;
        } catch (Exception e) {
            return null;
        }
    }
}
