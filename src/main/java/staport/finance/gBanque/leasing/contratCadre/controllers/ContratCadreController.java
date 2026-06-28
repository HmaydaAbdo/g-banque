package staport.finance.gBanque.leasing.contratCadre.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.leasing.contratCadre.dtos.ContratCadreRequestDTO;
import staport.finance.gBanque.leasing.contratCadre.dtos.ContratCadreResponseDTO;
import staport.finance.gBanque.leasing.contratCadre.dtos.ContratCadreSearchDTO;
import staport.finance.gBanque.leasing.contratCadre.dtos.ContratCadreUpdateDTO;
import staport.finance.gBanque.leasing.contratCadre.enums.StatutContratCadre;
import staport.finance.gBanque.leasing.contratCadre.services.IContratCadreService;

import java.util.List;

@Tag(name = "Contrats cadres", description = "Gestion des contrats cadres des organismes de leasing")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/contrats-cadres")
public class ContratCadreController {

    private static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";

    private final IContratCadreService service;

    public ContratCadreController(IContratCadreService service) {
        this.service = service;
    }

    @Operation(summary = "Rechercher les contrats cadres",
               description = "Liste paginée et triable avec filtres optionnels (organisme, statut).")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping
    public ResponseEntity<Page<ContratCadreResponseDTO>> search(
            @RequestParam(required = false) Long organismeId,
            @RequestParam(required = false) StatutContratCadre statut,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.search(new ContratCadreSearchDTO(organismeId, statut), pageable));
    }

    @Operation(summary = "Créer un contrat cadre",
               description = "Bloqué si l'organisme a déjà un contrat cadre ACTIF.")
    @ApiResponse(responseCode = "201", description = "Contrat cadre créé")
    @ApiResponse(responseCode = "400", description = "Données invalides ou contrat cadre actif existant")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @PostMapping
    public ResponseEntity<ContratCadreResponseDTO> create(
            @Valid @RequestBody ContratCadreRequestDTO dto) {
        return new ResponseEntity<>(service.create(dto), HttpStatus.CREATED);
    }

    @Operation(summary = "Mettre à jour un contrat cadre",
               description = "Modifie le plafond et les dates. L'organisme ne peut pas être modifié.")
    @ApiResponse(responseCode = "200", description = "Contrat cadre mis à jour")
    @ApiResponse(responseCode = "404", description = "Contrat cadre introuvable")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @PutMapping("/{id}")
    public ResponseEntity<ContratCadreResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ContratCadreUpdateDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @Operation(summary = "Récupérer un contrat cadre par ID")
    @ApiResponse(responseCode = "404", description = "Contrat cadre introuvable")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/{id}")
    public ResponseEntity<ContratCadreResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @Operation(summary = "Lister les contrats cadres d'un organisme",
               description = "Triés par date de début décroissante.")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @GetMapping("/organisme/{organismeId}")
    public ResponseEntity<List<ContratCadreResponseDTO>> findByOrganisme(
            @PathVariable Long organismeId) {
        return ResponseEntity.ok(service.findByOrganisme(organismeId));
    }

    @Operation(summary = "Archiver un contrat cadre",
               description = "Passe le statut de ACTIF à ARCHIVE. Irréversible.")
    @ApiResponse(responseCode = "200", description = "Contrat cadre archivé")
    @ApiResponse(responseCode = "400", description = "Le contrat cadre n'est pas ACTIF")
    @ApiResponse(responseCode = "404", description = "Contrat cadre introuvable")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @PatchMapping("/{id}/archiver")
    public ResponseEntity<ContratCadreResponseDTO> archiver(@PathVariable Long id) {
        return ResponseEntity.ok(service.archiver(id));
    }
}
