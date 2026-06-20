package staport.finance.gBanque.security.controller;


import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.security.dtos.CreateUserRequest;
import staport.finance.gBanque.security.dtos.DepartmentRoleRequest;
import staport.finance.gBanque.security.dtos.UserDepartmentRoleResponse;
import staport.finance.gBanque.security.dtos.UserResponse;
import staport.finance.gBanque.security.dtos.UserSearchCriteria;
import staport.finance.gBanque.security.enums.DepartmentRoleType;
import staport.finance.gBanque.security.services.UserService;

import java.util.List;
import java.util.Map;

/**
 * Controller "thin" — aucune entité JPA n'est jamais retournée ; tout le mapping
 * {@code User}/{@code UserDepartmentRole} ↔ DTO se fait dans {@code UserService}
 * via {@code UserMapper}/{@code UserDepartmentRoleMapper}.
 */
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
    // ADMIN ENDPOINTS
    // ========================================

    /**
     * Recherche paginée des utilisateurs (Admin système uniquement).
     * GET /users?page=0&size=10&sort=username,asc&username=...&enabled=...&systemAdmin=...&departmentId=...
     */
    @GetMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0")        int page,
            @RequestParam(defaultValue = "10")       int size,
            @RequestParam(defaultValue = "id")       String[] sort,
            @ModelAttribute UserSearchCriteria searchCriteria) {

        String         sortField = sort[0];
        Sort.Direction direction = (sort.length > 1)
                ? Sort.Direction.fromOptionalString(sort[1]).orElse(Sort.Direction.ASC)
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        return ResponseEntity.ok(userService.searchUsers(searchCriteria, pageable));
    }

    /**
     * Get user by ID (Admin only)
     * GET /users/{userId}
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    /**
     * Create a new user (Admin only)
     * POST /users
     * Body: {"username": "john", "password": "pass12345", "enabled": true}
     */
    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    /**
     * Delete a user (Admin only)
     * DELETE /users/{userId}
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok().body(Map.of("message", "User deleted successfully"));
    }

    /**
     * Liste les appartenances département+rôle d'un utilisateur (Admin système uniquement)
     * GET /users/{userId}/department-roles
     */
    @GetMapping("/{userId}/department-roles")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<List<UserDepartmentRoleResponse>> getDepartmentRoles(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getDepartmentRoles(userId));
    }

    /**
     * Assigne (ou met à jour) le rôle d'un utilisateur dans un département (Admin système uniquement)
     * POST /users/{userId}/department-roles
     * Body: {"departmentId": 1, "roleInDept": "MEMBER"} (roleInDept omis → MEMBER par défaut)
     */
    @PostMapping("/{userId}/department-roles")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<UserDepartmentRoleResponse> assignDepartmentRole(
            @PathVariable Long userId,
            @Valid @RequestBody DepartmentRoleRequest request) {

        UserDepartmentRoleResponse result = (request.roleInDept() == null || request.roleInDept().isBlank())
                ? userService.assignDepartmentRole(userId, request.departmentId())
                : userService.assignDepartmentRole(userId, request.departmentId(),
                        DepartmentRoleType.valueOf(request.roleInDept().toUpperCase()));
        return ResponseEntity.ok(result);
    }

    /**
     * Retire un utilisateur d'un département (Admin système uniquement)
     * DELETE /users/{userId}/department-roles/{departmentId}
     */
    @DeleteMapping("/{userId}/department-roles/{departmentId}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, String>> removeDepartmentRole(
            @PathVariable Long userId,
            @PathVariable Long departmentId) {
        userService.removeDepartmentRole(userId, departmentId);
        return ResponseEntity.ok().body(Map.of("message", "Department role removed successfully"));
    }

    /**
     * Définit le statut administrateur système d'un utilisateur (Admin système uniquement)
     * PATCH /users/{userId}/system-admin
     * Body: {"isSystemAdmin": true}
     */
    @PatchMapping("/{userId}/system-admin")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<UserResponse> setSystemAdmin(
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> request) {
        return ResponseEntity.ok(userService.setSystemAdmin(userId, Boolean.TRUE.equals(request.get("isSystemAdmin"))));
    }

    /**
     * Enable or disable a user (Admin only)
     * PATCH /users/{userId}/enabled
     * Body: {"enabled": true}
     */
    @PatchMapping("/{userId}/enabled")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<UserResponse> setUserEnabled(
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> request) {
        return ResponseEntity.ok(userService.setUserEnabled(userId, Boolean.TRUE.equals(request.get("enabled"))));
    }

    /**
     * Update user password (Admin only)
     * PATCH /users/{userId}/password
     * Body: {"password": "newPassword123"}
     */
    @PatchMapping("/{userId}/password")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, String>> updatePassword(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        userService.updatePassword(userId, request.get("password"));
        return ResponseEntity.ok().body(Map.of("message", "Password updated successfully"));
    }

}
