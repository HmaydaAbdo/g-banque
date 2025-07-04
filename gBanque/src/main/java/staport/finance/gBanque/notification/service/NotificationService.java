package staport.finance.gBanque.notification.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import staport.finance.gBanque.notification.dtos.NotificationResponse;
import staport.finance.gBanque.notification.entities.Notification;
import staport.finance.gBanque.notification.entities.NotificationType;
import staport.finance.gBanque.notification.repos.NotificationRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;

    public NotificationService(SimpMessagingTemplate messagingTemplate, NotificationRepository notificationRepository) {
        this.messagingTemplate = messagingTemplate;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Sends a public/global notification to all clients subscribed to a specific topic
     * and persists the notification in the database. The sender is always "System".
     * Clients will subscribe to `/topic/public-notifications`.
     *
     * @param title A brief title for the notification
     * @param content The main message body
     * @param type The type of notification (INFO, WARNING, ALERT, SUCCESS)
     */
    public void sendPublicNotification(String title, String content, NotificationType type) {
        // Create and persist the Notification entity
        Notification notificationToPersist = new Notification(title, content, type);
        notificationRepository.save(notificationToPersist);

        // Convert the persisted Notification to NotificationResponse for WebSocket broadcast
        NotificationResponse response = new NotificationResponse(
                notificationToPersist.getId(),
                notificationToPersist.getTitle(),
                notificationToPersist.getContent(),
                notificationToPersist.getType(),
                notificationToPersist.getTimestamp(),
                notificationToPersist.isReadStatus()
        );

        // Send to the specified topic. All clients subscribed to "/topic/public-notifications" will receive this.
        messagingTemplate.convertAndSend("/topic/public-notifications", response);
        System.out.println("Sent and persisted public notification: " + notificationToPersist);
    }

    /**
     * Retrieves public notifications from the database with pagination and sorting.
     * Optionally filters by read status.
     *
     * @param readStatus If true, gets read notifications; if false, gets unread; if null, gets all.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of NotificationResponse DTOs.
     */
    public Page<NotificationResponse> getPaginatedNotifications(Boolean readStatus, Pageable pageable) {
        Page<Notification> notificationsPage;
        if (readStatus != null) {
            notificationsPage = notificationRepository.findByReadStatus(readStatus, pageable);
        } else {
            notificationsPage = notificationRepository.findAll(pageable);
        }

        List<NotificationResponse> responseList = notificationsPage.getContent().stream()
                .map(notification -> new NotificationResponse(
                        notification.getId(),
                        notification.getTitle(),
                        notification.getContent(),
                        notification.getType(),
                        notification.getTimestamp(),
                        notification.isReadStatus()
                ))
                .collect(Collectors.toList());

        return new PageImpl<>(responseList, pageable, notificationsPage.getTotalElements());
    }

    /**
     * Marks a specific notification as read.
     *
     * @param notificationId The ID of the notification to mark as read.
     * @return True if the notification was found and marked as read, false otherwise.
     */
    public boolean markNotificationAsRead(Long notificationId) {
        Optional<Notification> optionalNotification = notificationRepository.findById(notificationId);
        if (optionalNotification.isPresent()) {
            Notification notification = optionalNotification.get();
            notification.setReadStatus(true);
            notificationRepository.save(notification);
            return true;
        }
        return false;
    }
}
