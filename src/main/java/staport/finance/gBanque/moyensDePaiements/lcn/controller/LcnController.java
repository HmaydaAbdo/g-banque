package staport.finance.gBanque.moyensDePaiements.lcn.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.moyensDePaiements.lcn.dtos.*;
import staport.finance.gBanque.moyensDePaiements.lcn.services.LcnService;

import java.time.LocalDate;

@Tag(name = "LCN", description = "Gestion des LCN (lettres de change normalisées) — encaissements et décaissements")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/lcns")
@PreAuthorize(LcnController.FINANCE_OR_ADMIN)
public class LcnController {

    static final String FINANCE_OR_ADMIN =
            "hasAnyAuthority('DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR', 'SYSTEM_ADMIN')";
    private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";

    private final LcnService lcnService;

    public LcnController(LcnService lcnService) {
        this.lcnService = lcnService;
    }

    @Operation(summary = "Créer un LCN d'encaissement")
    @PostMapping()
    public ResponseEntity<LcnResponseDTO> createLcn(@RequestBody LcnRequestDto requestDto) {
        LcnResponseDTO createdLcn = lcnService.createLcn(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdLcn);
    }

    @Operation(summary = "Modifier un LCN d'encaissement")
    @PutMapping("/{id}")
    public ResponseEntity<LcnResponseDTO> updateLcn(@PathVariable Long id, @RequestBody LcnRequestDto requestDto) {
        LcnResponseDTO response = lcnService.editEncaissementLcn(id, requestDto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Consulter un LCN par ID")
    @GetMapping("/{id}")
    public ResponseEntity<LcnResponseDTO> getLcnById(@PathVariable Long id) {
        LcnResponseDTO response = lcnService.getLcnById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Supprimer un LCN")
    @PreAuthorize(SYSTEM_ADMIN)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLcn(@PathVariable Long id) {
        lcnService.deleteLcn(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Lister les LCN de décaissement")
    @GetMapping("/decaissements")
    public ResponseEntity<Page<LcnResponseDTO>> getAllDecaissements(LcnSearchDTO searchDto, @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        Page<LcnResponseDTO> result = lcnService.getDecaissements(searchDto, pageable);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Lister les LCN d'encaissement")
    @GetMapping("/encaissements")
    public ResponseEntity<Page<LcnResponseDTO>> getAllEncaissements(LcnSearchDTO searchDto, @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        Page<LcnResponseDTO> result = lcnService.getEncaissements(searchDto, pageable);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Basculer le statut d'un LCN (ENCOURS ↔ REGLE)")
    @PreAuthorize(SYSTEM_ADMIN)
    @PatchMapping("/toggle/{id}")
    public ResponseEntity<LcnResponseDTO> toggleLcnStatus(@PathVariable Long id) {
        LcnResponseDTO updatedLcn = lcnService.toggleLcnStatus(id);
        return ResponseEntity.ok(updatedLcn);
    }

    @Operation(summary = "Encours de décaissement LCN pour une banque")
    @GetMapping("/decaissements/encours/{banqueId}")
    public ResponseEntity<EncoursLcnResponse> getEncoursDecaissementsForLcn(@PathVariable Long banqueId, EncoursLcnSearchDto searchDto,
                                                                            @PageableDefault(size = 10, sort = "dateEcheance") Pageable pageable) {
        EncoursLcnResponse result = lcnService.getEncoursDecaissementsForLcn(searchDto, banqueId, pageable);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Encours d'encaissement LCN pour une banque")
    @GetMapping("/encaissements/encours/{banqueId}")
    public ResponseEntity<EncoursLcnResponse> getEncoursEncaissementsForLcn(
            @PathVariable Long banqueId,
            EncoursLcnSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEcheance") Pageable pageable) {
        EncoursLcnResponse result = lcnService.getEncoursEncaissementsForLcn(searchDto, banqueId, pageable);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Exporter les LCN d'encaissement en PDF",
            description = "Ignore la pagination — exporte tous les résultats filtrés.")
    @GetMapping("/encaissements/export/pdf")
    public ResponseEntity<byte[]> exportEncaissementsPdf(LcnSearchDTO searchDto) {
        return pdfResponse(lcnService.exportEncaissementsPdf(searchDto), "lcn_encaissements");
    }

    @Operation(summary = "Exporter les LCN de décaissement en PDF",
            description = "Ignore la pagination — exporte tous les résultats filtrés.")
    @GetMapping("/decaissements/export/pdf")
    public ResponseEntity<byte[]> exportDecaissementsPdf(LcnSearchDTO searchDto) {
        return pdfResponse(lcnService.exportDecaissementsPdf(searchDto), "lcn_decaissements");
    }

    @Operation(summary = "Exporter en PDF l'encours d'encaissement de LCN d'une banque",
            description = "Ignore la pagination — exporte tous les résultats filtrés.")
    @GetMapping("/encaissements/encours/{banqueId}/export/pdf")
    public ResponseEntity<byte[]> exportEncoursEncaissementPdf(@PathVariable Long banqueId, EncoursLcnSearchDto searchDto) {
        return pdfResponse(lcnService.exportEncoursEncaissementPdf(searchDto, banqueId), "lcn_encours_encaissement");
    }

    @Operation(summary = "Exporter en PDF l'encours de décaissement de LCN d'une banque",
            description = "Ignore la pagination — exporte tous les résultats filtrés.")
    @GetMapping("/decaissements/encours/{banqueId}/export/pdf")
    public ResponseEntity<byte[]> exportEncoursDecaissementPdf(@PathVariable Long banqueId, EncoursLcnSearchDto searchDto) {
        return pdfResponse(lcnService.exportEncoursDecaissementPdf(searchDto, banqueId), "lcn_encours_decaissement");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filenamePrefix) {
        String filename = filenamePrefix + "_" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
