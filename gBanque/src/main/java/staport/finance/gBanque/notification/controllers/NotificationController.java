package staport.finance.gBanque.notification.controllers;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.notification.dtos.NotificationResponse;
import staport.finance.gBanque.notification.dtos.NotificationTriggerRequest;
import staport.finance.gBanque.notification.entities.NotificationType;
import staport.finance.gBanque.notification.service.NotificationService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * REST endpoint to trigger a public notification using a request body.
     * The notification will be broadcasted via WebSocket and persisted in the database.
     * Body: { "title": "System Update", "content": "New features are available!", "type": "INFO" }
     *
     * @param request The request body containing notification details.
     * @param bindingResult Used to check for validation errors.
     * @return ResponseEntity indicating success or failure.
     */
    @PostMapping("/public")
    public ResponseEntity<String> triggerPublicNotification(
            @Valid @RequestBody NotificationTriggerRequest request,
            BindingResult bindingResult) {
        // Handle validation errors
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining("; "));
            return ResponseEntity.badRequest().body("Validation failed: " + errors);
        }

        try {

            notificationService.sendPublicNotification(
                    request.getTitle(),
                    request.getContent(),
                    request.getType()
            );
            return ResponseEntity.ok("Public " + request.getType().name().toLowerCase() + " notification sent and persisted by System: " + request.getTitle());
        } catch (Exception e) {
            System.err.println("Error triggering public notification: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error triggering public notification: " + e.getMessage());
        }
    }

    /**
     * REST endpoint to retrieve historical public notifications with pagination and sorting.
     * Notifications are ordered by timestamp, most recent first by default.
     *
     * @param pageable Pagination and sorting information provided by Spring Data.
     * @return A paginated list of NotificationResponse DTOs.
     */
    @GetMapping("/history")
    public ResponseEntity<Page<NotificationResponse>> getPublicNotificationHistory(
            @PageableDefault(sort = "timestamp", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        try {
            Page<NotificationResponse> history = notificationService.getPaginatedNotifications(null, pageable);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            System.err.println("Error retrieving public notification history: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Page.empty());
        }
    }

    /**
     * REST endpoint to retrieve historical unread public notifications with pagination and sorting.
     * Notifications are ordered by timestamp, most recent first by default.
     *
     * @param pageable Pagination and sorting information provided by Spring Data.
     * @return A paginated list of NotificationResponse DTOs for unread notifications.
     */
    @GetMapping("/unread")
    public ResponseEntity<Page<NotificationResponse>> getUnreadNotifications(
            @PageableDefault(sort = "timestamp", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        try {
            Page<NotificationResponse> unreadNotifications = notificationService.getPaginatedNotifications(false, pageable);
            return ResponseEntity.ok(unreadNotifications);
        } catch (Exception e) {
            System.err.println("Error retrieving unread notifications: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Page.empty());
        }
    }

    /**
     * REST endpoint to retrieve historical read public notifications with pagination and sorting.
     * Notifications are ordered by timestamp, most recent first by default.
     *
     * @param pageable Pagination and sorting information provided by Spring Data.
     * @return A paginated list of NotificationResponse DTOs for read notifications.
     */
    @GetMapping("/read")
    public ResponseEntity<Page<NotificationResponse>> getReadNotifications(
            @PageableDefault(sort = "timestamp", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        try {
            Page<NotificationResponse> readNotifications = notificationService.getPaginatedNotifications(true, pageable);
            return ResponseEntity.ok(readNotifications);
        } catch (Exception e) {
            System.err.println("Error retrieving read notifications: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Page.empty());
        }
    }

    /**
     * REST endpoint to mark a specific notification as read.
     *
     * @param id The ID of the notification to mark as read.
     * @return ResponseEntity indicating success or if the notification was not found.
     */
    @PutMapping("/{id}/mark-as-read")
    public ResponseEntity<String> markNotificationAsRead(@PathVariable Long id) {
        try {
            boolean updated = notificationService.markNotificationAsRead(id);
            if (updated) {
                return ResponseEntity.ok("Notification with ID " + id + " marked as read.");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Notification with ID " + id + " not found.");
            }
        } catch (Exception e) {
            System.err.println("Error marking notification as read for ID " + id + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error marking notification as read: " + e.getMessage());
        }
    }
}
