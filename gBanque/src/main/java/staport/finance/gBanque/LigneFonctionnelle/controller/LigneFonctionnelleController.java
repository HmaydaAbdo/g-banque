package staport.finance.gBanque.LigneFonctionnelle.controller;

import staport.finance.gBanque.LigneFonctionnelle.dtos.LigneFonctionnelleRequest;
import staport.finance.gBanque.LigneFonctionnelle.dtos.LigneFonctionnelleResponse;
import staport.finance.gBanque.LigneFonctionnelle.dtos.LigneFonctionnelleSearchCriteria;
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
    public ResponseEntity<Page<LigneFonctionnelleResponse>> getCurrentLigneFonctionnelles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort,
            @ModelAttribute LigneFonctionnelleSearchCriteria searchCriteria) {

        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        Page<LigneFonctionnelleResponse> lignes = ligneFonctionnelleService.getCurrentLigneFonctionnelles(searchCriteria, pageable);
        return ResponseEntity.ok(lignes);
    }

    /**
     * Get all functional lines associated with a specific bank ID.
     * GET /lignes-fonctionnelles/banque/{banqueId}
     * @param banqueId The ID of the bank.
     * @return A list of functional lines as DTOs with HTTP status 200 OK.
     */
    @GetMapping("/banque/{banqueId}")
    public ResponseEntity<List<LigneFonctionnelleResponse>> getLignesFonctionnellesByBanqueId(@PathVariable Long banqueId) {
        List<LigneFonctionnelleResponse> lignes = ligneFonctionnelleService.getLignesFonctionnellesByBanqueId(banqueId);
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
}
