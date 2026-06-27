package staport.finance.gBanque.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.user.dtos.request.CreateUserRequest;
import staport.finance.gBanque.user.dtos.request.DepartmentRoleRequest;
import staport.finance.gBanque.user.dtos.request.SetEnabledRequest;
import staport.finance.gBanque.user.dtos.request.SetSystemAdminRequest;
import staport.finance.gBanque.user.dtos.request.UpdatePasswordRequest;
import staport.finance.gBanque.user.dtos.request.UserSearchCriteria;
import staport.finance.gBanque.user.dtos.response.UserDepartmentRoleResponse;
import staport.finance.gBanque.user.dtos.response.UserResponse;
import staport.finance.gBanque.user.enums.DepartmentRoleType;
import staport.finance.gBanque.user.services.UserService;
import staport.finance.gBanque.common.PageableUtils;

import java.util.List;
import java.util.Map;

/**
 * Controller "thin" — aucune entité JPA n'est jamais retournée ; tout le mapping
 * {@code User}/{@code UserDepartmentRole} ↔ DTO se fait dans {@code UserService}
 * via {@code UserMapper}/{@code UserDepartmentRoleMapper}.
 *
 * <p>Module entièrement réservé au {@code SYSTEM_ADMIN} — {@code @PreAuthorize}
 * appliqué une seule fois au niveau classe (même convention que
 * {@code DepartmentController}), y compris {@code /usernames} (utilisé par la
 * page Logs, elle-même admin-only côté frontend).
 */
@Tag(name = "Utilisateurs", description = "Gestion des comptes utilisateurs et de leurs rôles — réservé au SYSTEM_ADMIN")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/users")
@PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ========================================
    // EXISTING ENDPOINT (KEEP AS IS)
    // ========================================

    @Operation(summary = "Liste les usernames distincts", description = "Utilisé par la page Logs pour le filtre par utilisateur.")
    @GetMapping("/usernames")
    public ResponseEntity<List<String>> getDistinctUsernames() {
        return ResponseEntity.ok(userService.getAllDistinctUsernames());
    }

    // ========================================
    // ADMIN ENDPOINTS
    // ========================================

    /**
     * Recherche paginée des utilisateurs.
     * GET /users?page=0&size=10&sort=username,asc&username=...&enabled=...&systemAdmin=...&departmentId=...
     */
    @Operation(summary = "Recherche paginée des utilisateurs")
    @Parameter(name = "username",     description = "Filtre LIKE sur le nom d'utilisateur", example = "ali")
    @Parameter(name = "enabled",      description = "Filtre sur l'état actif/inactif du compte")
    @Parameter(name = "systemAdmin",  description = "Filtre sur le statut SYSTEM_ADMIN")
    @Parameter(name = "departmentId", description = "Filtre sur l'appartenance à un département")
    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String[] sort,
            @ModelAttribute UserSearchCriteria searchCriteria) {

        String sortField = sort[0];
        Sort.Direction direction = (sort.length > 1)
                ? Sort.Direction.fromOptionalString(sort[1]).orElse(Sort.Direction.ASC)
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, PageableUtils.clampSize(size), Sort.by(direction, sortField));
        return ResponseEntity.ok(userService.searchUsers(searchCriteria, pageable));
    }

    @Operation(summary = "Détail d'un utilisateur par ID")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @Operation(summary = "Créer un nouvel utilisateur")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Utilisateur créé"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "409", description = "Ce nom d'utilisateur existe déjà")
    })
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @Operation(summary = "Supprimer un utilisateur")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(Map.of("message", "Utilisateur supprimé avec succès."));
    }

    @Operation(summary = "Rôles département de l'utilisateur")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    @GetMapping("/{userId}/department-roles")
    public ResponseEntity<List<UserDepartmentRoleResponse>> getDepartmentRoles(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getDepartmentRoles(userId));
    }

    @Operation(
            summary = "Assigner un rôle dans un département",
            description = "Comportement upsert : remplace le rôle existant si l'utilisateur est déjà dans le département. " +
                    "roleInDept omis ou null → MEMBER par défaut (principe du moindre privilège)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "404", description = "Utilisateur ou département introuvable")
    })
    @PostMapping("/{userId}/department-roles")
    public ResponseEntity<UserDepartmentRoleResponse> assignDepartmentRole(
            @PathVariable Long userId,
            @Valid @RequestBody DepartmentRoleRequest request) {

        UserDepartmentRoleResponse result = (request.roleInDept() == null || request.roleInDept().isBlank())
                ? userService.assignDepartmentRole(userId, request.departmentId())
                : userService.assignDepartmentRole(userId, request.departmentId(),
                        DepartmentRoleType.valueOf(request.roleInDept().toUpperCase()));
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Retirer l'utilisateur d'un département")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Aucun rôle trouvé pour cet utilisateur dans ce département")
    })
    @DeleteMapping("/{userId}/department-roles/{departmentId}")
    public ResponseEntity<Map<String, String>> removeDepartmentRole(
            @PathVariable Long userId,
            @PathVariable Long departmentId) {
        userService.removeDepartmentRole(userId, departmentId);
        return ResponseEntity.ok(Map.of("message", "Rôle département retiré avec succès."));
    }

    @Operation(summary = "Définir le statut SYSTEM_ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    @PatchMapping("/{userId}/system-admin")
    public ResponseEntity<UserResponse> setSystemAdmin(
            @PathVariable Long userId,
            @Valid @RequestBody SetSystemAdminRequest request) {
        return ResponseEntity.ok(userService.setSystemAdmin(userId, Boolean.TRUE.equals(request.isSystemAdmin())));
    }

    @Operation(summary = "Activer ou désactiver un compte utilisateur")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    @PatchMapping("/{userId}/enabled")
    public ResponseEntity<UserResponse> setUserEnabled(
            @PathVariable Long userId,
            @Valid @RequestBody SetEnabledRequest request) {
        return ResponseEntity.ok(userService.setUserEnabled(userId, Boolean.TRUE.equals(request.enabled())));
    }

    @Operation(summary = "Changer le mot de passe d'un utilisateur")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Mot de passe absent ou trop court"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    @PatchMapping("/{userId}/password")
    public ResponseEntity<Map<String, String>> updatePassword(
            @PathVariable Long userId,
            @Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(userId, request.password());
        return ResponseEntity.ok(Map.of("message", "Mot de passe mis à jour avec succès."));
    }
}
