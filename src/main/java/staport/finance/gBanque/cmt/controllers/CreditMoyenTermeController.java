package staport.finance.gBanque.cmt.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.cmt.dtos.DeleteResponseDto;
import staport.finance.gBanque.cmt.dtos.CreditMoyenTermeRequestDto;
import staport.finance.gBanque.cmt.dtos.CreditMoyenTermeResponseDto;
import staport.finance.gBanque.cmt.dtos.CreditMoyenTermeSearchDto;
import staport.finance.gBanque.cmt.services.CreditMoyenTermeService;

import java.time.LocalDate;

/**
 * Contrôleur REST pour la gestion des crédits à moyen terme (CMT).
 */
@Tag(name = "Crédits Moyen Terme", description = "Gestion des crédits à moyen terme et de leur tableau d'amortissement")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/credits-cmt")
public class CreditMoyenTermeController {

    private static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
    private static final String ADMIN_ONLY = "hasAuthority('SYSTEM_ADMIN')";

    private final CreditMoyenTermeService creditMoyenTermeService;

    public CreditMoyenTermeController(CreditMoyenTermeService creditMoyenTermeService) {
        this.creditMoyenTermeService = creditMoyenTermeService;
    }

    @Operation(summary = "Créer un crédit à moyen terme")
    @ApiResponse(responseCode = "201", description = "Crédit créé")
    @ApiResponse(responseCode = "404", description = "Banque introuvable")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @PostMapping
    public ResponseEntity<CreditMoyenTermeResponseDto> createCreditMoyenTerme(@Valid @RequestBody CreditMoyenTermeRequestDto requestDto) {
        CreditMoyenTermeResponseDto createdCredit = creditMoyenTermeService.create(requestDto);
        return new ResponseEntity<>(createdCredit, HttpStatus.CREATED);
    }

    @Operation(summary = "Lister les crédits actifs")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/actifs")
    public ResponseEntity<Page<CreditMoyenTermeResponseDto>> searchActiveCreditsMoyenTerme(CreditMoyenTermeSearchDto searchDto, Pageable pageable) {
        return ResponseEntity.ok(creditMoyenTermeService.searchActiveCredits(searchDto, pageable));
    }

    @Operation(summary = "Lister les crédits en attente de main levée")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/en-attente-de-main-levees")
    public ResponseEntity<Page<CreditMoyenTermeResponseDto>> searchPendingClosureCreditsMoyenTerme(CreditMoyenTermeSearchDto searchDto, Pageable pageable) {
        return ResponseEntity.ok(creditMoyenTermeService.searchPendingClosureCredits(searchDto, pageable));
    }

    @Operation(summary = "Lister les crédits archivés")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/archives")
    public ResponseEntity<Page<CreditMoyenTermeResponseDto>> searchArchivedCreditsMoyenTerme(CreditMoyenTermeSearchDto searchDto, Pageable pageable) {
        return ResponseEntity.ok(creditMoyenTermeService.searchArchivedCredits(searchDto, pageable));
    }

    @Operation(summary = "Archiver un crédit (main levée)",
            description = "Bloqué si au moins une échéance reste PREVU")
    @ApiResponse(responseCode = "400", description = "Crédit déjà archivé ou échéances encore PREVU")
    @ApiResponse(responseCode = "404", description = "Crédit introuvable")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @PutMapping("/{id}/archiver")
    public ResponseEntity<CreditMoyenTermeResponseDto> markAsArchived(@PathVariable Long id) {
        return ResponseEntity.ok(creditMoyenTermeService.markAsArchived(id));
    }

    @Operation(summary = "Récupérer un crédit par ID")
    @ApiResponse(responseCode = "404", description = "Crédit introuvable")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/{id}")
    public ResponseEntity<CreditMoyenTermeResponseDto> getCreditMoyenTermeById(@PathVariable Long id) {
        return ResponseEntity.ok(creditMoyenTermeService.findById(id));
    }

    @Operation(summary = "Modifier un crédit à moyen terme",
            description = "Régénère intégralement le tableau d'amortissement")
    @ApiResponse(responseCode = "404", description = "Crédit ou banque introuvable")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @PutMapping("/{id}")
    public ResponseEntity<CreditMoyenTermeResponseDto> updateCreditMoyenTerme(@PathVariable Long id, @Valid @RequestBody CreditMoyenTermeRequestDto requestDto) {
        return ResponseEntity.ok(creditMoyenTermeService.update(id, requestDto));
    }

    @Operation(summary = "Exporter en PDF les crédits actifs filtrés",
            description = "Mêmes critères que GET /credits-cmt/actifs — sans pagination, le fichier contient tous les résultats filtrés")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/actifs/export/pdf")
    public ResponseEntity<byte[]> exportActiveCreditsPdf(@ModelAttribute CreditMoyenTermeSearchDto searchDto) {
        byte[] pdf = creditMoyenTermeService.exportActiveCreditsPdf(searchDto);
        return pdfResponse(pdf, "credits_cmt_actifs");
    }

    @Operation(summary = "Exporter en PDF les crédits en attente de main levée filtrés",
            description = "Mêmes critères que GET /credits-cmt/en-attente-de-main-levees — sans pagination")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/en-attente-de-main-levees/export/pdf")
    public ResponseEntity<byte[]> exportPendingClosureCreditsPdf(@ModelAttribute CreditMoyenTermeSearchDto searchDto) {
        byte[] pdf = creditMoyenTermeService.exportPendingClosureCreditsPdf(searchDto);
        return pdfResponse(pdf, "credits_cmt_en_attente_de_main_levee");
    }

    @Operation(summary = "Exporter en PDF les crédits archivés filtrés",
            description = "Mêmes critères que GET /credits-cmt/archives — sans pagination")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/archives/export/pdf")
    public ResponseEntity<byte[]> exportArchivedCreditsPdf(@ModelAttribute CreditMoyenTermeSearchDto searchDto) {
        byte[] pdf = creditMoyenTermeService.exportArchivedCreditsPdf(searchDto);
        return pdfResponse(pdf, "credits_cmt_archives");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filenamePrefix) {
        String filename = filenamePrefix + "_" + LocalDate.now() + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @Operation(summary = "Supprimer un crédit à moyen terme",
            description = "Bloqué si le crédit contient des échéances déjà payées")
    @ApiResponse(responseCode = "400", description = "Crédit avec échéances payées — suppression bloquée")
    @ApiResponse(responseCode = "404", description = "Crédit introuvable")
    @PreAuthorize(ADMIN_ONLY)
    @DeleteMapping("/{id}")
    public ResponseEntity<DeleteResponseDto> deleteCreditMoyenTerme(@PathVariable Long id) {
        return ResponseEntity.ok(creditMoyenTermeService.delete(id));
    }
}
