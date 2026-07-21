package staport.finance.gBanque.cmt.echeances.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.cmt.echeances.dtos.*;
import staport.finance.gBanque.cmt.echeances.services.EcheanceCMTService;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;
import staport.finance.gBanque.cmt.services.CreditMoyenTermeService;

import java.time.LocalDate;


/**
 * Contrôleur REST pour la gestion et la consultation des échéances de crédit à moyen terme (CMT).
 */
@Tag(name = "Échéances CMT", description = "Consultation du tableau d'amortissement, des encours et gestion du statut des échéances")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/echeances-cmt")
public class EcheanceCMTController {

    private static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
    private static final String ADMIN_ONLY = "hasAuthority('SYSTEM_ADMIN')";

    private final EcheanceCMTService echeanceCmtService;
    private final CreditMoyenTermeService creditMoyenTermeService;

    public EcheanceCMTController(EcheanceCMTService echeanceCmtService, CreditMoyenTermeService creditMoyenTermeService) {
        this.echeanceCmtService = echeanceCmtService;
        this.creditMoyenTermeService = creditMoyenTermeService;
    }

    @Operation(summary = "Récupérer le tableau d'amortissement d'un crédit")
    @ApiResponse(responseCode = "404", description = "Crédit introuvable")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/tableau-amortissement/{creditId}")
    public ResponseEntity<TableauAmortissementCMTResponseDto> getTableauAmortissement(
            @PathVariable Long creditId,
            EcheanceCMTSearchDto searchDto,
            Pageable pageable) {
        CreditMoyenTerme credit = creditMoyenTermeService.findOrThrow(creditId);
        return ResponseEntity.ok(echeanceCmtService.getTableauAmortissementForCMT(credit, searchDto, pageable));
    }

    @Operation(summary = "Exporter en PDF le tableau d'amortissement d'un crédit",
            description = "Mêmes critères que GET /echeances-cmt/tableau-amortissement/{creditId} — sans pagination, avec les totaux Endettement/Payé/Restant")
    @ApiResponse(responseCode = "404", description = "Crédit introuvable")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/tableau-amortissement/{creditId}/export/pdf")
    public ResponseEntity<byte[]> exportTableauAmortissementPdf(
            @PathVariable Long creditId,
            @ModelAttribute EcheanceCMTSearchDto searchDto) {
        CreditMoyenTerme credit = creditMoyenTermeService.findOrThrow(creditId);
        byte[] pdf = echeanceCmtService.exportTableauAmortissementPdf(credit, searchDto);

        String filename = "tableau_amortissement_cmt_" + creditId + "_" + LocalDate.now() + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @Operation(summary = "Basculer le statut d'une échéance (PREVU ↔ PAYE)",
            description = "Action sensible réservée à l'administrateur système")
    @ApiResponse(responseCode = "404", description = "Échéance introuvable")
    @PreAuthorize(ADMIN_ONLY)
    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<EcheanceCMTResponseDto> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(echeanceCmtService.toggleStatus(id));
    }

    @Operation(summary = "Modifier une échéance au statut PREVU",
            description = "Permet à l'administrateur de modifier les champs d'une échéance non encore validée (statut PREVU). Bloqué si l'échéance est PAYE.")
    @ApiResponse(responseCode = "404", description = "Échéance introuvable")
    @ApiResponse(responseCode = "400", description = "Échéance déjà payée ou données invalides")
    @PreAuthorize(ADMIN_ONLY)
    @PutMapping("/{id}")
    public ResponseEntity<EcheanceCMTResponseDto> updateEcheance(
            @PathVariable Long id,
            @Valid @RequestBody EcheanceCMTUpdateRequestDto dto) {
        return ResponseEntity.ok(echeanceCmtService.updateEcheance(id, dto));
    }

    @Operation(summary = "Récupérer les encours CMT d'une banque")
    @ApiResponse(responseCode = "404", description = "Banque introuvable")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/encours")
    public ResponseEntity<EncoursCMTResponseDTO> getEncoursCMT(
            @Valid @ModelAttribute EncoursCMTRequestDTO requestDTO,
            Pageable pageable) {
        return ResponseEntity.ok(echeanceCmtService.getEncoursCMTForBanque(requestDTO, pageable));
    }
}
