package staport.finance.gBanque.droitConstate.controller;

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
import staport.finance.gBanque.droitConstate.dtos.request.DroitConstateRequestDto;
import staport.finance.gBanque.droitConstate.dtos.request.DroitConstateSearchDto;
import staport.finance.gBanque.droitConstate.dtos.response.DroitConstateResponseDto;
import staport.finance.gBanque.droitConstate.service.IDroitConstateService;

import java.time.LocalDate;

/**
 * REST controller for managing Droit Constaté records.
 * Provides a complete CRUD and search API.
 *
 * <p>Injecte {@link IDroitConstateService} (DIP — jamais la classe concrète).
 *
 * <p>Autorisations (cf. {@code docs/rbac/droits-constate.md}) : réservé Finance/SYSTEM_ADMIN sur
 * toutes les actions (donnée financière propre au département Finance, mirroring
 * {@code CautionController}), suppression et changement de statut (PREVU/PAYE) réservés
 * SYSTEM_ADMIN.
 */
@Tag(name = "Droits Constatés", description = "Gestion des droits constatés (déblocages/remboursements) rattachés aux marchés")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/droitsConstate")
public class DroitConstateController {

    private static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
    private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";

    private final IDroitConstateService droitConstateService;

    public DroitConstateController(IDroitConstateService droitConstateService) {
        this.droitConstateService = droitConstateService;
    }

    @Operation(summary = "Créer un droit constaté")
    @PostMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<DroitConstateResponseDto> createDroitConstate(@Valid @RequestBody DroitConstateRequestDto requestDTO) throws Exception {
        DroitConstateResponseDto responseDTO = droitConstateService.createDroitConstate(requestDTO);
        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

    @Operation(summary = "Modifier un droit constaté")
    @PutMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<DroitConstateResponseDto> updateDroitConstate(@PathVariable Long id, @Valid @RequestBody DroitConstateRequestDto requestDTO) throws Exception {
        DroitConstateResponseDto responseDTO = droitConstateService.updateDroitConstate(id, requestDTO);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Supprimer un droit constaté", description = "Réservé SYSTEM_ADMIN.")
    @DeleteMapping("/{id}")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<Void> deleteDroitConstate(@PathVariable Long id) {
        droitConstateService.deleteDroitConstate(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Récupérer un droit constaté par ID")
    @GetMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<DroitConstateResponseDto> getDroitConstateById(@PathVariable Long id) {
        DroitConstateResponseDto responseDTO = droitConstateService.findById(id);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Recherche paginée des droits constatés")
    @GetMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<Page<DroitConstateResponseDto>> searchDroitConstates(@ModelAttribute DroitConstateSearchDto searchDTO, @PageableDefault Pageable pageable) {
        Page<DroitConstateResponseDto> page = droitConstateService.findAll(searchDTO, pageable);
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Exporter les droits constatés en PDF",
            description = "Ignore la pagination — exporte tous les résultats filtrés. Mêmes filtres que GET /droitsConstate.")
    @GetMapping("/export/pdf")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<byte[]> exportPdf(@ModelAttribute DroitConstateSearchDto searchDTO) {
        byte[] pdf = droitConstateService.exportPdf(searchDTO);
        return pdfResponse(pdf, "droits_constates");
    }

    @Operation(summary = "Changer le statut PREVU/PAYE", description = "Réservé SYSTEM_ADMIN — cohérent avec la " +
            "correction déjà appliquée aux modules Moyens de paiement/CMT sur ce type d'action.")
    @PatchMapping("/{id}/status/toggle")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<DroitConstateResponseDto> toggleStatus(@PathVariable Long id) {
        DroitConstateResponseDto responseDTO = droitConstateService.toggleStatus(id);
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
