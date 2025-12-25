package staport.finance.gBanque.notification.dtos;

import staport.finance.gBanque.notification.entities.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class NotificationTriggerRequest {

  @NotBlank(message = "Title cannot be blank")
  private String title;

  @NotBlank(message = "Content cannot be blank")
  private String content;

  @NotNull(message = "Notification type cannot be null")
  private NotificationType type;

  public NotificationTriggerRequest() {}

  public NotificationTriggerRequest(String title, String content, NotificationType type) {
    this.title = title;
    this.content = content;
    this.type = type;
  }

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public NotificationType getType() { return type; }
  public void setType(NotificationType type) { this.type = type; }
}
