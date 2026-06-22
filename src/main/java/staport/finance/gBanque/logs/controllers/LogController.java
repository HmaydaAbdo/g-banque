package staport.finance.gBanque.logs.controllers;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.logs.dtos.LogEntryDetailResponse;
import staport.finance.gBanque.logs.dtos.LogResponse;
import staport.finance.gBanque.logs.dtos.LogSearchCriteria;
import staport.finance.gBanque.logs.services.LogService;

import java.util.List;

@RestController
@RequestMapping("/logs") // Définit le chemin de base de l'API
public class LogController {

    private final LogService logService;

    @Autowired
    public LogController(LogService logService) {
        this.logService = logService;
    }

    /**
     * Endpoint pour récupérer les logs avec des capacités de recherche, pagination et tri.
     * Exemple d'appel :
     * GET /api/logs?userName=john&operationType=UPDATE&entityName=LigneFonctionnelle&timestampStart=2023-01-01T00:00:00Z&timestampEnd=2023-12-31T23:59:59Z&page=0&size=10&sort=timestamp,desc
     *
     * @param criteria Les critères de recherche (userName, operationType, entityName, timestampStart, timestampEnd)
     * @param pageable Les informations de pagination et de tri (page, size, sort) fournies par Spring Data
     * @return Une Page de LogResponse contenant les logs trouvés.
     */
    @GetMapping
    public ResponseEntity<Page<LogResponse>> getLogs(
            LogSearchCriteria criteria,
            Pageable pageable) {

        if (!pageable.getSort().isSorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "timestamp"));
        }
        Page<LogResponse> logs = logService.getLogs(criteria, pageable);
        return ResponseEntity.ok(logs);
    }


    /**
     * Endpoint pour supprimer un log par son ID.
     * Exemple d'appel :
     * DELETE /api/logs/123
     *
     * @param id L'ID du log à supprimer.
     * @return ResponseEntity avec un statut 204 No Content en cas de succès, ou 404 Not Found si le log n'existe pas.
     */
    @DeleteMapping("/{id}") // {id} est une variable de chemin
    public ResponseEntity<Void> deleteLog(@PathVariable Long id) {
        logService.deleteLogById(id);
        return ResponseEntity.noContent().build(); // Statut 204 No Content pour une suppression réussie
    }

    /**
     * Détail champ-par-champ d'une ligne de log — stocké à l'écriture (anciens
     * aspects par entité) ou reconstruit à la demande via Envers (nouvel aspect
     * générique). Exemple : GET /logs/42/details
     */
    @GetMapping("/{id}/details")
    public ResponseEntity<List<LogEntryDetailResponse>> getLogDetails(@PathVariable Long id) {
        return ResponseEntity.ok(logService.getAuditDetail(id));
    }

    /**
     * Endpoint pour récupérer les noms distincts des entités loguées.
     * Exemple d'appel : GET /logs/entities
     *
     * @return Liste de noms d'entités distincts.
     */
    @GetMapping("/entities")
    public ResponseEntity<List<String>> getDistinctEntityNames() {
        List<String> entityNames = logService.getEntityNames();
        return ResponseEntity.ok(entityNames);
    }
}