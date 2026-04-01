package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleRequest;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleResponse;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleSearchCriteria;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.enums.PorteeNature;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.exceptions.NatureLFExceptionHandler;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.INatureLigneFonctionnelleService;

import java.util.List;

/**
 * REST controller for the NatureLigneFonctionnelle module.
 * Injects {@link INatureLigneFonctionnelleService} (DIP — never the concrete class).
 * Exception handling is delegated to {@link NatureLFExceptionHandler}.
 */
@RestController
@RequestMapping("/natures-lignes-fonctionnelles")
public class NatureLigneFonctionnelleController {

    private static final Logger logger = LoggerFactory.getLogger(NatureLigneFonctionnelleController.class);

    private final INatureLigneFonctionnelleService service;

    public NatureLigneFonctionnelleController(INatureLigneFonctionnelleService service) {
        this.service = service;
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<NatureLigneFonctionnelleResponse> createNatureLigneFonctionnelle(
            @Valid @RequestBody NatureLigneFonctionnelleRequest request) {

        logger.info("POST /natures-lignes-fonctionnelles — label: {}", request.label());
        NatureLigneFonctionnelleResponse created = service.createNatureLigneFonctionnelle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<NatureLigneFonctionnelleResponse> getNatureLigneFonctionnelleById(
            @PathVariable Long id) {

        logger.debug("GET /natures-lignes-fonctionnelles/{}", id);
        return ResponseEntity.ok(service.getNatureLigneFonctionnelleById(id));
    }

    /**
     * Paginated search.
     * GET /natures-lignes-fonctionnelles?page=0&size=10&sort=label&sort=asc&label=FC
     */
    @GetMapping
    public ResponseEntity<Page<NatureLigneFonctionnelleResponse>> getAllNatureLigneFonctionnelles(
            @ModelAttribute NatureLigneFonctionnelleSearchCriteria searchCriteria,
            Pageable pageable) {

        logger.debug("GET /natures-lignes-fonctionnelles — criteria: {}, pageable: {}",
                searchCriteria, pageable);

        return ResponseEntity.ok(service.getAllNatureLigneFonctionnelles(searchCriteria, pageable));
    }

    /**
     * Non-paginated list — for dropdowns / select inputs.
     * GET /natures-lignes-fonctionnelles/list
     * GET /natures-lignes-fonctionnelles/list?portee=GENERALE
     * GET /natures-lignes-fonctionnelles/list?portee=SPECIFIQUE

     * Si portee est absent → toutes les natures.
     * Si portee = GENERALE → natures GENERALE + GENERALE_ET_SPECIFIQUE.
     * Si portee = SPECIFIQUE → natures SPECIFIQUE + GENERALE_ET_SPECIFIQUE.
     */
    @GetMapping("/list")
    public ResponseEntity<List<NatureLigneFonctionnelleResponse>> getAllNatureLigneFonctionnellesList(
            @RequestParam(required = false) PorteeNature portee) {
        logger.debug("GET /natures-lignes-fonctionnelles/list — portee: {}", portee);
        return ResponseEntity.ok(service.getAllNatureLigneFonctionnellesList(portee));
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<NatureLigneFonctionnelleResponse> updateNatureLigneFonctionnelle(
            @PathVariable Long id,
            @Valid @RequestBody NatureLigneFonctionnelleRequest request) {

        logger.info("PUT /natures-lignes-fonctionnelles/{}", id);
        return ResponseEntity.ok(service.updateNatureLigneFonctionnelle(id, request));
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNatureLigneFonctionnelle(@PathVariable Long id) {
        logger.info("DELETE /natures-lignes-fonctionnelles/{}", id);
        service.deleteNatureLigneFonctionnelle(id);
        return ResponseEntity.noContent().build();
    }
}
