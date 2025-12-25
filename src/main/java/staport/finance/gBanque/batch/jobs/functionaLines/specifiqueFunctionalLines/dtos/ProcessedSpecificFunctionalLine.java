package staport.finance.gBanque.batch.jobs.functionaLines.specifiqueFunctionalLines.dtos;

import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.notification.entities.NotificationType;

import java.util.Objects;

/**
 * DTO to transfer processed specific functional line data from the ItemProcessor to the ItemWriter.
 * Encapsulates the entity, notification details, and persistence flag.
 */
public class ProcessedSpecificFunctionalLine {

    private final LigneFonctionnelleSpecifique ligneFonctionnelleSpecifique;
    private final String notificationTitle;
    private final String notificationContent;
    private final NotificationType notificationType;
    private final boolean needsPersistence;

    /**
     * Complete constructor with all fields.
     */
    public ProcessedSpecificFunctionalLine(
            LigneFonctionnelleSpecifique ligneFonctionnelleSpecifique,
            String notificationTitle,
            String notificationContent,
            NotificationType notificationType,
            boolean needsPersistence) {
        this.ligneFonctionnelleSpecifique = Objects.requireNonNull(ligneFonctionnelleSpecifique);
        this.notificationTitle = notificationTitle;
        this.notificationContent = notificationContent;
        this.notificationType = notificationType;
        this.needsPersistence = needsPersistence;
    }

    /**
     * Constructor for cases with notification but no persistence needed.
     */
    public ProcessedSpecificFunctionalLine(
            LigneFonctionnelleSpecifique ligneFonctionnelleSpecifique,
            String notificationTitle,
            String notificationContent,
            NotificationType notificationType) {
        this(ligneFonctionnelleSpecifique, notificationTitle, notificationContent, notificationType, false);
    }

    /**
     * Constructor for cases without notification or persistence.
     */
    public ProcessedSpecificFunctionalLine(LigneFonctionnelleSpecifique ligneFonctionnelleSpecifique) {
        this(ligneFonctionnelleSpecifique, null, null, null, false);
    }

    // Getters

    public LigneFonctionnelleSpecifique getLigneFonctionnelleSpecifique() {
        return ligneFonctionnelleSpecifique;
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
     */
    public boolean hasNotification() {
        return notificationType != null;
    }

    @Override
    public String toString() {
        return "ProcessedSpecificFunctionalLine{" +
                "ligneFonctionnelleSpecifiqueId=" + (ligneFonctionnelleSpecifique != null ? ligneFonctionnelleSpecifique.getId() : "null") +
                ", notificationType=" + notificationType +
                ", notificationTitle='" + notificationTitle + '\'' +
                ", needsPersistence=" + needsPersistence +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessedSpecificFunctionalLine that = (ProcessedSpecificFunctionalLine) o;
        return needsPersistence == that.needsPersistence &&
                Objects.equals(ligneFonctionnelleSpecifique, that.ligneFonctionnelleSpecifique) &&
                Objects.equals(notificationTitle, that.notificationTitle) &&
                Objects.equals(notificationContent, that.notificationContent) &&
                notificationType == that.notificationType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ligneFonctionnelleSpecifique, notificationTitle, notificationContent,
                notificationType, needsPersistence);
    }
}