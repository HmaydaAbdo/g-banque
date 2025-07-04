package staport.finance.gBanque.notification.dtos;

import staport.finance.gBanque.notification.entities.NotificationType;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class NotificationResponse {
    private Long id;
    private String title;
    private String content;
    private NotificationType type;
    private String timestamp;
    private boolean readStatus;

    public NotificationResponse() {
    }

    public NotificationResponse(Long id, String title, String content, NotificationType type, OffsetDateTime timestamp, boolean readStatus) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.type = type;
        this.timestamp = timestamp != null ? timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
        this.readStatus = readStatus;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public boolean isReadStatus() { return readStatus; }
    public void setReadStatus(boolean readStatus) { this.readStatus = readStatus; }
}
