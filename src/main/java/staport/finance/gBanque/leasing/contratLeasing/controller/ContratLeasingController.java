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
}
