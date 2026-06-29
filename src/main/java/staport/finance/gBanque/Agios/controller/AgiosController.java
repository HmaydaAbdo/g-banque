package staport.finance.gBanque.Agios.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.Agios.dtos.AgiosRequestDto;
import staport.finance.gBanque.Agios.dtos.AgiosResponseDto;
import staport.finance.gBanque.Agios.dtos.AgiosSearchDto;
import staport.finance.gBanque.Agios.dtos.EncoursAgiosDto;
import staport.finance.gBanque.Agios.service.AgiosService;

/**
 * Contrôleur REST du module Agios.
 *
 * <p>Thin controller — délègue toute la logique à {@link AgiosService}.
 * La gestion des exceptions est déléguée à
 * {@link staport.finance.gBanque.Agios.exceptions.AgiosExceptionHandler}.
 *
 * <h2>RBAC</h2>
 * <ul>
 *   <li>Lecture, création, modification — {@code DEPT_Finance:MEMBER}, {@code DEPT_Finance:DIRECTOR} ou {@code SYSTEM_ADMIN}</li>
 *   <li>Suppression et toggle statut — {@code SYSTEM_ADMIN} uniquement</li>
 * </ul>
 */
@Tag(name = "Agios", description = "Gestion des agios bancaires")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/agios")
public class AgiosController {

    private static final String FINANCE_OR_ADMIN =
            "hasAnyAuthority('DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR', 'SYSTEM_ADMIN')";
    private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";

    private final AgiosService agiosService;

    public AgiosController(AgiosService agiosService) {
        this.agiosService = agiosService;
    }

    // ── Création ──────────────────────────────────────────────────────────────

    @Operation(summary = "Créer un agios")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Agios créé"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "404", description = "Banque introuvable")
    })
    @PostMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<AgiosResponseDto> createAgios(@Valid @RequestBody AgiosRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(agiosService.createAgios(dto));
    }

    // ── Lecture par ID ────────────────────────────────────────────────────────

    @Operation(summary = "Récupérer un agios par ID")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Agios introuvable")
    })
    @GetMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<AgiosResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(agiosService.getById(id));
    }

    // ── Recherche paginée ─────────────────────────────────────────────────────

    @Operation(summary = "Rechercher des agios")
    @PostMapping("/search")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<Page<AgiosResponseDto>> searchAgios(
            @RequestBody AgiosSearchDto dto,
            Pageable pageable) {
        return ResponseEntity.ok(agiosService.searchAgios(dto, pageable));
    }

    // ── Modification ──────────────────────────────────────────────────────────

    @Operation(summary = "Modifier un agios")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "404", description = "Agios ou banque introuvable")
    })
    @PutMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<AgiosResponseDto> updateAgios(
            @PathVariable Long id,
            @Valid @RequestBody AgiosRequestDto dto) {
        return ResponseEntity.ok(agiosService.updateAgios(id, dto));
    }

    // ── Suppression ───────────────────────────────────────────────────────────

    @Operation(summary = "Supprimer un agios")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Agios supprimé"),
            @ApiResponse(responseCode = "404", description = "Agios introuvable")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<Void> deleteAgios(@PathVariable Long id) {
        agiosService.deleteAgios(id);
        return ResponseEntity.noContent().build();
    }

    // ── Encours ───────────────────────────────────────────────────────────────

    @Operation(summary = "Récupérer les agios en cours d'une banque")
    @GetMapping("/encours/{banqueId}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<EncoursAgiosDto> getOutstandingAgios(
            @PathVariable Long banqueId,
            @RequestParam(required = false) String libelle,
            @RequestParam(required = false) java.math.BigDecimal montant,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        AgiosSearchDto criteria = new AgiosSearchDto(banqueId, libelle, montant, null);
        return ResponseEntity.ok(agiosService.getOutstandingAgios(criteria, pageable));
    }

    // ── Toggle statut ─────────────────────────────────────────────────────────

    @Operation(summary = "Basculer le statut d'un agios (PREVU ↔ PAYE)")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Agios introuvable")
    })
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<AgiosResponseDto> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(agiosService.toggleStatus(id));
    }
}
