package staport.finance.gBanque.moyensDePaiements.virements.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.moyensDePaiements.virements.dtos.*;
import staport.finance.gBanque.moyensDePaiements.virements.service.VirementService;

import java.time.LocalDate;

@Tag(name = "Virements", description = "Gestion des virements nationaux — encaissements et décaissements")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/virements")
@PreAuthorize(VirementController.FINANCE_OR_ADMIN)
public class VirementController {

    static final String FINANCE_OR_ADMIN =
            "hasAnyAuthority('DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR', 'SYSTEM_ADMIN')";
    private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";

    private final VirementService virementService;

    public VirementController(VirementService virementService) {
        this.virementService = virementService;
    }

    @Operation(summary = "Créer un virement d'encaissement")
    @PostMapping
    public ResponseEntity<VirementResponseDto> createVirement(@Valid @RequestBody VirementRequestDto dto) {
        VirementResponseDto response = virementService.createEncaissementVirement(dto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Modifier un virement d'encaissement")
    @PutMapping("/{id}")
    public ResponseEntity<VirementResponseDto> updateVirement(@PathVariable Long id, @Valid @RequestBody VirementRequestDto dto) {
        VirementResponseDto response = virementService.editEncaissementVirement(id, dto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Consulter un virement par ID")
    @GetMapping("/{id}")
    public ResponseEntity<VirementResponseDto> getVirementByid(@PathVariable Long id) {
        VirementResponseDto response = virementService.getVirementById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Supprimer un virement")
    @PreAuthorize(SYSTEM_ADMIN)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVirement(@PathVariable Long id) {
        virementService.deleteVirement(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Lister les virements d'encaissement")
    @GetMapping("/encaissements")
    public ResponseEntity<Page<VirementResponseDto>> getEncaissementVirements(VirementSearchDto searchDto, @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        Page<VirementResponseDto> result = virementService.getEncaissementVirements(searchDto, pageable);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Lister les virements de décaissement")
    @GetMapping("/decaissements")
    public ResponseEntity<Page<VirementResponseDto>> getDecaissementVirements(VirementSearchDto searchDto, @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        Page<VirementResponseDto> result = virementService.getDecaissementVirements(searchDto, pageable);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Basculer le statut d'un virement (ENCOURS ↔ REGLE)")
    @PreAuthorize(SYSTEM_ADMIN)
    @PatchMapping("/toggle/{id}")
    public ResponseEntity<VirementResponseDto> toggleVirementStatus(@PathVariable Long id) {
        VirementResponseDto updatedVirement = virementService.toggleVirementStatus(id);
        return ResponseEntity.ok(updatedVirement);
    }

    @Operation(summary = "Exporter les virements d'encaissement en PDF",
            description = "Ignore la pagination — exporte tous les résultats filtrés.")
    @GetMapping("/encaissements/export/pdf")
    public ResponseEntity<byte[]> exportEncaissementsPdf(VirementSearchDto searchDto) {
        return pdfResponse(virementService.exportEncaissementsPdf(searchDto), "virements_encaissements");
    }

    @Operation(summary = "Exporter les virements de décaissement en PDF",
            description = "Ignore la pagination — exporte tous les résultats filtrés.")
    @GetMapping("/decaissements/export/pdf")
    public ResponseEntity<byte[]> exportDecaissementsPdf(VirementSearchDto searchDto) {
        return pdfResponse(virementService.exportDecaissementsPdf(searchDto), "virements_decaissements");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filenamePrefix) {
        String filename = filenamePrefix + "_" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
