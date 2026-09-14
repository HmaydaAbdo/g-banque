package staport.finance.gBanque.audit;

import org.hibernate.envers.RevisionListener;

/**
 * Pose {@code username} sur chaque révision Envers — voir {@link CurrentUserResolver}
 * pour la résolution (utilisateur authentifié, fallback batch/"System").
 */
public class AuditRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        AuditRevisionEntity entity = (AuditRevisionEntity) revisionEntity;
        entity.setUsername(CurrentUserResolver.resolveUsername());
    }
}
