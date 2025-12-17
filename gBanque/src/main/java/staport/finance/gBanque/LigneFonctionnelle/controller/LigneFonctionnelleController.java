package staport.finance.gBanque.LigneFonctionnelle.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import staport.finance.gBanque.LigneFonctionnelle.dtos.*;
import staport.finance.gBanque.LigneFonctionnelle.services.LigneFonctionnelleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/lignes-fonctionnelles")
public class LigneFonctionnelleController {

    private static final Logger logger = LoggerFactory.getLogger(LigneFonctionnelleController.class);
    private final LigneFonctionnelleService ligneFonctionnelleService;

    @Autowired
    public LigneFonctionnelleController(LigneFonctionnelleService ligneFonctionnelleService) {
        this.ligneFonctionnelleService = ligneFonctionnelleService;
    }

    @PostMapping
    public ResponseEntity<LigneFonctionnelleResponse> createLigneFonctionnelle(@Valid @RequestBody LigneFonctionnelleRequest request) {
        logger.info("Creating new ligne fonctionnelle for banque ID: {} with request : {}", request.getBanqueId(), request);
        LigneFonctionnelleResponse createdLigne = ligneFonctionnelleService.createLigneFonctionnelle(request);
        logger.info("Successfully created ligne fonctionnelle with ID: {}", createdLigne.getId());
        return new ResponseEntity<>(createdLigne, HttpStatus.CREATED);
    }


    @PutMapping("/{id}")
    public ResponseEntity<LigneFonctionnelleResponse> updateLigneFonctionnelle(
            @PathVariable Long id,
            @Valid @RequestBody LigneFonctionnelleRequest request) {
        logger.info("Updating ligne fonctionnelle with ID: {}", id);
        logger.debug("Update request details: {}", request);
        LigneFonctionnelleResponse updatedLigne = ligneFonctionnelleService.updateLigneFonctionnelle(id, request);
        logger.info("Successfully updated ligne fonctionnelle ID: {}", id);
        return ResponseEntity.ok(updatedLigne);
    }



    @GetMapping("/{id}")
    public ResponseEntity<LigneFonctionnelleResponse> getLigneFonctionnelleById(@PathVariable Long id) {
        logger.info("Fetching ligne fonctionnelle by ID: {}", id);
        LigneFonctionnelleResponse ligne = ligneFonctionnelleService.getLigneFonctionnelleById(id);
        logger.debug("Successfully retrieved ligne fonctionnelle ID: {}", id);
        return ResponseEntity.ok(ligne);
    }


    @GetMapping
    public ResponseEntity<Page<LigneFonctionnelleResponse>> getPaginatedLigneFonctionnelles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
                @RequestParam(defaultValue = "id,asc") String[] sort,
            @ModelAttribute LigneFonctionnelleSearchCriteria searchCriteria) {

        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        logger.info("Fetching paginated lignes fonctionnelles - Page: {}, Size: {}, Sort: {} {}", page, size, sort[0], sort[1]);
        logger.debug("Search criteria: {}", searchCriteria);
        Page<LigneFonctionnelleResponse> lignes = ligneFonctionnelleService.getPaginatedLigneFonctionnelles(searchCriteria, pageable);

        logger.info("Successfully retrieved {} ligne(s) fonctionnelle(s) out of {} total (Page {}/{})",
                lignes.getNumberOfElements(),
                lignes.getTotalElements(),
                lignes.getNumber() + 1,
                lignes.getTotalPages());

        return ResponseEntity.ok(lignes);
    }

    /**
     * Deactivates a specific  functional line by id
     * PUT /lignes-fonctionnelles/deactivate/{ligneId}
     * @param ligneId The ID of the functional line.
     * @return HTTP status 204 No Content on success.
     */
    @PutMapping("/deactivate/{ligneId}")
    public ResponseEntity<Void> deactivateLigneFonctionnelle(
            @PathVariable Long ligneId)
    {
        logger.info("Attempting to deactivate ligne fonctionnelle with ID: {}", ligneId);
        ligneFonctionnelleService.deactivateLigneFonctionnelle(ligneId);
        logger.info("Successfully deactivated ligne fonctionnelle ID: {}", ligneId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLigneFonctionnelle(@PathVariable Long id) {
        logger.info("Attempting to delete ligne fonctionnelle with ID: {}", id);
        ligneFonctionnelleService.deleteLigneFonctionnelle(id);
        logger.info("Successfully deleted ligne fonctionnelle ID: {}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint dédié pour la gestion (création, mise à jour, suppression) des Sous-lignes
     * d'une Ligne Mère spécifique.
     * PUT /lignes-fonctionnelles/{ligneMereId}/sous-lignes
     * @param ligneMereId L'ID de la Ligne Mère.
     * @param sousLignesRequests La liste complète des Sous-lignes désirées.
     * @return La Ligne Mère mise à jour (incluant les nouvelles Sous-lignes).
     */
    @PutMapping("/{ligneMereId}/sous-lignes")
    public ResponseEntity<LigneFonctionnelleResponse> manageSousLignes(
            @PathVariable Long ligneMereId,
            @Valid @RequestBody List<SousLigneRequestDto> sousLignesRequests) {
        logger.info("Managing sous-lignes for ligne mère ID: {}", ligneMereId);
        logger.debug("Number of sous-lignes requests: {}", sousLignesRequests.size());
        logger.debug("Sous-lignes requests details: {}", sousLignesRequests);

        LigneFonctionnelleResponse updatedLigneMere = ligneFonctionnelleService.manageSousLignes(ligneMereId, sousLignesRequests);
        logger.info("Successfully managed sous-lignes for ligne mère ID: {}", ligneMereId);

        return ResponseEntity.ok(updatedLigneMere);
    }

    /**
     * Get functional lines associated with a specific bank ID, with pagination, sorting,
     * and search by nature ligne fonctionnelle, year of dateDepart, and 'actif' status.
     * GET /lignes-fonctionnelles/banque/{banqueId}
     * @param banqueId The ID of the bank.
     * @param page Page number (0-indexed, default 0).
     * @param size Page size (default 10).
     * @param sort Sort criteria (e.g., "dateDepart,desc", "plafond,asc").
     * @param searchCriteria DTO for search filters (natureLigneFonctionnelleId, yearDepart, actif).
     * @return A paginated list of functional lines as DTOs with HTTP status 200 OK.
     */
    @GetMapping("/banque/{banqueId}")
    public ResponseEntity<Page<LigneFonctionnelleResponse>> getPaginatedLignesFonctionnellesByBanqueId(
            @PathVariable Long banqueId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort,
            @ModelAttribute LigneFonctionnelleSearchCriteria searchCriteria) {

        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        logger.info("Fetching paginated lignes fonctionnelles for banque ID: {} - Page: {}, Size: {}, Sort: {} {}",
                banqueId, page, size, sort[0], sort[1]);
        logger.debug("Search criteria for banque {}: {}", banqueId, searchCriteria);

        Page<LigneFonctionnelleResponse> lignes = ligneFonctionnelleService.getPaginatedLignesFonctionnellesByBanqueId(banqueId, searchCriteria, pageable);

        logger.info("Successfully retrieved {} ligne(s) fonctionnelle(s) for banque ID: {} out of {} total (Page {}/{})",
                lignes.getNumberOfElements(),
                banqueId,
                lignes.getTotalElements(),
                lignes.getNumber() + 1,
                lignes.getTotalPages());

        return ResponseEntity.ok(lignes);
    }

    /**
     * Retrieves historical plafond and taux data for a specific bank and nature of functional line,
     * formatted for a PrimeNG line chart.
     * GET /lignes-fonctionnelles/history
     *
     * @return A LigneFonctionnelleChartData object with HTTP status 200 OK.
     */
    @GetMapping("/history")
    public ResponseEntity<LigneFonctionnelleChartData> getLigneFonctionnelleChartHistory(
            @RequestParam("banqueId") Long banqueId,
            @RequestParam("natureLigneFonctionnelleId") Long natureLigneFonctionnelleId) {

        logger.info("Fetching historical chart data for banque ID: {}, nature ligne fonctionnelle ID: {}",
                banqueId, natureLigneFonctionnelleId);
        LigneFonctionnelleChartData chartData = ligneFonctionnelleService.getHistoricalChartData(banqueId, natureLigneFonctionnelleId);
        logger.info("Successfully retrieved historical chart data - {} data points", chartData.getLabels() != null ? chartData.getLabels().size() : 0);
        logger.debug("Chart data details: {}", chartData);
        return ResponseEntity.ok(chartData);
    }
}