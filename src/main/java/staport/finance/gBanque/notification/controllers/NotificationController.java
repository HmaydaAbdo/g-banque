package staport.finance.gBanque.notification.controllers;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.notification.dtos.NotificationResponse;
import staport.finance.gBanque.notification.dtos.NotificationTriggerRequest;
import staport.finance.gBanque.notification.service.NotificationService;
import staport.finance.gBanque.user.entities.User;
import staport.finance.gBanque.user.services.UserService;



@RestController
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    // --- Public Notification Trigger ---

    @PostMapping("/public")
    public ResponseEntity<String> triggerPublicNotification(@Valid @RequestBody NotificationTriggerRequest request) {
        notificationService.sendPublicNotification(request.getTitle(), request.getContent(), request.getType());
        return ResponseEntity.ok("Public " + request.getType().name().toLowerCase() + " notification sent and persisted by System: " + request.getTitle());
    }

    // --- Status-Dependent Endpoints (Modified to Pass User) ---

    /**
     * REST endpoint to retrieve historical public notifications (ALL status).
     * Status is calculated for the current authenticated user.
     */
    @GetMapping("/history")
    public ResponseEntity<Page<NotificationResponse>> getPublicNotificationHistory(@PageableDefault(sort = "timestamp", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        User currentUser = userService.getCurrentUser();
        // Pass the user and null (for all statuses) to the service
        Page<NotificationResponse> history = notificationService.getPaginatedNotifications(currentUser, null, pageable);
        return ResponseEntity.ok(history);
    }

    /**
     * REST endpoint to retrieve historical UNREAD public notifications.
     * Filtered for the current authenticated user.
     */
    @GetMapping("/unread")
    public ResponseEntity<Page<NotificationResponse>> getUnreadNotifications(@PageableDefault(sort = "timestamp", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        // Retrieve the authenticated user
        User currentUser = userService.getCurrentUser();
        // Pass the user and 'false' (for unread) to the service
        Page<NotificationResponse> unreadNotifications = notificationService.getPaginatedNotifications(currentUser, false, pageable);
        return ResponseEntity.ok(unreadNotifications);

    }

    /**
     * REST endpoint to retrieve historical READ public notifications.
     * Filtered for the current authenticated user.
     */
    @GetMapping("/read")
    public ResponseEntity<Page<NotificationResponse>> getReadNotifications(@PageableDefault(sort = "timestamp", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

            // Retrieve the authenticated user
            User currentUser = userService.getCurrentUser();
            // Pass the user and 'true' (for read) to the service
            Page<NotificationResponse> readNotifications = notificationService.getPaginatedNotifications(currentUser, true, pageable);
            return ResponseEntity.ok(readNotifications);

    }


    /**
     * REST endpoint to mark a specific notification as read for the current user.
     */
    @PutMapping("/{id}/mark-as-read")
    public ResponseEntity<String> markNotificationAsRead(@PathVariable Long id) {
        try {
            // Retrieve the authenticated user
            User currentUser = userService.getCurrentUser();

            // Pass the notification ID and the current user to the service
            boolean updated = notificationService.markNotificationAsRead(id, currentUser);

            if (updated) {
                return ResponseEntity.ok("Notification with ID " + id + " marked as read for User " + currentUser.getId() + ".");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Notification with ID " + id + " not found.");
            }
        } catch (Exception e) {
            System.err.println("Error marking notification as read for ID " + id + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error marking notification as read: " + e.getMessage());
        }
    }
}