package staport.finance.gBanque.security.controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.security.dtos.CreateUserRequest;
import staport.finance.gBanque.security.dtos.RoleRequest;
import staport.finance.gBanque.security.entities.User;
import staport.finance.gBanque.security.enums.RoleName;
import staport.finance.gBanque.security.services.UserService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ========================================
    // EXISTING ENDPOINT (KEEP AS IS)
    // ========================================

    @GetMapping("/usernames")
    public ResponseEntity<List<String>> getDistinctUsernames() {
        return ResponseEntity.ok(userService.getAllDistinctUsernames());
    }

    // ========================================
    // NEW ADMIN ENDPOINTS
    // ========================================

    /**
     * Get all users (Admin only)
     * GET /users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Get user by ID (Admin only)
     * GET /users/{userId}
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> getUserById(@PathVariable Long userId) {
        return userService.findUserById(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new user (Admin only)
     * POST /users
     * Body: {"username": "john", "password": "pass123", "enabled": true}
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        try {
            User user = userService.createUser(
                    request.getUsername(),
                    request.getPassword(),
                    request.isEnabled()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete a user (Admin only)
     * DELETE /users/{userId}
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        try {
            userService.deleteUser(userId);
            return ResponseEntity.ok().body(Map.of("message", "User deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Add a role to a user (Admin only)
     * POST /users/{userId}/roles
     * Body: {"roleId": 1} OR {"roleName": "ADMIN"}
     */
    @PostMapping("/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addRoleToUser(
            @PathVariable Long userId,
            @RequestBody RoleRequest request) {
        try {
            User user;
            if (request.getRoleId() != null) {
                user = userService.addRoleToUser(userId, request.getRoleId());
            } else if (request.getRoleName() != null) {
                RoleName roleName = RoleName.valueOf(request.getRoleName());
                user = userService.addRoleToUser(userId, roleName);
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Either roleId or roleName must be provided"));
            }
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Remove a role from a user (Admin only)
     * DELETE /users/{userId}/roles
     * Body: {"roleId": 1} OR {"roleName": "ADMIN"}
     */
    @DeleteMapping("/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> removeRoleFromUser(
            @PathVariable Long userId,
            @RequestBody RoleRequest request) {
        try {
            User user;
            if (request.getRoleId() != null) {
                user = userService.removeRoleFromUser(userId, request.getRoleId());
            } else if (request.getRoleName() != null) {
                RoleName roleName = RoleName.valueOf(request.getRoleName());
                user = userService.removeRoleFromUser(userId, roleName);
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Either roleId or roleName must be provided"));
            }
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Enable or disable a user (Admin only)
     * PATCH /users/{userId}/enabled
     * Body: {"enabled": true}
     */
    @PatchMapping("/{userId}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> setUserEnabled(
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> request) {
        try {
            User user = userService.setUserEnabled(userId, request.get("enabled"));
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update user password (Admin only)
     * PATCH /users/{userId}/password
     * Body: {"password": "newPassword123"}
     */
    @PatchMapping("/{userId}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePassword(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        try {
            userService.updatePassword(userId, request.get("password"));
            return ResponseEntity.ok().body(Map.of("message", "Password updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

}