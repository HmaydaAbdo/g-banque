package staport.finance.gBanque.prefinancement.controllers;

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
import staport.finance.gBanque.prefinancement.dtos.request.PrefinancementRequestDTO;
import staport.finance.gBanque.prefinancement.dtos.request.PrefinancementSearchDTO;
import staport.finance.gBanque.prefinancement.dtos.response.PrefinancementResponseDTO;
import staport.finance.gBanque.prefinancement.services.IPrefinancementService;

import java.time.LocalDate;

/**
 * REST controller for managing Prefinancement records.
 * Provides a complete CRUD and search API.
 *
 * <p>Injecte {@link IPrefinancementService} (DIP — jamais la classe concrète).
 *
 * <p>Autorisations (cf. {@code docs/rbac/prefinancements.md}) : réservé Finance/SYSTEM_ADMIN sur
 * toutes les actions (donnée financière propre au département Finance, mirroring
 * {@code CautionController}), suppression et changement de statut (PREVU/PAYE) réservés
 * SYSTEM_ADMIN.
 */
@Tag(name = "Préfinancements", description = "Gestion des préfinancements (déblocages/remboursements) rattachés aux marchés")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/prefinancements")
public class PrefinancementController {

    private static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
    private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";

    private final IPrefinancementService prefinancementService;

    public PrefinancementController(IPrefinancementService prefinancementService) {
        this.prefinancementService = prefinancementService;
    }

    @Operation(summary = "Créer un préfinancement")
    @PostMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<PrefinancementResponseDTO> createPrefinancement(@Valid @RequestBody PrefinancementRequestDTO requestDTO) throws Exception {
        PrefinancementResponseDTO responseDTO = prefinancementService.createPrefinancement(requestDTO);
        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

    @Operation(summary = "Modifier un préfinancement")
    @PutMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<PrefinancementResponseDTO> updatePrefinancement(@PathVariable Long id, @Valid @RequestBody PrefinancementRequestDTO requestDTO) throws Exception {
        PrefinancementResponseDTO responseDTO = prefinancementService.updatePrefinancement(id, requestDTO);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Supprimer un préfinancement", description = "Réservé SYSTEM_ADMIN.")
    @DeleteMapping("/{id}")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<Void> deletePrefinancement(@PathVariable Long id) {
        prefinancementService.deletePrefinancement(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Récupérer un préfinancement par ID")
    @GetMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<PrefinancementResponseDTO> getPrefinancementById(@PathVariable Long id) {
        PrefinancementResponseDTO responseDTO = prefinancementService.findById(id);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Recherche paginée des préfinancements")
    @GetMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<Page<PrefinancementResponseDTO>> searchPrefinancements(@ModelAttribute PrefinancementSearchDTO searchDTO, @PageableDefault Pageable pageable) {
        Page<PrefinancementResponseDTO> page = prefinancementService.findAll(searchDTO, pageable);
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Exporter les préfinancements en PDF",
            description = "Ignore la pagination — exporte tous les résultats filtrés. Mêmes filtres que GET /prefinancements.")
    @GetMapping("/export/pdf")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<byte[]> exportPdf(@ModelAttribute PrefinancementSearchDTO searchDTO) {
        byte[] pdf = prefinancementService.exportPdf(searchDTO);
        return pdfResponse(pdf, "prefinancements");
    }

    @Operation(summary = "Changer le statut PREVU/PAYE", description = "Réservé SYSTEM_ADMIN — cohérent avec la " +
            "correction déjà appliquée aux modules Moyens de paiement/CMT sur ce type d'action.")
    @PatchMapping("/{id}/status/toggle")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<PrefinancementResponseDTO> toggleStatus(@PathVariable Long id) {
        PrefinancementResponseDTO responseDTO = prefinancementService.toggleStatus(id);
        return ResponseEntity.ok(responseDTO);
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filenamePrefix) {
        String filename = filenamePrefix + "_" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
