package staport.finance.gBanque.leasing.lesOrganismsLeasing.controllers;

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
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingRequestDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingResponseDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingSearchDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.services.interfaces.OrganismeLeasingService;

import java.time.LocalDate;

@Tag(name = "Organismes de leasing", description = "Gestion des organismes de leasing")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/OrganismesLeasing")
public class OrganismeLeasingController {

    private static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
    private static final String ADMIN_ONLY = "hasAuthority('SYSTEM_ADMIN')";

    private final OrganismeLeasingService organismeLeasingService;

    public OrganismeLeasingController(OrganismeLeasingService organismeLeasingService) {
        this.organismeLeasingService = organismeLeasingService;
    }

    @Operation(summary = "Créer un organisme de leasing")
    @ApiResponse(responseCode = "201", description = "Organisme créé")
    @ApiResponse(responseCode = "400", description = "Données invalides")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @PostMapping
    public ResponseEntity<OrganismeLeasingResponseDTO> createOrganismeLeasing(
            @Valid @RequestBody OrganismeLeasingRequestDTO requestDTO) {
        OrganismeLeasingResponseDTO created = organismeLeasingService.create(requestDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Operation(summary = "Modifier un organisme de leasing")
    @ApiResponse(responseCode = "400", description = "Données invalides")
    @ApiResponse(responseCode = "404", description = "Organisme introuvable")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @PutMapping("/{id}")
    public ResponseEntity<OrganismeLeasingResponseDTO> updateOrganismeLeasing(
            @PathVariable Long id,
            @Valid @RequestBody OrganismeLeasingRequestDTO requestDTO) {
        return ResponseEntity.ok(organismeLeasingService.update(id, requestDTO));
    }

    @Operation(summary = "Récupérer un organisme par ID")
    @ApiResponse(responseCode = "404", description = "Organisme introuvable")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/{id}")
    public ResponseEntity<OrganismeLeasingResponseDTO> getOrganismeLeasingById(@PathVariable Long id) {
        return ResponseEntity.ok(organismeLeasingService.getById(id));
    }

    @Operation(summary = "Supprimer un organisme de leasing",
            description = "Bloqué si l'organisme est lié à des contrats existants")
    @ApiResponse(responseCode = "204", description = "Organisme supprimé")
    @ApiResponse(responseCode = "400", description = "Organisme lié à des contrats — suppression bloquée")
    @ApiResponse(responseCode = "404", description = "Organisme introuvable")
    @PreAuthorize(ADMIN_ONLY)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrganismeLeasing(@PathVariable Long id) {
        organismeLeasingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Rechercher des organismes de leasing")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping
    public ResponseEntity<Page<OrganismeLeasingResponseDTO>> searchOrganismeLeasings(
            OrganismeLeasingSearchDTO criteria, Pageable pageable) {
        return ResponseEntity.ok(organismeLeasingService.search(criteria, pageable));
    }

    @Operation(summary = "Exporter en PDF les organismes de leasing",
            description = "Mêmes critères que GET /OrganismesLeasing — sans pagination")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@ModelAttribute OrganismeLeasingSearchDTO criteria) {
        byte[] pdf = organismeLeasingService.exportPdf(criteria);
        String filename = "organismes_leasing_" + LocalDate.now() + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
