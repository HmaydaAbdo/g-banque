package staport.finance.gBanque.LigneFonctionnelle.controller;

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

@RestController
@RequestMapping("/lignes-fonctionnelles")
public class LigneFonctionnelleController {

    private final LigneFonctionnelleService ligneFonctionnelleService;

    @Autowired
    public LigneFonctionnelleController(LigneFonctionnelleService ligneFonctionnelleService) {
        this.ligneFonctionnelleService = ligneFonctionnelleService;
    }

    @PostMapping
    public ResponseEntity<LigneFonctionnelleResponse> createLigneFonctionnelle(@Valid @RequestBody LigneFonctionnelleRequest request) {
        LigneFonctionnelleResponse createdLigne = ligneFonctionnelleService.createLigneFonctionnelle(request);
        return new ResponseEntity<>(createdLigne, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LigneFonctionnelleResponse> getLigneFonctionnelleById(@PathVariable Long id) {
        LigneFonctionnelleResponse ligne = ligneFonctionnelleService.getLigneFonctionnelleById(id);
        return ResponseEntity.ok(ligne);
    }


    @GetMapping
    public ResponseEntity<Page<LigneFonctionnelleResponse>> getActiveLigneFonctionnelles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort,
            @ModelAttribute LigneFonctionnelleSearchCriteria searchCriteria) {

        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        Page<LigneFonctionnelleResponse> lignes = ligneFonctionnelleService.getLigneFonctionnelles(searchCriteria, pageable);
        return ResponseEntity.ok(lignes);
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
    public ResponseEntity<Page<LigneFonctionnelleResponse>> getLignesFonctionnellesByBanqueId(
            @PathVariable Long banqueId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort,
            @ModelAttribute LigneFonctionnelleSearchCriteria searchCriteria) {

        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        Page<LigneFonctionnelleResponse> lignes = ligneFonctionnelleService.getLignesFonctionnellesByBanqueId(banqueId, searchCriteria, pageable);
        return ResponseEntity.ok(lignes);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LigneFonctionnelleResponse> updateLigneFonctionnelle(
            @PathVariable Long id,
            @Valid @RequestBody LigneFonctionnelleRequest request) {
        LigneFonctionnelleResponse updatedLigne = ligneFonctionnelleService.updateLigneFonctionnelle(id, request);
        return ResponseEntity.ok(updatedLigne);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLigneFonctionnelle(@PathVariable Long id) {
        ligneFonctionnelleService.deleteLigneFonctionnelle(id);
        return ResponseEntity.noContent().build();
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
        ligneFonctionnelleService.deactivateLigneFonctionnelleByNatureAndBank(ligneId);
        return ResponseEntity.noContent().build();
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

        LigneFonctionnelleChartData chartData = ligneFonctionnelleService.getHistoricalChartData(banqueId, natureLigneFonctionnelleId);

        return ResponseEntity.ok(chartData);
    }
}