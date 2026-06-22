package staport.finance.gBanque.audit;

/**
 * Permet à un traitement batch (CMT/Leasing main levée, synchro FunPaiement...)
 * de s'identifier auprès de l'audit avant d'invoquer des méthodes annotées
 * {@link AuditableAction} — sans ça, {@link CurrentUserResolver} retombe sur
 * {@code "System"} (pas de distinction entre jobs automatisés).
 *
 * <p>Mécanisme prêt mais pas encore câblé sur les jobs existants — voir
 * TODO.md ("Audit (qui a fait quoi)"). Usage prévu :
 * {@code BatchAuditContext.runAs("LeasingMainLevee", () -> service.doWork())}.
 */
public final class BatchAuditContext {

    private static final ThreadLocal<String> CURRENT_JOB = new ThreadLocal<>();

    private BatchAuditContext() {
    }

    public static String getCurrentJobName() {
        return CURRENT_JOB.get();
    }

    public static void runAs(String jobName, Runnable action) {
        CURRENT_JOB.set(jobName);
        try {
            action.run();
        } finally {
            CURRENT_JOB.remove();
        }
    }
}
