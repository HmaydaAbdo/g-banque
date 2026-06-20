package staport.finance.gBanque.notification.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.notification.entities.Notification;
import staport.finance.gBanque.notification.entities.UserNotificationStatus;
import staport.finance.gBanque.user.entities.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserNotificationStatusRepository extends JpaRepository<UserNotificationStatus, Long> {

    /**
     * 1. Finds the status record for a specific notification and user.
     * Used for marking a single notification as read.
     *
     * @param notification The Notification entity.
     * @param user The User entity.
     * @return An Optional containing the UserNotificationStatus record, if it exists.
     */
    Optional<UserNotificationStatus> findByNotificationAndUser(Notification notification, User user);

    /**
     * 2. Custom query to fetch all existing UserNotificationStatus records for a specific user
     * across a list of public notifications. This is crucial for efficiently determining
     * the read status for a paginated list of public notifications in the service layer.
     *
     * @param notificationIds A list of IDs for the public notifications to check.
     * @param userId The ID of the authenticated user.
     * @return A list of UserNotificationStatus objects.
     */
    @Query("SELECT uns FROM UserNotificationStatus uns WHERE uns.notification.id IN :notificationIds AND uns.user.id = :userId")
    List<UserNotificationStatus> findByUserAndNotificationIds(
            @Param("userId") Long userId,
            @Param("notificationIds") List<Long> notificationIds);

    /**
     * 3. Custom query to find all UserNotificationStatus records for a specific user
     * that match a given read status (e.g., all UNREAD statuses for a user).
     * This is an alternative approach to pagination, though a full history join query is often more efficient.
     *
     * @param user The User entity.
     * @param isRead The read status to filter by (true for read, false for unread).
     * @return A list of UserNotificationStatus objects.
     */
    List<UserNotificationStatus> findByUserAndIsRead(User user, boolean isRead);
}