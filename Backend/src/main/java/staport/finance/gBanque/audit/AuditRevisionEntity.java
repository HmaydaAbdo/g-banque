package staport.finance.gBanque.audit;

import jakarta.persistence.Entity;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

/**
 * Révision Envers custom — étend {@link DefaultRevisionEntity} (id + timestamp
 * déjà fournis) en ajoutant {@code username}, posé par {@link AuditRevisionListener}.
 */
@Entity
@RevisionEntity(AuditRevisionListener.class)
public class AuditRevisionEntity extends DefaultRevisionEntity {

    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
