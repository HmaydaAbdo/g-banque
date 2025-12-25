package staport.finance.gBanque.notification.repos;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.notification.entities.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Finds all notifications ordered by timestamp in descending order with pagination.
     * This remains, but the core logic for history retrieval is below.
     *
     * @param pageable Pagination and sorting information.
     * @return A paginated list of notifications.
     */
    Page<Notification> findAll(Pageable pageable);

    /**
     * Finds public notifications with the read status calculated for a specific user,
     * filtered by the requested read status. This is the **performant** solution
     * for displaying the user's notification history.
     *
     * @param userId The ID of the authenticated user.
     * @param readStatus The status to filter by (TRUE, FALSE, or NULL for all).
     * @param pageable Pagination and sorting information.
     * @return A Page of Object[] arrays, where each array contains [Notification entity, isRead boolean].
     */
    @Query(value = "SELECT n, uns.isRead " +
            "FROM Notification n " +
            // LEFT JOIN to get the status record IF it exists for the current user
            "LEFT JOIN UserNotificationStatus uns ON uns.notification.id = n.id AND uns.user.id = :userId " +
            // Complex WHERE clause handles the filtering:
            // 1. If readStatus is NULL (get all), or
            // 2. If filtering for READ (TRUE) AND the status record says it's READ (TRUE), or
            // 3. If filtering for UNREAD (FALSE) AND (the status record is NULL OR it's FALSE)
            "WHERE (:readStatus IS NULL) OR " +
            "      (:readStatus = TRUE AND uns.isRead = TRUE) OR " +
            "      (:readStatus = FALSE AND (uns.isRead IS NULL OR uns.isRead = FALSE))",
            // Separate countQuery is necessary for efficient pagination total count
            countQuery = "SELECT COUNT(n) FROM Notification n " +
                    "LEFT JOIN UserNotificationStatus uns ON uns.notification.id = n.id AND uns.user.id = :userId " +
                    "WHERE (:readStatus IS NULL) OR " +
                    "      (:readStatus = TRUE AND uns.isRead = TRUE) OR " +
                    "      (:readStatus = FALSE AND (uns.isRead IS NULL OR uns.isRead = FALSE))")
    Page<Object[]> findNotificationsWithUserStatus(
            @Param("userId") Long userId,
            @Param("readStatus") Boolean readStatus,
            Pageable pageable
    );
}