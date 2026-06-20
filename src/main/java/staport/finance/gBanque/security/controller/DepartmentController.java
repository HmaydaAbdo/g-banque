package staport.finance.gBanque.security.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.security.dtos.DepartmentRequest;
import staport.finance.gBanque.security.dtos.DepartmentResponse;
import staport.finance.gBanque.security.services.DepartmentService;

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
@RestController
@RequestMapping("/admin/departments")
@PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentService.createDepartment(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponse> updateDepartment(@PathVariable Long id,
                                                                 @Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, request));
    }

    /**
     * Supprime un département. Échoue avec 409 si des utilisateurs y sont
     * encore assignés (voir {@code DepartmentExceptionHandler}).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }
}
