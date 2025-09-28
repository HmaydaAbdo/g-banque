package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.jobs.dtos;

import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.notification.entities.NotificationType;

/**
 * DTO to transfer processed specific functional line data from the ItemProcessor to the ItemWriter.
 */
public class ProcessedSpecificFunctionalLine {

    private LigneFonctionnelleSpecifique ligneFonctionnelleSpecifique;
    private String notificationTitle;
    private String notificationContent;
    private NotificationType notificationType;


    public ProcessedSpecificFunctionalLine(LigneFonctionnelleSpecifique ligneFonctionnelleSpecifique, String notificationTitle, String notificationContent, NotificationType notificationType) {
        this.ligneFonctionnelleSpecifique = ligneFonctionnelleSpecifique;
        this.notificationTitle = notificationTitle;
        this.notificationContent = notificationContent;
        this.notificationType = notificationType;

    }


    public LigneFonctionnelleSpecifique getLigneFonctionnelleSpecifique() {
        return ligneFonctionnelleSpecifique;
    }

    public void setLigneFonctionnelleSpecifique(LigneFonctionnelleSpecifique ligneFonctionnelleSpecifique) {
        this.ligneFonctionnelleSpecifique = ligneFonctionnelleSpecifique;
    }

    public String getNotificationTitle() {
        return notificationTitle;
    }

    public void setNotificationTitle(String notificationTitle) {
        this.notificationTitle = notificationTitle;
    }

    public String getNotificationContent() {
        return notificationContent;
    }

    public void setNotificationContent(String notificationContent) {
        this.notificationContent = notificationContent;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }



    /**
     * Checks if this processed functional line carries notification details.
     *
     * @return true if a notification type is set, false otherwise.
     */
    public boolean hasNotification() {
        return notificationType != null;
    }


}