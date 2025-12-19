package staport.finance.gBanque.batch.jobs.functionaLines.generalFunctionalLines.dtos;

import staport.finance.gBanque.LigneFonctionnelle.entities.LigneFonctionnelle;
import staport.finance.gBanque.notification.entities.NotificationType;

import java.util.Objects;

/**
 * DTO (Data Transfer Object) to transfer processed functional line data
 * from the ItemProcessor to the ItemWriter in a Spring Batch job.
 * It encapsulates the functional line entity (which may have been modified, e.g., deactivated),
 * a flag indicating if the line needs to be persisted (e.g., due to deactivation),
 * and details for a notification if one needs to be generated and sent.
 *
 * This helps maintain the Single Responsibility Principle by clearly defining
 * the data contract between the processing and writing stages.
 */
public class ProcessedFunctionalLine {

    private final LigneFonctionnelle ligneFonctionnelle;
    private final String notificationTitle;
    private final String notificationContent;
    private final NotificationType notificationType;
    private final boolean needsPersistence; // ✅ Nouveau champ

    /**
     * Complete constructor for cases where a functional line is processed with all details.
     *
     * @param ligneFonctionnelle The LigneFonctionnelle entity, potentially modified.
     * @param notificationTitle The title for the notification.
     * @param notificationContent The content/body for the notification.
     * @param notificationType The type of notification (e.g., ALERT, WARNING).
     * @param needsPersistence Flag indicating if the entity needs explicit persistence.
     */
    public ProcessedFunctionalLine(
            LigneFonctionnelle ligneFonctionnelle,
            String notificationTitle,
            String notificationContent,
            NotificationType notificationType,
            boolean needsPersistence) {
        this.ligneFonctionnelle = Objects.requireNonNull(ligneFonctionnelle, "ligneFonctionnelle cannot be null");
        this.notificationTitle = notificationTitle;
        this.notificationContent = notificationContent;
        this.notificationType = notificationType;
        this.needsPersistence = needsPersistence;
    }

    /**
     * Constructor for cases where a functional line is processed with notification but no persistence needed.
     *
     * @param ligneFonctionnelle The LigneFonctionnelle entity.
     * @param notificationTitle The title for the notification.
     * @param notificationContent The content/body for the notification.
     * @param notificationType The type of notification (e.g., ALERT, WARNING).
     */
    public ProcessedFunctionalLine(
            LigneFonctionnelle ligneFonctionnelle,
            String notificationTitle,
            String notificationContent,
            NotificationType notificationType) {
        this(ligneFonctionnelle, notificationTitle, notificationContent, notificationType, false);
    }

    /**
     * Constructor for cases where a functional line is processed but no notification is required
     * and no explicit persistence is needed.
     *
     * @param ligneFonctionnelle The LigneFonctionnelle entity.
     */
    public ProcessedFunctionalLine(LigneFonctionnelle ligneFonctionnelle) {
        this(ligneFonctionnelle, null, null, null, false);
    }

    // --- Getters ---

    public LigneFonctionnelle getLigneFonctionnelle() {
        return ligneFonctionnelle;
    }

    public String getNotificationTitle() {
        return notificationTitle;
    }

    public String getNotificationContent() {
        return notificationContent;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public boolean needsPersistence() {
        return needsPersistence;
    }

    /**
     * Checks if this processed functional line carries notification details.
     *
     * @return true if a notification type is set, false otherwise.
     */
    public boolean hasNotification() {
        return notificationType != null;
    }

    @Override
    public String toString() {
        return "ProcessedFunctionalLine{" +
                "ligneFonctionnelleId=" + (ligneFonctionnelle != null ? ligneFonctionnelle.getId() : "null") +
                ", notificationType=" + notificationType +
                ", notificationTitle='" + notificationTitle + '\'' +
                ", needsPersistence=" + needsPersistence +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessedFunctionalLine that = (ProcessedFunctionalLine) o;
        return needsPersistence == that.needsPersistence &&
                Objects.equals(ligneFonctionnelle, that.ligneFonctionnelle) &&
                Objects.equals(notificationTitle, that.notificationTitle) &&
                Objects.equals(notificationContent, that.notificationContent) &&
                notificationType == that.notificationType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ligneFonctionnelle, notificationTitle, notificationContent,
                notificationType, needsPersistence);
    }
}