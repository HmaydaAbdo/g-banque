package staport.finance.gBanque.leasing.echeancesLeasing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.contratLeasing.service.ContratLeasingService;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.*;
import staport.finance.gBanque.leasing.echeancesLeasing.services.interfaces.IEcheanceLeasingService;

import java.time.LocalDate;

@Tag(name = "Échéances de leasing", description = "Consultation et gestion des échéances de leasing")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/echeances-leasing")
public class EcheanceLeasingController {

    private static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
    private static final String ADMIN_ONLY = "hasAuthority('SYSTEM_ADMIN')";

    private final IEcheanceLeasingService echeanceLeasingService;
    private final ContratLeasingService contratLeasingService;

    public EcheanceLeasingController(IEcheanceLeasingService echeanceLeasingService,
                                     ContratLeasingService contratLeasingService) {
        this.echeanceLeasingService = echeanceLeasingService;
        this.contratLeasingService = contratLeasingService;
    }

    @Operation(summary = "Tableau d'amortissement d'un contrat",
            description = "Retourne le tableau complet avec filtres optionnels sur numéro, période, montant et statut")
    @ApiResponse(responseCode = "404", description = "Contrat introuvable")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/tableau-amortissement/{contratId}")
    public ResponseEntity<TableauAmortissementDTO> getTableauAmortissement(
            @PathVariable Long contratId,
            EcheanceLeasingSearchDTO searchDTO,
            Pageable pageable) {
        String numeroContrat = contratLeasingService.getContratLeasingById(contratId).numeroContrat();
        return ResponseEntity.ok(
                echeanceLeasingService.getTableauAmortissementForContrat(contratId, numeroContrat, searchDTO, pageable));
    }

    @Operation(summary = "Exporter en PDF le tableau d'amortissement d'un contrat",
            description = "Mêmes critères que GET /echeances-leasing/tableau-amortissement/{contratId} — sans pagination, avec les totaux Endettement/Payé/Restant")
    @ApiResponse(responseCode = "404", description = "Contrat introuvable")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/tableau-amortissement/{contratId}/export/pdf")
    public ResponseEntity<byte[]> exportTableauAmortissementPdf(
            @PathVariable Long contratId,
            @ModelAttribute EcheanceLeasingSearchDTO searchDTO) {
        ContratLeasing contrat = contratLeasingService.findOrThrow(contratId);
        byte[] pdf = echeanceLeasingService.exportTableauAmortissementPdf(contrat, searchDTO);

        String filename = "tableau_amortissement_leasing_" + contratId + "_" + LocalDate.now() + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @Operation(summary = "Basculer le statut d'une échéance (PREVU ↔ PAYE)",
            description = "Réservé aux administrateurs système")
    @ApiResponse(responseCode = "404", description = "Échéance introuvable")
    @PreAuthorize(ADMIN_ONLY)
    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<EcheanceLeasingResponseDTO> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(echeanceLeasingService.toggleStatus(id));
    }

    @Operation(summary = "Modifier une échéance au statut PREVU",
            description = "Permet à l'administrateur de modifier les champs d'une échéance non encore validée (statut PREVU). Bloqué si l'échéance est PAYE.")
    @ApiResponse(responseCode = "404", description = "Échéance introuvable")
    @ApiResponse(responseCode = "400", description = "Échéance déjà payée ou données invalides")
    @PreAuthorize(ADMIN_ONLY)
    @PutMapping("/{id}")
    public ResponseEntity<EcheanceLeasingResponseDTO> updateEcheance(
            @PathVariable Long id,
            @Valid @RequestBody EcheanceLeasingUpdateRequestDTO dto) {
        return ResponseEntity.ok(echeanceLeasingService.updateEcheance(id, dto));
    }

    @Operation(summary = "Encours de leasing pour une banque",
            description = "Filtrable par période et organisme de leasing")
    @ApiResponse(responseCode = "400", description = "Paramètres invalides")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/encours")
    public ResponseEntity<EncoursLeasingResponseDTO> getEncoursLeasing(
            @Valid @ModelAttribute EncoursLeasingRequestDTO requestDTO,
            Pageable pageable) {
        return ResponseEntity.ok(
                echeanceLeasingService.getEncoursLeasingForBanque(requestDTO, pageable));
    }

    @Operation(summary = "Exporter en PDF l'encours d'échéances de leasing d'une banque",
            description = "Mêmes critères que GET /echeances-leasing/encours — sans pagination")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/encours/export/pdf")
    public ResponseEntity<byte[]> exportEncoursLeasingPdf(@Valid @ModelAttribute EncoursLeasingRequestDTO requestDTO) {
        byte[] pdf = echeanceLeasingService.exportEncoursLeasingPdf(requestDTO);
        String filename = "encours_leasing_" + requestDTO.banqueId() + "_" + LocalDate.now() + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
