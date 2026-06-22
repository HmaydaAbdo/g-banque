package staport.finance.gBanque.audit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.springframework.stereotype.Service;
import staport.finance.gBanque.logs.dtos.LogEntryDetailResponse;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Reconstruit le détail champ-par-champ (avant/après) d'une révision Envers,
 * à la demande — jamais calculé ni stocké à l'écriture (voir Backend/CLAUDE.md,
 * section "Audit"). Mécanisme générique unique, réutilisé par toute entité
 * auditée — jamais de comparaison écrite à la main par entité.
 */
@Service
public class EnversDiffService {

    private final EntityManager entityManager;

    public EnversDiffService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * @param operationType {@code Log.operationType} — si {@code "DELETE"}, l'état
     *                       "après" est considéré absent (l'entité n'existe plus).
     */
    public List<LogEntryDetailResponse> diff(Class<?> entityClass, Long entityId, Number revision, String operationType) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        List<Number> revisions = reader.getRevisions(entityClass, entityId);
        int idx = revisions.indexOf(revision);

        Object previous = (idx > 0) ? reader.find(entityClass, entityId, revisions.get(idx - 1)) : null;
        Object current = "DELETE".equals(operationType) ? null : reader.find(entityClass, entityId, revision);

        return compareFields(entityClass, previous, current);
    }

    private List<LogEntryDetailResponse> compareFields(Class<?> entityClass, Object previous, Object current) {
        List<LogEntryDetailResponse> details = new ArrayList<>();
        for (Field field : entityClass.getDeclaredFields()) {
            if (isRelationOrCollection(field)) {
                continue;
            }
            String oldValue = readField(field, previous);
            String newValue = readField(field, current);
            if (!Objects.equals(oldValue, newValue)) {
                details.add(new LogEntryDetailResponse(null, field.getName(), oldValue, newValue));
            }
        }
        return details;
    }

    private boolean isRelationOrCollection(Field field) {
        return Collection.class.isAssignableFrom(field.getType())
                || field.isAnnotationPresent(OneToMany.class)
                || field.isAnnotationPresent(ManyToMany.class)
                || field.isAnnotationPresent(ManyToOne.class)
                || field.isAnnotationPresent(OneToOne.class);
    }

    private String readField(Field field, Object target) {
        if (target == null) {
            return null;
        }
        try {
            field.setAccessible(true);
            Object value = field.get(target);
            return value == null ? null : String.valueOf(value);
        } catch (IllegalAccessException e) {
            return null;
        }
    }
}
