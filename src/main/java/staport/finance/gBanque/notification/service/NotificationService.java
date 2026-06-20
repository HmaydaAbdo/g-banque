package staport.finance.gBanque.notification.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.notification.dtos.NotificationResponse;
import staport.finance.gBanque.notification.entities.Notification;
import staport.finance.gBanque.notification.entities.NotificationType;
import staport.finance.gBanque.notification.entities.UserNotificationStatus;
import staport.finance.gBanque.notification.repos.NotificationRepository;
import staport.finance.gBanque.notification.repos.UserNotificationStatusRepository;
import staport.finance.gBanque.user.entities.User;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserNotificationStatusRepository userNotificationStatusRepository;

    public NotificationService(
                               NotificationRepository notificationRepository,
                               UserNotificationStatusRepository userNotificationStatusRepository) {
        this.notificationRepository = notificationRepository;
        this.userNotificationStatusRepository = userNotificationStatusRepository;
    }

    /**
     * Sends a public/global notification. ONLY the master Notification record is saved.
     * User status records are created upon first interaction (mark as read).
     */
    @Transactional // Ensures persistence and broadcast are atomic (optional, but good practice)
    public void sendPublicNotification(String title, String content, NotificationType type) {
        // 1. Create and persist the master Notification entity
        Notification notificationToPersist = new Notification(title, content, type);
        notificationRepository.save(notificationToPersist);

    }

    /**
     * Retrieves notifications from the database with pagination and sorting,
     * filtered by the CURRENT USER's status using a performant JPA JOIN query.
     *
     * @param currentUser The authenticated user.
     * @param readStatus If true, gets read; if false, gets unread; if null, gets all.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of NotificationResponse DTOs.
     */
    public Page<NotificationResponse> getPaginatedNotifications(User currentUser, Boolean readStatus, Pageable pageable) {

        // Use the performant custom repository method (assuming it's defined with the LEFT JOIN logic)
        // This query correctly handles NULL status (absence of record) as UNREAD (false).
        Page<Object[]> resultsPage = notificationRepository.findNotificationsWithUserStatus(
                currentUser.getId(),
                readStatus,
                pageable
        );

        List<NotificationResponse> responseList = resultsPage.getContent().stream()
                .map(result -> {
                    Notification notification = (Notification) result[0];
                    Boolean isReadValue = (Boolean) result[1];

                    // If isReadValue is null (no record in UserNotificationStatus), the status is UNREAD (false).
                    boolean isRead = isReadValue != null && isReadValue;

                    return new NotificationResponse(
                            notification.getId(),
                            notification.getTitle(),
                            notification.getContent(),
                            notification.getType(),
                            notification.getTimestamp(),
                            isRead
                    );
                })
                .collect(Collectors.toList());

        // Returns a new PageImpl using the database-filtered content and total count.
        return new PageImpl<>(responseList, pageable, resultsPage.getTotalElements());
    }

    /**
     * Marks a specific notification as read for the CURRENT USER.
     * Creates the UserNotificationStatus record if it doesn't exist.
     *
     * @param notificationId The ID of the notification to mark as read.
     * @param currentUser The authenticated user.
     * @return True if the status was updated/created, false if the notification wasn't found.
     */
    @Transactional
    public boolean markNotificationAsRead(Long notificationId, User currentUser) {
        Optional<Notification> optionalNotification = notificationRepository.findById(notificationId);

        if (optionalNotification.isPresent()) {
            Notification notification = optionalNotification.get();

            // 1. Find the existing status record for this user and notification
            Optional<UserNotificationStatus> optionalStatus = userNotificationStatusRepository
                    .findByNotificationAndUser(notification, currentUser);

            // 2. Get the status record OR create a new one (since it's their first interaction)
            UserNotificationStatus status = optionalStatus.orElseGet(() ->
                    new UserNotificationStatus(notification, currentUser)
            );

            // 3. Only update and save if the status is currently unread
            if (!status.isRead()) {
                status.setRead(true);
                userNotificationStatusRepository.save(status);
                return true;
            }
            // Already marked as read
            return true;
        }
        // Notification itself not found
        return false;
    }
}