package staport.finance.gBanque.audit;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import org.hibernate.envers.Audited;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Associe le nom simple d'une entité auditée (ex. {@code "User"}) à sa classe
 * Java — pour retrouver la classe à partir de {@code Log.entityName} (une
 * chaîne) quand on reconstruit le détail à la demande via Envers
 * ({@code EnversDiffService}).
 *
 * <p>Construit une fois au démarrage en scannant les entités JPA portant
 * {@code @Audited} — aucune liste à maintenir à la main.
 */
@Component
public class AuditedEntityRegistry {

    private final EntityManagerFactory entityManagerFactory;
    private final Map<String, Class<?>> bySimpleName = new HashMap<>();

    public AuditedEntityRegistry(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @PostConstruct
    void buildRegistry() {
        for (EntityType<?> entityType : entityManagerFactory.getMetamodel().getEntities()) {
            Class<?> javaType = entityType.getJavaType();
            if (javaType.isAnnotationPresent(Audited.class)) {
                bySimpleName.put(javaType.getSimpleName(), javaType);
            }
        }
    }

    public Class<?> resolve(String entityName) {
        return bySimpleName.get(entityName);
    }
}
