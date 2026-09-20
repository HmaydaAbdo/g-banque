package staport.finance.gBanque.provision.controller;

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
import staport.finance.gBanque.provision.dtos.*;
import staport.finance.gBanque.provision.services.IProvisionService;

import java.time.LocalDate;

/**
 * Injecte {@link IProvisionService} (DIP — jamais la classe concrète).
 * <p>Autorisations (cf. {@code docs/rbac/README.md}, chapitre Provision) : réservé Finance/
 * SYSTEM_ADMIN sur toutes les actions (donnée financière propre au département Finance, mirroring
 */
@Tag(name = "Provision", description = "Gestion des provisions (opérations financières d'encaissement ou de décaissement) rattachées aux banques")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/provision")
public class ProvisionController {

    private static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
    private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";

    private static final Logger log = LoggerFactory.getLogger(ProvisionController.class);
    private final IProvisionService provisionService;

    public ProvisionController(IProvisionService provisionService) {
        this.provisionService = provisionService;
    }

    @Operation(summary = "Créer une provision")
    @PostMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<ProvisionResponseDto> createProvision(@Validated @RequestBody ProvisionRequestDto requestDto) {
        log.info("Requête REST pour créer une provision - Libellé: '{}', Montant: {}, Motif: {}, Sens: {}",
                requestDto.libelle(), requestDto.montant(), requestDto.motif(), requestDto.sens());

        ProvisionResponseDto response = provisionService.createProvision(requestDto);
        log.info("Provision créée avec succès - ID: {}, Sens: {}", response.id(), response.sens());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Récupérer une provision par ID")
    @GetMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<ProvisionResponseDto> getById(@PathVariable Long id) {
        log.info("Requête REST pour récupérer la provision ID: {}", id);
        return ResponseEntity.ok(provisionService.getById(id));
    }

    @Operation(summary = "Recherche paginée des provisions")
    @PostMapping("/search")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<Page<ProvisionResponseDto>> searchProvision(@RequestBody ProvisionSearchDto searchDto, Pageable pageable) {
        log.info("Requête REST pour rechercher des provisions - Critères: {}", searchDto);
        Page<ProvisionResponseDto> page = provisionService.searchProvision(searchDto, pageable);
        log.info("Recherche terminée - {} résultat(s)", page.getTotalElements());
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Modifier une provision")
    @PutMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<ProvisionResponseDto> updateProvision(@PathVariable Long id, @Validated @RequestBody ProvisionRequestDto requestDto) {
        log.info("Requête REST pour mettre à jour la provision ID: {}", id);
        ProvisionResponseDto response = provisionService.updateProvision(id, requestDto);
        log.info("Provision ID: {} mise à jour avec succès", id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Supprimer une provision", description = "Réservé SYSTEM_ADMIN. Refusé si la provision n'est plus au statut PREVU.")
    @DeleteMapping("/{id}")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<Void> deleteProvision(@PathVariable Long id) {
        log.info("Requête REST pour supprimer la provision ID: {}", id);
        provisionService.deleteProvision(id);
        log.info("Provision ID: {} supprimée avec succès", id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Changer le statut PREVU/PAYE", description = "Réservé SYSTEM_ADMIN.")
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<ProvisionResponseDto> toggleStatus(@PathVariable Long id) {
        log.info("Requête REST pour basculer le statut de la provision ID: {}", id);
        ProvisionResponseDto response = provisionService.toggleStatus(id);
        log.info("Statut de la provision ID: {} basculé vers {}", id, response.status());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Exporter la liste des provisions en PDF", description = "Mêmes critères de filtre que /search, sans pagination.")
    @GetMapping("/export/pdf")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<byte[]> exporterPdf(@ModelAttribute ProvisionSearchDto searchDto) {
        byte[] pdf = provisionService.exporterPdf(searchDto);
        String filename = "provisions_" + LocalDate.now() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
