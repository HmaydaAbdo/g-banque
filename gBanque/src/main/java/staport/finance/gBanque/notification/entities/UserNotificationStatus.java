package staport.finance.gBanque.notification.entities;

import jakarta.persistence.*;
import staport.finance.gBanque.security.entities.User;

@Entity
@Table(name = "user_notification_status",
        uniqueConstraints = {
        // Ensures only one status record exists for a given user and notification
                @UniqueConstraint(columnNames = {"notification_id", "user_id"})
        }
)
public class UserNotificationStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Foreign Key to the public Notification
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    // Foreign Key to the User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // The individual status for this specific user/notification pair
    @Column(nullable = false)
    private boolean isRead = false;

    // --- Constructors ---
    public UserNotificationStatus() {
    }

    public UserNotificationStatus(Notification notification, User user) {
        this.notification = notification;
        this.user = user;
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Notification getNotification() { return notification; }
    public void setNotification(Notification notification) { this.notification = notification; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}