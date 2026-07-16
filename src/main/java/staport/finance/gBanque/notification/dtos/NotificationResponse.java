package staport.finance.gBanque.notification.dtos;

import staport.finance.gBanque.notification.entities.NotificationType;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DTO de réponse pour une notification, avec son statut lu/non-lu propre à l'utilisateur courant.
 *
 * <p>{@code timestamp} est déjà formaté en chaîne ({@code yyyy-MM-dd HH:mm:ss}) — comportement
 * d'origine conservé (constructeur additionnel prenant un {@link OffsetDateTime} brut, comme avant
 * la conversion en record).
 */
public record NotificationResponse(

        Long id,
        String title,
        String content,
        NotificationType type,
        String timestamp,
        boolean readStatus

) {
    public NotificationResponse(Long id, String title, String content, NotificationType type,
                                 OffsetDateTime rawTimestamp, boolean readStatus) {
        this(id, title, content, type,
                rawTimestamp != null ? rawTimestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null,
                readStatus);
    }
}
