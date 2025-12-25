package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleRequest;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleResponse;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleSearchCriteria;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.NatureLigneFonctionnelleService;
import java.util.List;

@RestController
@RequestMapping("/natures-lignes-fonctionnelles")
public class NatureLigneFonctionnelleController {

    private final NatureLigneFonctionnelleService service;

    @Autowired
    public NatureLigneFonctionnelleController(NatureLigneFonctionnelleService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<NatureLigneFonctionnelleResponse> createNatureLigneFonctionnelle(@Valid @RequestBody NatureLigneFonctionnelleRequest request) {
        NatureLigneFonctionnelleResponse created = service.createNatureLigneFonctionnelle(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NatureLigneFonctionnelleResponse> getNatureLigneFonctionnelleById(@PathVariable Long id) {
        NatureLigneFonctionnelleResponse response = service.getNatureLigneFonctionnelleById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all functional line natures with pagination, sorting, and search criteria.
     * GET /natures-lignes-fonctionnelles?page=0&size=10&sort=label,asc&label=FACILITE
     * @param page The page number (0-indexed).
     * @param size The number of items per page.
     * @param sort The sorting criteria (e.g., "id,asc" or "label,desc").
     * @param searchCriteria Search criteria for label and description.
     * @return A Page of NatureLigneFonctionnelleResponse DTOs.
     */
    @GetMapping
    public ResponseEntity<Page<NatureLigneFonctionnelleResponse>> getAllNatureLigneFonctionnelles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort,
            @ModelAttribute NatureLigneFonctionnelleSearchCriteria searchCriteria) { // Use @ModelAttribute for search criteria

        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        Page<NatureLigneFonctionnelleResponse> responses = service.getAllNatureLigneFonctionnelles(searchCriteria, pageable);
        return ResponseEntity.ok(responses);
    }

    /**
     * Retrieves all functional line natures as a simple list (non-paginated).
     * Useful for dropdowns or selection lists where full pagination is not required.
     * GET /natures-lignes-fonctionnelles/list
     * @return A List of NatureLigneFonctionnelleResponse DTOs.
     */
    @GetMapping("/list")
    public ResponseEntity<List<NatureLigneFonctionnelleResponse>> getAllNatureLigneFonctionnellesList() {
        List<NatureLigneFonctionnelleResponse> responses = service.getAllNatureLigneFonctionnellesList();
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NatureLigneFonctionnelleResponse> updateNatureLigneFonctionnelle(@PathVariable Long id, @Valid @RequestBody NatureLigneFonctionnelleRequest request) {
        NatureLigneFonctionnelleResponse updated = service.updateNatureLigneFonctionnelle(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNatureLigneFonctionnelle(@PathVariable Long id) {
        service.deleteNatureLigneFonctionnelle(id);
        return ResponseEntity.noContent().build();
    }
}