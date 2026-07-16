package staport.finance.gBanque.impayees.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.impayees.dtos.*;
import staport.finance.gBanque.impayees.service.IImpayeeService;

/**
 * Injecte {@link IImpayeeService} (DIP — jamais la classe concrète).
 *
 * <p>Autorisations (cf. {@code docs/rbac/impayees.md}) : réservé Finance/SYSTEM_ADMIN sur toutes
 * les actions (donnée financière propre au département Finance, mirroring
 * {@code ImpotEtTaxeController}/{@code CommissionController}), suppression et changement de statut
 * (PREVU/PAYE) réservés SYSTEM_ADMIN.
 */
@Tag(name = "Impayées", description = "Gestion des chèques/effets impayés rattachés aux banques")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/impayees")
public class ImpayeeController {

    private static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
    private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";

    private static final Logger log = LogManager.getLogger(ImpayeeController.class);
    private final IImpayeeService impayeeService;

    public ImpayeeController(IImpayeeService impayeeService) {
        this.impayeeService = impayeeService;
    }

    @Operation(summary = "Créer une impayée")
    @PostMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<ImpayeeResponseDto> createImpayee(@Validated @RequestBody ImpayeeRequestDto requestDto) {
        log.info("Requête REST: Création d'une impayée: {}", requestDto);
        return ResponseEntity.ok(impayeeService.createImpayee(requestDto));
    }

    @Operation(summary = "Récupérer une impayée par ID")
    @GetMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<ImpayeeResponseDto> getById(@PathVariable Long id) {
        log.info("Requête REST: Récupération impayée id={}", id);
        return ResponseEntity.ok(impayeeService.getById(id));
    }

    @Operation(summary = "Recherche paginée des impayées")
    @PostMapping("/search")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<Page<ImpayeeResponseDto>> searchImpayees(@RequestBody ImpayeeSearchDto searchDto, Pageable pageable) {
        log.info("Requête REST: Recherche impayées - Critères: {}", searchDto);
        return ResponseEntity.ok(impayeeService.searchImpayees(searchDto, pageable));
    }

    @Operation(summary = "Modifier une impayée")
    @PutMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<ImpayeeResponseDto> updateImpayee(@PathVariable Long id, @Validated @RequestBody ImpayeeRequestDto requestDto) {
        log.info("Requête REST: Mise à jour impayée id={}", id);
        return ResponseEntity.ok(impayeeService.updateImpayee(id, requestDto));
    }

    @Operation(summary = "Supprimer une impayée", description = "Réservé SYSTEM_ADMIN. Refusé si l'impayée n'est plus au statut PREVU.")
    @DeleteMapping("/{id}")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<Void> deleteImpayee(@PathVariable Long id) {
        log.info("Requête REST: Suppression impayée id={}", id);
        impayeeService.deleteImpayee(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Consulter les encours d'impayées d'une banque")
    @GetMapping("/encours/{banqueId}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<EncoursImpayeeDto> getOutstandingImpayees(@PathVariable Long banqueId, @ModelAttribute EncoursImpayeeSearchDto searchDto, @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("Requête REST: Récupération encours pour banqueId={}", banqueId);
        return ResponseEntity.ok(impayeeService.getOutstandingImpayees(searchDto, banqueId, pageable));
    }

    @Operation(summary = "Changer le statut PREVU/PAYE", description = "Réservé SYSTEM_ADMIN.")
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<ImpayeeResponseDto> toggleStatus(@PathVariable Long id) {
        log.info("Requête REST: Basculement statut pour impayée ID: {}", id);
        ImpayeeResponseDto response = impayeeService.toggleStatus(id);
        log.info("Statut de l'ID {} mis à jour vers {}", id, response.status());
        return ResponseEntity.ok(response);
    }
}
