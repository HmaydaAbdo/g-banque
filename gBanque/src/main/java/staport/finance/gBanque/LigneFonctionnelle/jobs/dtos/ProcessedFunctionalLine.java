
package staport.finance.gBanque.LigneFonctionnelle.jobs.dtos;

import staport.finance.gBanque.LigneFonctionnelle.entities.LigneFonctionnelle;
import staport.finance.gBanque.notification.entities.NotificationType;



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
    private LigneFonctionnelle ligneFonctionnelle;
    private String notificationTitle;
    private String notificationContent;
    private NotificationType notificationType;


    /**
     * Constructor for cases where a functional line is processed and a notification needs to be generated,
     * and/or the functional line needs to be persisted (e.g., if its 'actif' status changed).
     *
     * @param ligneFonctionnelle The LigneFonctionnelle entity, potentially modified (e.g., status changed).
     * @param notificationTitle The title for the notification.
     * @param notificationContent The content/body for the notification.
     * @param notificationType The type of notification (e.g., ALERT, WARNING).
     */
    public ProcessedFunctionalLine(LigneFonctionnelle ligneFonctionnelle, String notificationTitle, String notificationContent, NotificationType notificationType ) {
        this.ligneFonctionnelle = ligneFonctionnelle;
        this.notificationTitle = notificationTitle;
        this.notificationContent = notificationContent;
        this.notificationType = notificationType;

    }

    /**
     * Constructor for cases where a functional line is processed but no specific notification is required,
     * and the functional line does NOT need to be explicitly persisted by the writer
     * (e.g., if only `dateModification` would be updated, which batch can handle implicitly if the entity is returned).
     * This is for clarity when no *specific* persistence action beyond standard batch processing is implied.
     *
     * @param ligneFonctionnelle The LigneFonctionnelle entity, potentially modified.
     */
    public ProcessedFunctionalLine(LigneFonctionnelle ligneFonctionnelle ) {
        this.ligneFonctionnelle = ligneFonctionnelle;

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
                '}';
    }
}
