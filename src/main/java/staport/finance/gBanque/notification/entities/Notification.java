package staport.finance.gBanque.notification.entities;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Notification publique diffusée à tous les utilisateurs.
 *
 * <p>{@code UserNotificationStatus} (statut lu/non-lu propre à chaque utilisateur) est également
 * {@code @Audited} depuis le 16/07/2026 — voir le javadoc de cette classe pour la justification.
 */
@Audited
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private OffsetDateTime timestamp;

    // Default constructor for JPA
    public Notification() {
    }

    // Constructor
    public Notification( String title, String content, NotificationType type) {
        this.title = title;
        this.content = content;
        this.type = type;
        this.timestamp = OffsetDateTime.now(); // Set current timestamp on creation
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
    }

    // Note: isReadStatus() and setReadStatus() are removed.

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", type=" + type +
                ", timestamp=" + timestamp +
                '}';
    }
}