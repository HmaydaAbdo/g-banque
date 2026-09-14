package staport.finance.gBanque.audit;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Résout l'identité "qui a fait l'action" pour l'audit — utilisé à la fois par
 * {@link AuditRevisionListener} (révisions Envers) et {@code GenericAuditAspect}
 * (table {@code Log}), pour que les deux restent cohérents.
 *
 * <p>Static (pas un bean Spring) car {@code RevisionListener} est instancié
 * directement par Hibernate Envers, hors du contexte d'injection Spring.
 *
 * <p>Priorité : job batch identifié ({@link BatchAuditContext}) &gt; utilisateur
 * authentifié &gt; {@code "System"} (même comportement que les anciens
 * {@code *AuditAspect}, désormais centralisé ici).
 */
public final class CurrentUserResolver {

    private CurrentUserResolver() {
    }

    public static String resolveUsername() {
        String batchJob = BatchAuditContext.getCurrentJobName();
        if (batchJob != null) {
            return "BATCH:" + batchJob;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            return authentication.getName();
        }
        return "System";
    }
}
