package staport.finance.gBanque.notification.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.notification.dtos.NotificationResponse;
import staport.finance.gBanque.notification.entities.Notification;
import staport.finance.gBanque.notification.entities.NotificationType;
import staport.finance.gBanque.notification.entities.UserNotificationStatus;
import staport.finance.gBanque.notification.repos.NotificationRepository;
import staport.finance.gBanque.notification.repos.UserNotificationStatusRepository;
import staport.finance.gBanque.user.entities.User;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implémentation du service Notifications.
 *
 * <p>Renommé depuis {@code NotificationService} (sweep de conformité) pour respecter la convention
 * {@code IXxxService}/{@code XxxServiceImpl} — le contrôleur et les 4 writers batch
 * (Leasing/CMT/Lignes Fonctionnelles générale et spécifique) injectaient auparavant la classe
 * concrète directement.
 */
@Service
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final UserNotificationStatusRepository userNotificationStatusRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                    UserNotificationStatusRepository userNotificationStatusRepository) {
        this.notificationRepository = notificationRepository;
        this.userNotificationStatusRepository = userNotificationStatusRepository;
    }

    /**
     * Envoie une notification publique/globale. Seule l'entité maîtresse {@code Notification} est
     * enregistrée — les statuts par utilisateur sont créés à la première interaction (marquer comme
     * lue).
     */
    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = Notification.class)
    public Long sendPublicNotification(String title, String content, NotificationType type) {
        Notification notificationToPersist = new Notification(title, content, type);
        Notification saved = notificationRepository.save(notificationToPersist);
        return saved.getId();
    }

    /**
     * Récupère les notifications, paginées et triées, filtrées par le statut de l'utilisateur
     * courant via une requête JPA performante (JOIN).
     *
     * @param currentUser L'utilisateur authentifié.
     * @param readStatus  Si {@code true}, ne retourne que les lues ; si {@code false}, que les
     *                    non-lues ; si {@code null}, toutes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getPaginatedNotifications(User currentUser, Boolean readStatus, Pageable pageable) {

        Page<Object[]> resultsPage = notificationRepository.findNotificationsWithUserStatus(
                currentUser.getId(),
                readStatus,
                pageable
        );

        List<NotificationResponse> responseList = resultsPage.getContent().stream()
                .map(result -> {
                    Notification notification = (Notification) result[0];
                    Boolean isReadValue = (Boolean) result[1];

                    // Si isReadValue est null (aucun enregistrement dans UserNotificationStatus), le statut est NON-LU (false).
                    boolean isRead = isReadValue != null && isReadValue;

                    return new NotificationResponse(
                            notification.getId(),
                            notification.getTitle(),
                            notification.getContent(),
                            notification.getType(),
                            notification.getTimestamp(),
                            isRead
                    );
                })
                .collect(Collectors.toList());

        return new PageImpl<>(responseList, pageable, resultsPage.getTotalElements());
    }

    /**
     * Marque une notification comme lue pour l'utilisateur courant. Crée l'enregistrement
     * {@code UserNotificationStatus} s'il n'existe pas encore (première interaction).
     *
     * <p>Comportement "upsert" (create ou update selon le cas) : {@code AuditOperation.UPDATE} par
     * convention, mirroring {@code UserService.assignDepartmentRole(...)} (voir
     * {@code audit/AuditOperation.java}). Retourne l'ID de la ligne {@code UserNotificationStatus}
     * touchée (nécessaire à {@code @AuditableAction} — {@code GenericAuditAspect} l'utilise
     * directement, cf. javadoc {@code extractIdFromResult}), ou {@code null} si la notification
     * n'existe pas.
     */
    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = UserNotificationStatus.class)
    public Long markNotificationAsRead(Long notificationId, User currentUser) {
        Optional<Notification> optionalNotification = notificationRepository.findById(notificationId);

        if (optionalNotification.isEmpty()) {
            return null;
        }
        Notification notification = optionalNotification.get();

        UserNotificationStatus status = userNotificationStatusRepository
                .findByNotificationAndUser(notification, currentUser)
                .orElseGet(() -> new UserNotificationStatus(notification, currentUser));

        status.setRead(true);
        UserNotificationStatus saved = userNotificationStatusRepository.save(status);
        return saved.getId();
    }
}
