package staport.finance.gBanque.leasing.batch.dto;

import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.notification.entities.NotificationType;

public class ProcessedContratLeasing {

    private final ContratLeasing contratLeasing;
    private final String notificationTitle;
    private final String notificationContent;
    private final NotificationType notificationType;
    private final boolean needsPersistence;

    public ProcessedContratLeasing(
            ContratLeasing contratLeasing,
            String notificationTitle,
            String notificationContent,
            NotificationType notificationType,
            boolean needsPersistence) {
        this.contratLeasing = contratLeasing;
        this.notificationTitle = notificationTitle;
        this.notificationContent = notificationContent;
        this.notificationType = notificationType;
        this.needsPersistence = needsPersistence;
    }

    public ContratLeasing getContratLeasing() {
        return contratLeasing;
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

    public boolean hasNotification() {
        return notificationTitle != null && notificationContent != null && notificationType != null;
    }

    @Override
    public String toString() {
        return "ProcessedContratLeasing{" +
                "contratId=" + contratLeasing.getId() +
                ", numeroContrat='" + contratLeasing.getNumeroContrat() + '\'' +
                ", hasNotification=" + hasNotification() +
                ", needsPersistence=" + needsPersistence +
                '}';
    }
}
