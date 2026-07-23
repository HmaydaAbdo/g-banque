package staport.finance.gBanque.restitution.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.restitution.dtos.EncoursRestitution;
import staport.finance.gBanque.restitution.dtos.EncoursRestitutionSearchDto;
import staport.finance.gBanque.restitution.dtos.RestitutionRequestDto;
import staport.finance.gBanque.restitution.dtos.RestitutionResponseDto;
import staport.finance.gBanque.restitution.dtos.RestitutionSearchDto;
import staport.finance.gBanque.restitution.service.IRestitutionService;

import java.time.LocalDate;

/**
 * Injecte {@link IRestitutionService} (DIP — jamais la classe concrète).
 *
 * <p>Autorisations (cf. {@code docs/rbac/restitution.md}) : réservé Finance/SYSTEM_ADMIN sur toutes
 * les actions (donnée financière propre au département Finance, mirroring
 * {@code ImpayeeController}/{@code ImpotEtTaxeController}), suppression et changement de statut
 * (PREVU/PAYE) réservés SYSTEM_ADMIN.
 */
@Tag(name = "Restitutions", description = "Gestion des restitutions rattachées aux banques")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/restitutions")
public class RestitutionController {

    private static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
    private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";

    private static final Logger log = LogManager.getLogger(RestitutionController.class);
    private final IRestitutionService restitutionService;

    public RestitutionController(IRestitutionService restitutionService) {
        this.restitutionService = restitutionService;
    }

    // --- Create ---
    @Operation(summary = "Créer une restitution")
    @PostMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<RestitutionResponseDto> createRestitution(@Validated @RequestBody RestitutionRequestDto requestDto) {
        log.info("Requête REST pour créer une restitution: {}", requestDto);
        RestitutionResponseDto responseDto = restitutionService.createRestitution(requestDto);
        log.info("Restitution créée avec succès: id={}", responseDto.id());
        return ResponseEntity.ok(responseDto);
    }

    // --- Get by ID ---
    @Operation(summary = "Récupérer une restitution par ID")
    @GetMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<RestitutionResponseDto> getById(@PathVariable Long id) {
        log.info("Requête REST pour récupérer la restitution avec l'id={}", id);
        RestitutionResponseDto responseDto = restitutionService.getById(id);
        log.info("Restitution récupérée avec succès: id={}", id);
        return ResponseEntity.ok(responseDto);
    }

    // --- Search with pagination ---
    @Operation(summary = "Recherche paginée des restitutions")
    @PostMapping("/search")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<Page<RestitutionResponseDto>> searchRestitutions(
            @RequestBody RestitutionSearchDto searchDto,
            Pageable pageable) {
        log.info("Requête REST pour rechercher des restitutions avec les critères: {}", searchDto);
        Page<RestitutionResponseDto> page = restitutionService.searchRestitutions(searchDto, pageable);
        log.info("Recherche terminée: {} restitutions trouvées sur {} page(s)",
                page.getTotalElements(), page.getTotalPages());
        return ResponseEntity.ok(page);
    }

    // --- Update ---
    @Operation(summary = "Modifier une restitution")
    @PutMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<RestitutionResponseDto> updateRestitution(
            @PathVariable Long id,
            @Validated @RequestBody RestitutionRequestDto requestDto) {
        log.info("Requête REST pour mettre à jour la restitution id={} avec les données: {}", id, requestDto);
        RestitutionResponseDto responseDto = restitutionService.updateRestitution(id, requestDto);
        log.info("Restitution mise à jour avec succès: id={}", id);
        return ResponseEntity.ok(responseDto);
    }

    // --- Delete ---
    @Operation(summary = "Supprimer une restitution", description = "Réservé SYSTEM_ADMIN. Refusé si la restitution n'est plus au statut PREVU.")
    @DeleteMapping("/{id}")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<Void> deleteRestitution(@PathVariable Long id) {
        log.info("Requête REST pour supprimer la restitution id={}", id);
        restitutionService.deleteRestitution(id);
        log.info("Restitution supprimée avec succès: id={}", id);
        return ResponseEntity.noContent().build();
    }

    // --- Get Outstanding Restitutions ---
    @Operation(summary = "Consulter les encours de restitutions d'une banque")
    @GetMapping("/encours/{banqueId}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<EncoursRestitution> getOutstandingRestitutions(
            @PathVariable Long banqueId,
            @ModelAttribute EncoursRestitutionSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("Requête REST pour récupérer les encours de restitutions pour la banque id={}", banqueId);
        EncoursRestitution result = restitutionService.getOutstandingRestitutions(searchDto, banqueId, pageable);
        log.info("Encours de restitutions récupérés: {} restitutions, total des encours={}",
                result.page().getTotalElements(), result.totalDesEncours());
        return ResponseEntity.ok(result);
    }

    // --- Toggle Status ---
    @Operation(summary = "Changer le statut PREVU/PAYE", description = "Réservé SYSTEM_ADMIN.")
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<RestitutionResponseDto> toggleStatus(@PathVariable Long id) {
        log.info("Requête REST pour basculer le statut de la restitution ID: {}", id);
        RestitutionResponseDto response = restitutionService.toggleStatus(id);
        log.info("Statut de la restitution ID: {} basculé vers {}", id, response.status());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Exporter la liste des restitutions en PDF", description = "Mêmes critères de filtre que /search, sans pagination.")
    @GetMapping("/export/pdf")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<byte[]> exporterPdf(@ModelAttribute RestitutionSearchDto searchDto) {
        byte[] pdf = restitutionService.exporterPdf(searchDto);
        String filename = "restitutions_" + LocalDate.now() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @Operation(summary = "Exporter l'encours de restitutions d'une banque en PDF", description = "Mêmes critères de filtre que l'encours, sans pagination.")
    @GetMapping("/encours/{banqueId}/export/pdf")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<byte[]> exporterEncoursPdf(
            @PathVariable Long banqueId,
            @ModelAttribute EncoursRestitutionSearchDto searchDto) {
        byte[] pdf = restitutionService.exporterEncoursPdf(searchDto, banqueId);
        String filename = "restitutions_encours_" + LocalDate.now() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
