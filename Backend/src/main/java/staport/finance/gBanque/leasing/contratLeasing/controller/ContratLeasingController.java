package staport.finance.gBanque.leasing.contratLeasing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingRequestDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingResponseDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingSearchDTO;
import staport.finance.gBanque.leasing.contratLeasing.service.ContratLeasingService;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Contrats de leasing", description = "Gestion des contrats de leasing et de leurs échéances")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/contratsLeasing")
public class ContratLeasingController {

    private static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
    private static final String ADMIN_ONLY = "hasAuthority('SYSTEM_ADMIN')";

    private final ContratLeasingService contratLeasingService;

    public ContratLeasingController(ContratLeasingService contratLeasingService) {
        this.contratLeasingService = contratLeasingService;
    }

    @Operation(summary = "Créer un contrat de leasing")
    @ApiResponse(responseCode = "201", description = "Contrat créé")
    @ApiResponse(responseCode = "400", description = "Données invalides ou règles métier non respectées")
    @ApiResponse(responseCode = "404", description = "Banque ou organisme de leasing introuvable")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @PostMapping
    public ResponseEntity<ContratLeasingResponseDTO> createContratLeasing(
            @Valid @RequestBody ContratLeasingRequestDTO requestDTO) {
        ContratLeasingResponseDTO created = contratLeasingService.createContratLeasing(requestDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Operation(summary = "Récupérer un contrat par ID")
    @ApiResponse(responseCode = "404", description = "Contrat introuvable")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/{id}")
    public ResponseEntity<ContratLeasingResponseDTO> getContratLeasingById(@PathVariable Long id) {
        return ResponseEntity.ok(contratLeasingService.getContratLeasingById(id));
    }

    @Operation(summary = "Modifier un contrat de leasing")
    @ApiResponse(responseCode = "400", description = "Données invalides")
    @ApiResponse(responseCode = "404", description = "Contrat introuvable")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @PutMapping("/{id}")
    public ResponseEntity<ContratLeasingResponseDTO> updateContratLeasing(
            @PathVariable Long id,
            @Valid @RequestBody ContratLeasingRequestDTO requestDTO) {
        return ResponseEntity.ok(contratLeasingService.updateContratLeasing(id, requestDTO));
    }

    @Operation(summary = "Clôturer un contrat (main levée)")
    @ApiResponse(responseCode = "400", description = "Contrat déjà inactif ou échéances encore PREVU")
    @ApiResponse(responseCode = "404", description = "Contrat introuvable")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @PutMapping("/{id}/markAsInactive")
    public ResponseEntity<ContratLeasingResponseDTO> markAsInactive(@PathVariable Long id) {
        return ResponseEntity.ok(contratLeasingService.markAsInactive(id));
    }

    @Operation(summary = "Supprimer un contrat de leasing",
            description = "Bloqué si le contrat contient des échéances déjà payées")
    @ApiResponse(responseCode = "204", description = "Contrat supprimé")
    @ApiResponse(responseCode = "400", description = "Contrat avec échéances payées — suppression bloquée")
    @ApiResponse(responseCode = "404", description = "Contrat introuvable")
    @PreAuthorize(ADMIN_ONLY)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContratLeasing(@PathVariable Long id) {
        contratLeasingService.deleteContratLeasing(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Lister les contrats actifs")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/actifs")
    public ResponseEntity<Page<ContratLeasingResponseDTO>> getActiveContrats(
            ContratLeasingSearchDTO searchDTO, Pageable pageable) {
        return ResponseEntity.ok(contratLeasingService.getActiveContrats(searchDTO, pageable));
    }

    @Operation(summary = "Lister les contrats en attente de main levée")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/en-attente-main-levee")
    public ResponseEntity<Page<ContratLeasingResponseDTO>> getPendingLienReleaseContrats(
            ContratLeasingSearchDTO searchDTO, Pageable pageable) {
        return ResponseEntity.ok(contratLeasingService.getPendingLienReleaseContrats(searchDTO, pageable));
    }

    @Operation(summary = "Lister les contrats clôturés (inactifs)")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/inactifs")
    public ResponseEntity<Page<ContratLeasingResponseDTO>> getInactiveContrats(
            ContratLeasingSearchDTO searchDTO, Pageable pageable) {
        return ResponseEntity.ok(contratLeasingService.getInactiveContrats(searchDTO, pageable));
    }

    @Operation(summary = "Exporter en PDF les contrats actifs",
            description = "Mêmes critères que GET /contratsLeasing/actifs — sans pagination. `colonnes` (optionnel, répétable) : numeroContrat, banque, organisme, immobilisation, type, montantAchatTTC, plafond, echeances, dateDebut, dateFin, duree, statut, endettement, resteAPayer, dejaPaye — absent, vide ou sans valeur reconnue : toutes les colonnes.")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/actifs/export/pdf")
    public ResponseEntity<byte[]> exportActiveContratsPdf(@ModelAttribute ContratLeasingSearchDTO searchDTO,
                                         @RequestParam(required = false) List<String> colonnes) {
        return pdfResponse(contratLeasingService.exportActiveContratsPdf(searchDTO, colonnes), "contrats_leasing_actifs");
    }

    @Operation(summary = "Exporter en Excel les contrats actifs",
            description = "Mêmes critères et même paramètre `colonnes` que /actifs/export/pdf.")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/actifs/export/excel")
    public ResponseEntity<byte[]> exportActiveContratsExcel(@ModelAttribute ContratLeasingSearchDTO searchDTO,
                                           @RequestParam(required = false) List<String> colonnes) {
        return excelResponse(contratLeasingService.exportActiveContratsExcel(searchDTO, colonnes), "contrats_leasing_actifs");
    }

    @Operation(summary = "Exporter en PDF les contrats en attente de main levée",
            description = "Mêmes critères que GET /contratsLeasing/en-attente-main-levee — sans pagination. `colonnes` (optionnel, répétable) : numeroContrat, banque, organisme, immobilisation, type, montantAchatTTC, plafond, echeances, dateDebut, dateFin, duree, statut, endettement, resteAPayer, dejaPaye — absent, vide ou sans valeur reconnue : toutes les colonnes.")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/en-attente-main-levee/export/pdf")
    public ResponseEntity<byte[]> exportPendingLienReleaseContratsPdf(@ModelAttribute ContratLeasingSearchDTO searchDTO,
                                         @RequestParam(required = false) List<String> colonnes) {
        return pdfResponse(contratLeasingService.exportPendingLienReleaseContratsPdf(searchDTO, colonnes), "contrats_leasing_en_attente_de_main_levee");
    }

    @Operation(summary = "Exporter en Excel les contrats en attente de main levée",
            description = "Mêmes critères et même paramètre `colonnes` que /en-attente-main-levee/export/pdf.")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/en-attente-main-levee/export/excel")
    public ResponseEntity<byte[]> exportPendingLienReleaseContratsExcel(@ModelAttribute ContratLeasingSearchDTO searchDTO,
                                           @RequestParam(required = false) List<String> colonnes) {
        return excelResponse(contratLeasingService.exportPendingLienReleaseContratsExcel(searchDTO, colonnes), "contrats_leasing_en_attente_de_main_levee");
    }

    @Operation(summary = "Exporter en PDF les contrats clôturés (inactifs)",
            description = "Mêmes critères que GET /contratsLeasing/inactifs — sans pagination. `colonnes` (optionnel, répétable) : numeroContrat, banque, organisme, immobilisation, type, montantAchatTTC, plafond, echeances, dateDebut, dateFin, duree, statut, endettement, resteAPayer, dejaPaye — absent, vide ou sans valeur reconnue : toutes les colonnes.")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/inactifs/export/pdf")
    public ResponseEntity<byte[]> exportInactiveContratsPdf(@ModelAttribute ContratLeasingSearchDTO searchDTO,
                                         @RequestParam(required = false) List<String> colonnes) {
        return pdfResponse(contratLeasingService.exportInactiveContratsPdf(searchDTO, colonnes), "contrats_leasing_archives");
    }

    @Operation(summary = "Exporter en Excel les contrats clôturés (inactifs)",
            description = "Mêmes critères et même paramètre `colonnes` que /inactifs/export/pdf.")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/inactifs/export/excel")
    public ResponseEntity<byte[]> exportInactiveContratsExcel(@ModelAttribute ContratLeasingSearchDTO searchDTO,
                                           @RequestParam(required = false) List<String> colonnes) {
        return excelResponse(contratLeasingService.exportInactiveContratsExcel(searchDTO, colonnes), "contrats_leasing_archives");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filenamePrefix) {
        String filename = filenamePrefix + "_" + LocalDate.now() + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    private ResponseEntity<byte[]> excelResponse(byte[] excel, String filenamePrefix) {
        String filename = filenamePrefix + "_" + LocalDate.now() + ".xlsx";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(excel, headers, HttpStatus.OK);
    }
}
