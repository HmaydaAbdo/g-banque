package staport.finance.gBanque.department.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.department.dtos.DepartmentRequest;
import staport.finance.gBanque.department.dtos.DepartmentResponse;
import staport.finance.gBanque.department.services.DepartmentService;

import java.util.List;

/**
 * Administration des départements métier (Trésorerie, Crédits, Paiements...).
 *
 * <p>Réservé au {@code SYSTEM_ADMIN} — pas de granularité MEMBER/DIRECTOR ici,
 * la gestion des départements eux-mêmes est une action transversale, pas
 * une action de département.
 *
 * <p>Controller "thin" — aucune entité JPA n'est jamais retournée ; tout le
 * mapping {@code Department} ↔ DTO se fait dans {@code DepartmentService}
 * via {@code DepartmentMapper}.
 */
@Tag(name = "Départements", description = "Administration des départements métier — réservé au SYSTEM_ADMIN")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/admin/departments")
@PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @Operation(summary = "Liste tous les départements")
    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @Operation(summary = "Détail d'un département par ID")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Département introuvable")
    })
    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @Operation(summary = "Créer un nouveau département")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Département créé"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "409", description = "Un département avec ce nom existe déjà")
    })
    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentService.createDepartment(request));
    }

    @Operation(summary = "Mettre à jour un département")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "404", description = "Département introuvable"),
            @ApiResponse(responseCode = "409", description = "Un département avec ce nom existe déjà")
    })
    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponse> updateDepartment(@PathVariable Long id,
                                                               @Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, request));
    }

    /**
     * Supprime un département. Échoue avec 409 si des utilisateurs y sont
     * encore assignés (voir {@code DepartmentExceptionHandler}).
     */
    @Operation(
            summary = "Supprimer un département",
            description = "Retourne 409 si des utilisateurs sont encore assignés à ce département — les retirer d'abord."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Département supprimé"),
            @ApiResponse(responseCode = "404", description = "Département introuvable"),
            @ApiResponse(responseCode = "409", description = "Des utilisateurs sont encore assignés à ce département")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }
}
