package staport.finance.gBanque.notification.repos;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.notification.entities.Notification;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    /**
     * Finds all notifications ordered by timestamp in descending order with pagination.
     *
     * @param pageable Pagination and sorting information.
     * @return A paginated list of notifications.
     */
    Page<Notification> findAll(Pageable pageable);

    /**
     * Finds notifications by read status, ordered by timestamp in descending order with pagination.
     *
     * @param readStatus The read status (true for read, false for unread).
     * @param pageable Pagination and sorting information.
     * @return A paginated list of notifications matching the read status.
     */
    Page<Notification> findByReadStatus(boolean readStatus, Pageable pageable);
}
