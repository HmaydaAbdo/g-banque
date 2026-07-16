package staport.finance.gBanque.notification.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.notification.dtos.NotificationResponse;
import staport.finance.gBanque.notification.entities.NotificationType;
import staport.finance.gBanque.user.entities.User;

/**
 * Contrat de service pour le module Notifications.
 *
 * <p>Le contrôleur, et tout autre module consommateur (writers batch Leasing/CMT/Lignes
 * Fonctionnelles générant des notifications d'échéance), injectent uniquement cette interface
 * (DIP). La classe concrète {@link NotificationServiceImpl} n'est jamais référencée depuis
 * l'extérieur du module — voir {@code architecture.md}, "Encapsulation des domaines".
 */
public interface INotificationService {

    /**
     * Envoie une notification publique (visible par tous les utilisateurs) et retourne l'ID de la
     * notification créée — nécessaire à {@code @AuditableAction} (l'ID ne peut pas être déduit des
     * paramètres, cf. {@code conventions/Audit.md}, section "Identifiant de l'entité").
     */
    Long sendPublicNotification(String title, String content, NotificationType type);

    Page<NotificationResponse> getPaginatedNotifications(User currentUser, Boolean readStatus, Pageable pageable);

    boolean markNotificationAsRead(Long notificationId, User currentUser);
}
