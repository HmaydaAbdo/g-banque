package staport.finance.gBanque.decomptes.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import staport.finance.gBanque.decomptes.dtos.request.DecompteRequestDto;
import staport.finance.gBanque.decomptes.dtos.request.DecompteSearchDto;
import staport.finance.gBanque.decomptes.dtos.request.EncoursDecompteSearchDto;
import staport.finance.gBanque.decomptes.dtos.response.DecompteResponseDto;
import staport.finance.gBanque.decomptes.dtos.response.EncoursDecompteDto;
import staport.finance.gBanque.decomptes.service.IDecompteService;

/**
 * REST controller for managing décomptes records.
 * Provides a complete CRUD and search API.
 *
 * <p>Injecte {@link IDecompteService} (DIP — jamais la classe concrète).
 *
 * <p>Autorisations (cf. {@code docs/rbac/decomptes.md}) : réservé Finance/SYSTEM_ADMIN sur
 * toutes les actions (donnée financière propre au département Finance, mirroring
 * {@code CautionController}), suppression et changement de statut (PREVU/PAYE) réservés
 * SYSTEM_ADMIN.
 */
@Tag(name = "Décomptes", description = "Gestion des décomptes de travaux rattachés aux marchés")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/decomptes")
public class DecompteController {

    private static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
    private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";

    private final IDecompteService decompteService;

    public DecompteController(IDecompteService decompteService) {
        this.decompteService = decompteService;
    }

    @Operation(summary = "Créer un décompte")
    @PostMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<DecompteResponseDto> createDecompte(@Valid @RequestBody DecompteRequestDto requestDTO) {
        DecompteResponseDto responseDTO = decompteService.createDecompte(requestDTO);
        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

    @Operation(summary = "Modifier un décompte")
    @PutMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<DecompteResponseDto> updateDecompte(@PathVariable Long id, @Valid @RequestBody DecompteRequestDto requestDTO) {
        DecompteResponseDto responseDTO = decompteService.updateDecompte(id, requestDTO);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Supprimer un décompte", description = "Réservé SYSTEM_ADMIN.")
    @DeleteMapping("/{id}")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<Void> deleteDecompte(@PathVariable Long id) {
        decompteService.deleteDecompte(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Récupérer un décompte par ID")
    @GetMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<DecompteResponseDto> getDecompteById(@PathVariable Long id) {
        DecompteResponseDto responseDTO = decompteService.findById(id);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Recherche paginée des décomptes")
    @GetMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<Page<DecompteResponseDto>> searchDecomptes(@ModelAttribute DecompteSearchDto searchDTO, @PageableDefault Pageable pageable) {
        Page<DecompteResponseDto> page = decompteService.findAll(searchDTO, pageable);
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Changer le statut PREVU/PAYE", description = "Réservé SYSTEM_ADMIN.")
    @PatchMapping("/{id}/toggle")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<DecompteResponseDto> toggleStatus(@PathVariable Long id) {
        DecompteResponseDto responseDTO = decompteService.toggleStatus(id);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Exporter les décomptes en PDF",
            description = "Ignore la pagination — exporte tous les résultats filtrés.")
    @GetMapping("/export/pdf")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<byte[]> exportPdf(@ModelAttribute DecompteSearchDto searchDTO) {
        byte[] pdf = decompteService.exportToPdf(searchDTO);
        return pdfResponse(pdf, "decomptes");
    }

    @Operation(summary = "Consulter les encours de décomptes d'une banque")
    @GetMapping("/encours/{banqueId}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<EncoursDecompteDto> getEncoursForDecomptes(
            @PathVariable Long banqueId,
            @ModelAttribute EncoursDecompteSearchDto searchDTO,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        EncoursDecompteDto result = decompteService.getDecomptesEncoursForBanque(searchDTO, banqueId, pageable);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Exporter en PDF l'encours de décomptes d'une banque",
            description = "Ignore la pagination — exporte tous les résultats filtrés.")
    @GetMapping("/encours/{banqueId}/export/pdf")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<byte[]> exportEncoursPdf(
            @PathVariable Long banqueId,
            @ModelAttribute EncoursDecompteSearchDto searchDTO) {
        byte[] pdf = decompteService.exportEncoursPdf(searchDTO, banqueId);
        return pdfResponse(pdf, "decomptes_encours");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filenamePrefix) {
        String filename = filenamePrefix + "_" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
