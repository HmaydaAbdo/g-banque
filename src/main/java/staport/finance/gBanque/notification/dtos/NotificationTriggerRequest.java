package staport.finance.gBanque.notification.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import staport.finance.gBanque.notification.entities.NotificationType;

/**
 * DTO de requête pour déclencher manuellement une notification publique.
 */
public record NotificationTriggerRequest(

        @NotBlank(message = "Title cannot be blank")
        String title,

        @NotBlank(message = "Content cannot be blank")
        String content,

        @NotNull(message = "Notification type cannot be null")
        NotificationType type

) {}
