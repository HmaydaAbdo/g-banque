package staport.finance.gBanque.factoring.controller;

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
import staport.finance.gBanque.factoring.dtos.request.EncoursFactoringSearchDto;
import staport.finance.gBanque.factoring.dtos.request.FactoringRequestDto;
import staport.finance.gBanque.factoring.dtos.request.FactoringSearchDto;
import staport.finance.gBanque.factoring.dtos.response.EncoursFactoringDto;
import staport.finance.gBanque.factoring.dtos.response.FactoringResponseDto;
import staport.finance.gBanque.factoring.service.IFactoringService;

import java.time.LocalDate;

/**
 * REST controller for managing Factoring records.
 * Provides a complete CRUD and search API.
 *
 * <p>Injecte {@link IFactoringService} (DIP — jamais la classe concrète).
 *
 * <p>Autorisations (cf. {@code docs/rbac/factoring.md}) : réservé Finance/SYSTEM_ADMIN sur toutes
 * les actions (donnée financière propre au département Finance, mirroring
 * {@code CautionController}), suppression et changement de statut (PREVU/PAYE) réservés
 * SYSTEM_ADMIN.
 */
@Tag(name = "Factoring", description = "Gestion des factorings (déblocages/remboursements) rattachés aux marchés")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/factoring")
public class FactoringController {

    private static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
    private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";

    private final IFactoringService factoringService;

    public FactoringController(IFactoringService factoringService) {
        this.factoringService = factoringService;
    }

    @Operation(summary = "Créer un factoring")
    @PostMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<FactoringResponseDto> createFactoring(@Valid @RequestBody FactoringRequestDto requestDTO) throws Exception {
        FactoringResponseDto responseDTO = factoringService.createFactoring(requestDTO);
        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

    @Operation(summary = "Modifier un factoring")
    @PutMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<FactoringResponseDto> updateFactoring(@PathVariable Long id, @Valid @RequestBody FactoringRequestDto requestDTO) throws Exception {
        FactoringResponseDto responseDTO = factoringService.updateFactoring(id, requestDTO);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Supprimer un factoring", description = "Réservé SYSTEM_ADMIN.")
    @DeleteMapping("/{id}")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<Void> deleteFactoring(@PathVariable Long id) {
        factoringService.deleteFactoring(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Récupérer un factoring par ID")
    @GetMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<FactoringResponseDto> getFactoringById(@PathVariable Long id) {
        FactoringResponseDto responseDTO = factoringService.findById(id);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Recherche paginée des factorings")
    @GetMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<Page<FactoringResponseDto>> searchFactoring(@ModelAttribute FactoringSearchDto searchDTO, @PageableDefault Pageable pageable) {
        Page<FactoringResponseDto> page = factoringService.findAll(searchDTO, pageable);
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Exporter les factorings en PDF",
            description = "Ignore la pagination — exporte tous les résultats filtrés. Mêmes filtres que GET /factoring.")
    @GetMapping("/export/pdf")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<byte[]> exportPdf(@ModelAttribute FactoringSearchDto searchDTO) {
        byte[] pdf = factoringService.exportPdf(searchDTO);
        return pdfResponse(pdf, "factoring");
    }

    @Operation(summary = "Changer le statut PREVU/PAYE", description = "Réservé SYSTEM_ADMIN — cohérent avec la " +
            "correction déjà appliquée aux modules Moyens de paiement/CMT sur ce type d'action.")
    @PatchMapping("/{id}/toggleStatus")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<FactoringResponseDto> toggleStatus(@PathVariable Long id) {
        FactoringResponseDto responseDTO = factoringService.toggleStatus(id);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Consulter les encours de remboursement de factoring d'une banque")
    @GetMapping("/remboursement/encours/{banqueId}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<EncoursFactoringDto> getEncoursForRemboursement(
            @PathVariable Long banqueId,
            @ModelAttribute EncoursFactoringSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        EncoursFactoringDto result = factoringService.getRemboursementEncoursForBanque(searchDto, banqueId, pageable);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Consulter les encours de déblocage de factoring d'une banque")
    @GetMapping("/deblocage/encours/{banqueId}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<EncoursFactoringDto> getEncoursForDeblocage(
            @PathVariable Long banqueId,
            @ModelAttribute EncoursFactoringSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        EncoursFactoringDto result = factoringService.getDeblocageEncoursForBanque(searchDto, banqueId, pageable);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Exporter en PDF l'encours de remboursement de factoring d'une banque",
            description = "Ignore la pagination — exporte tous les résultats filtrés.")
    @GetMapping("/remboursement/encours/{banqueId}/export/pdf")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<byte[]> exportRemboursementEncoursPdf(
            @PathVariable Long banqueId,
            @ModelAttribute EncoursFactoringSearchDto searchDto) {
        byte[] pdf = factoringService.exportRemboursementEncoursPdf(searchDto, banqueId);
        return pdfResponse(pdf, "factoring_encours_remboursement");
    }

    @Operation(summary = "Exporter en PDF l'encours de déblocage de factoring d'une banque",
            description = "Ignore la pagination — exporte tous les résultats filtrés.")
    @GetMapping("/deblocage/encours/{banqueId}/export/pdf")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<byte[]> exportDeblocageEncoursPdf(
            @PathVariable Long banqueId,
            @ModelAttribute EncoursFactoringSearchDto searchDto) {
        byte[] pdf = factoringService.exportDeblocageEncoursPdf(searchDto, banqueId);
        return pdfResponse(pdf, "factoring_encours_deblocage");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filenamePrefix) {
        String filename = filenamePrefix + "_" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
