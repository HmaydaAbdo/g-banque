package staport.finance.gBanque.notification.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.notification.dtos.NotificationResponse;
import staport.finance.gBanque.notification.dtos.NotificationTriggerRequest;
import staport.finance.gBanque.notification.service.INotificationService;
import staport.finance.gBanque.user.entities.User;
import staport.finance.gBanque.user.services.UserService;

/**
 * Injecte {@link INotificationService} (DIP — jamais la classe concrète).
 *
 * <p>Autorisations (cf. {@code docs/rbac/notifications.md}) : le déclenchement manuel d'une
 * notification publique est réservé {@code SYSTEM_ADMIN} (action d'administration) ; la
 * consultation et le marquage comme lue de ses propres notifications sont ouverts à tout
 * utilisateur authentifié (donnée personnelle, pas une donnée financière Finance).
 */
@Tag(name = "Notifications", description = "Notifications applicatives publiques")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/notification")
public class NotificationController {

    private static final String AUTHENTICATED = "isAuthenticated()";
    private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final INotificationService notificationService;
    private final UserService userService;

    public NotificationController(INotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @Operation(summary = "Déclencher une notification publique", description = "Réservé SYSTEM_ADMIN.")
    @PostMapping("/public")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<String> triggerPublicNotification(@Valid @RequestBody NotificationTriggerRequest request) {
        notificationService.sendPublicNotification(request.title(), request.content(), request.type());
        return ResponseEntity.ok("Public " + request.type().name().toLowerCase() + " notification sent and persisted by System: " + request.title());
    }

    @Operation(summary = "Historique de mes notifications (tous statuts)")
    @GetMapping("/history")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<Page<NotificationResponse>> getPublicNotificationHistory(@PageableDefault(sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        User currentUser = userService.getCurrentUser();
        Page<NotificationResponse> history = notificationService.getPaginatedNotifications(currentUser, null, pageable);
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "Mes notifications non lues")
    @GetMapping("/unread")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<Page<NotificationResponse>> getUnreadNotifications(@PageableDefault(sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        User currentUser = userService.getCurrentUser();
        Page<NotificationResponse> unreadNotifications = notificationService.getPaginatedNotifications(currentUser, false, pageable);
        return ResponseEntity.ok(unreadNotifications);
    }

    @Operation(summary = "Mes notifications lues")
    @GetMapping("/read")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<Page<NotificationResponse>> getReadNotifications(@PageableDefault(sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        User currentUser = userService.getCurrentUser();
        Page<NotificationResponse> readNotifications = notificationService.getPaginatedNotifications(currentUser, true, pageable);
        return ResponseEntity.ok(readNotifications);
    }

    @Operation(summary = "Marquer une notification comme lue")
    @PutMapping("/{id}/mark-as-read")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<String> markNotificationAsRead(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        Long statusId = notificationService.markNotificationAsRead(id, currentUser);

        if (statusId == null) {
            log.warn("Notification introuvable — id: {}", id);
            throw new ResourceNotFoundException("Notification introuvable avec l'ID: " + id);
        }
        return ResponseEntity.ok("Notification with ID " + id + " marked as read for User " + currentUser.getId() + ".");
    }
}
