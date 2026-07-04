package staport.finance.gBanque.cmt.batch.dto;

import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;
import staport.finance.gBanque.notification.entities.NotificationType;

public class ProcessedCreditMoyenTerme {

    private final CreditMoyenTerme creditMoyenTerme;
    private final String notificationTitle;
    private final String notificationContent;
    private final NotificationType notificationType;

    public ProcessedCreditMoyenTerme(
            CreditMoyenTerme creditMoyenTerme,
            String notificationTitle,
            String notificationContent,
            NotificationType notificationType) {
        this.creditMoyenTerme = creditMoyenTerme;
        this.notificationTitle = notificationTitle;
        this.notificationContent = notificationContent;
        this.notificationType = notificationType;
    }

    public CreditMoyenTerme getCreditMoyenTerme() {
        return creditMoyenTerme;
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

    public boolean hasNotification() {
        return notificationTitle != null && notificationContent != null && notificationType != null;
    }

    @Override
    public String toString() {
        return "ProcessedCreditMoyenTerme{" +
                "creditId=" + creditMoyenTerme.getId() +
                ", designation='" + creditMoyenTerme.getDesignation() + '\'' +
                ", hasNotification=" + hasNotification() +
                '}';
    }
}