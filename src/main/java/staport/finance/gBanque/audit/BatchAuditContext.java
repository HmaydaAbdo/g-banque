package staport.finance.gBanque.audit;

/**
 * Permet à un traitement batch (CMT/Leasing main levée, synchro FunPaiement...)
 * de s'identifier auprès de l'audit avant d'invoquer des méthodes annotées
 * {@link AuditableAction} — sans ça, {@link CurrentUserResolver} retombe sur
 * {@code "System"} (pas de distinction entre jobs automatisés).
 *
 * <p>Câblé (13/07/2026) sur {@code CreditMoyenTermeMainLeveeJobScheduler} ("CmtMainLevee"),
 * {@code ContratLeasingMainLeveeJobScheduler} ("LeasingMainLevee") et
 * {@code EtasDesEncoursService.launchPaymentImportJob} ("SynchronisationFunPaiement") — via
 * {@link #setCurrentJobName(String)}/{@link #clearCurrentJobName()} (leurs méthodes déclarent des
 * exceptions vérifiées Spring Batch, incompatibles avec le {@link Runnable} de {@link #runAs}).
 * Sans effet observable tant qu'aucune entité persistée par ces jobs n'est {@code @Audited} — à
 * ce jour, seule {@code Notification} (main levée) est concernée et ne l'est pas.
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

    /**
     * Variante manuelle de {@link #runAs(String, Runnable)} pour les traitements batch qui
     * déclarent des exceptions vérifiées (ex. {@code JobExecutionException} de Spring Batch) et ne
     * peuvent donc pas être exprimés comme un simple {@link Runnable}. À utiliser en début du bloc
     * {@code try} déjà existant, avec un appel symétrique à {@link #clearCurrentJobName()} dans le
     * {@code finally} correspondant.
     */
    public static void setCurrentJobName(String jobName) {
        CURRENT_JOB.set(jobName);
    }

    /**
     * @see #setCurrentJobName(String)
     */
    public static void clearCurrentJobName() {
        CURRENT_JOB.remove();
    }
}
