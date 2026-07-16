package staport.finance.gBanque.logs.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.logs.dtos.LogEntryDetailResponse;
import staport.finance.gBanque.logs.dtos.LogResponse;
import staport.finance.gBanque.logs.dtos.LogSearchCriteria;
import staport.finance.gBanque.logs.services.LogService;

import java.util.List;

/**
 * Injecte {@link LogService} (DIP — jamais la classe concrète).
 *
 * <p>Autorisations (cf. {@code docs/rbac/logs.md}) : réservé {@code SYSTEM_ADMIN} sur tous les
 * endpoints, y compris la consultation — le journal d'audit expose des informations sensibles
 * (utilisateur, entité, valeurs avant/après) sur l'ensemble des modules, cohérent avec le guard
 * frontend déjà en place ({@code systemAdminGuard} sur toute la page).
 */
@Tag(name = "Logs", description = "Consultation du journal d'audit applicatif")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/logs")
public class LogController {

    private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @Operation(summary = "Rechercher les journaux d'audit")
    @GetMapping
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<Page<LogResponse>> getLogs(
            LogSearchCriteria criteria,
            Pageable pageable) {

        if (!pageable.getSort().isSorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "timestamp"));
        }
        Page<LogResponse> logs = logService.getLogs(criteria, pageable);
        return ResponseEntity.ok(logs);
    }

    @Operation(summary = "Supprimer un journal d'audit", description = "Réservé SYSTEM_ADMIN.")
    @DeleteMapping("/{id}")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<Void> deleteLog(@PathVariable Long id) {
        logService.deleteLogById(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Détail champ-par-champ d'un journal d'audit")
    @GetMapping("/{id}/details")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<List<LogEntryDetailResponse>> getLogDetails(@PathVariable Long id) {
        return ResponseEntity.ok(logService.getAuditDetail(id));
    }

    @Operation(summary = "Lister les noms d'entités journalisées")
    @GetMapping("/entities")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<List<String>> getDistinctEntityNames() {
        List<String> entityNames = logService.getEntityNames();
        return ResponseEntity.ok(entityNames);
    }
}
