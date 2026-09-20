package staport.finance.gBanque.operationErrone.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.operationErrone.dtos.*;
import staport.finance.gBanque.operationErrone.services.IOperationErroneService;

import java.time.LocalDate;

/**
 * Injecte {@link IOperationErroneService} (DIP — jamais la classe concrète).
 *
 * <p>Autorisations (cf. {@code docs/rbac/operation-errone.md}) : réservé Finance/SYSTEM_ADMIN sur
 * toutes les actions (donnée financière propre au département Finance, mirroring
 * {@code ImpayeeController}/{@code RestitutionController}), suppression et changement de statut
 * (PREVU/PAYE) réservés SYSTEM_ADMIN.
 */
@Tag(name = "Opérations Erronées", description = "Gestion des opérations erronées rattachées aux banques")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/operation-errone")
public class OperationErroneController {

    private static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
    private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";

    private static final Logger log = LoggerFactory.getLogger(OperationErroneController.class);
    private final IOperationErroneService operationErroneService;

    public OperationErroneController(IOperationErroneService operationErroneService) {
        this.operationErroneService = operationErroneService;
    }

    @Operation(summary = "Créer une opération erronée")
    @PostMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<OperationErroneResponseDto> createOperationErrone(@Validated @RequestBody OperationErroneRequestDto requestDto) {
        log.info("Requête REST pour créer une opération erronée - Libellé: '{}', Montant: {}, Motif: {}",
                requestDto.libelle(), requestDto.montant(), requestDto.motif());

        OperationErroneResponseDto response = operationErroneService.createOperationErrone(requestDto);
        log.info("Opération erronée créée avec succès - ID: {}, Motif: {}", response.id(), response.motif());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Récupérer une opération erronée par ID")
    @GetMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<OperationErroneResponseDto> getById(@PathVariable Long id) {
        log.info("Requête REST pour récupérer l'opération erronée ID: {}", id);
        OperationErroneResponseDto response = operationErroneService.getById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Recherche paginée des opérations erronées")
    @PostMapping("/search")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<Page<OperationErroneResponseDto>> searchOperationErrone(@RequestBody OperationErroneSearchDto searchDto, Pageable pageable) {
        log.info("Requête REST pour rechercher des opérations erronées - Critères: {}", searchDto);
        Page<OperationErroneResponseDto> page = operationErroneService.searchOperationErrone(searchDto, pageable);
        log.info("Recherche terminée - {} résultat(s)", page.getTotalElements());
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Modifier une opération erronée")
    @PutMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<OperationErroneResponseDto> updateOperationErrone(@PathVariable Long id, @Validated @RequestBody OperationErroneRequestDto requestDto) {
        log.info("Requête REST pour mettre à jour l'opération erronée ID: {}", id);
        OperationErroneResponseDto response = operationErroneService.updateOperationErrone(id, requestDto);
        log.info("Opération erronée ID: {} mise à jour avec succès", id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Supprimer une opération erronée", description = "Réservé SYSTEM_ADMIN. Refusé si l'opération erronée n'est plus au statut PREVU.")
    @DeleteMapping("/{id}")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<Void> deleteOperationErrone(@PathVariable Long id) {
        log.info("Requête REST pour supprimer l'opération erronée ID: {}", id);
        operationErroneService.deleteOperationErrone(id);
        log.info("Opération erronée ID: {} supprimée avec succès", id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Changer le statut PREVU/PAYE", description = "Réservé SYSTEM_ADMIN.")
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<OperationErroneResponseDto> toggleStatus(@PathVariable Long id) {
        log.info("Requête REST pour basculer le statut de l'opération erronée ID: {}", id);
        OperationErroneResponseDto response = operationErroneService.toggleStatus(id);
        log.info("Statut de l'opération erronée ID: {} basculé vers {}", id, response.status());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Exporter la liste des opérations erronées en PDF", description = "Mêmes critères de filtre que /search, sans pagination.")
    @GetMapping("/export/pdf")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<byte[]> exporterPdf(@ModelAttribute OperationErroneSearchDto searchDto) {
        byte[] pdf = operationErroneService.exporterPdf(searchDto);
        String filename = "operations_erronees_" + LocalDate.now() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
