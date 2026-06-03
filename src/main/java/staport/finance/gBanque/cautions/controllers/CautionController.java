package staport.finance.gBanque.cautions.controllers;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.cautions.commissions.service.IGenerationCommissionCautionService;
import staport.finance.gBanque.cautions.commissions.service.ResultatGenerationCommissionDto;
import staport.finance.gBanque.cautions.dtos.CautionCreateDto;
import staport.finance.gBanque.cautions.dtos.CautionResponseDto;
import staport.finance.gBanque.cautions.dtos.CautionSearchCriteria;
import staport.finance.gBanque.cautions.dtos.CautionUpdateDto;
import staport.finance.gBanque.cautions.dtos.CloturerCautionRequest;
import staport.finance.gBanque.cautions.dtos.CloturerCautionResponse;
import staport.finance.gBanque.cautions.exceptions.CautionExceptionHandler;
import staport.finance.gBanque.cautions.services.ICautionService;

import java.time.YearMonth;

/**
 * Contrôleur REST pour le module Caution.
 *
 * <p>Injecte {@link ICautionService} (DIP — jamais la classe concrète).
 * Aucune logique métier ici — uniquement routage HTTP, validation des entrées,
 * et construction des réponses.
 *
 * <p>La gestion des exceptions est déléguée à {@link CautionExceptionHandler}.
 *
 * <p>Endpoints :
 * <ul>
 *   <li>POST   /cautions             — créer une caution</li>
 *   <li>GET    /cautions/{id}        — récupérer par ID</li>
 *   <li>GET    /cautions             — recherche paginée avec filtres</li>
 *   <li>PUT    /cautions/{id}        — modifier une caution</li>
 *   <li>PATCH  /cautions/{id}/toggle-actif — activer / désactiver manuellement</li>
 *   <li>DELETE /cautions/{id}        — soft-delete (actif = false)</li>
 * </ul>
 */
@RestController
@RequestMapping("/cautions")
public class CautionController {

    private static final Logger logger = LoggerFactory.getLogger(CautionController.class);
    private final IGenerationCommissionCautionService generationService;
    private final ICautionService cautionService;

    public CautionController(IGenerationCommissionCautionService generationService, ICautionService cautionService) {
        this.generationService = generationService;
        this.cautionService = cautionService;
    }

    // ── Création ──────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<CautionResponseDto> creer(@Valid @RequestBody CautionCreateDto dto) {
        logger.info("POST /cautions — numeroCaution: {}", dto.numeroCaution());
        CautionResponseDto created = cautionService.creer(dto);
        logger.info("Caution créée — id: {}", created.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── Lecture par ID ────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<CautionResponseDto> getById(@PathVariable Long id) {
        logger.debug("GET /cautions/{}", id);
        return ResponseEntity.ok(cautionService.getById(id));
    }

    // ── Recherche paginée ─────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<Page<CautionResponseDto>> rechercher(
            @RequestParam(defaultValue = "0")              int page,
            @RequestParam(defaultValue = "10")             int size,
            @RequestParam(defaultValue = "dateDePrise")    String[] sort,
            @ModelAttribute CautionSearchCriteria criteres) {

        String sortField       = sort[0];
        Sort.Direction direction = (sort.length > 1)
                ? Sort.Direction.fromOptionalString(sort[1]).orElse(Sort.Direction.DESC)
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        logger.debug("GET /cautions — page={}, size={}, sort={} {}, critères={}",
                page, size, sortField, direction, criteres);

        return ResponseEntity.ok(cautionService.rechercher(criteres, pageable));
    }

    // ── Modification ──────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<CautionResponseDto> modifier(
            @PathVariable Long id,
            @Valid @RequestBody CautionUpdateDto dto) {

        logger.info("PUT /cautions/{}", id);
        return ResponseEntity.ok(cautionService.modifier(id, dto));
    }

    // ── Suppression (soft-delete) ─────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        logger.info("DELETE /cautions/{}", id);
        cautionService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    // ── Clôture (main levée) ──────────────────────────────────────────────────

    @PostMapping("/{id}/cloturer")
    public ResponseEntity<CloturerCautionResponse> cloturer(
            @PathVariable Long id,
            @Valid @RequestBody CloturerCautionRequest request) {

        logger.info("POST /cautions/{}/cloturer — dateDeMainLevee: {}", id, request.dateDeMainLevee());
        return ResponseEntity.ok(cautionService.cloturer(id, request));
    }

    /**
     * Génère manuellement les commissions cautions pour un mois donné.
     *
     * <p>Paramètre optionnel {@code debutMois} :
     * <ul>
     *   <li>Absent (null) → génère les <b>deux</b> lots (fin de mois + début de mois) — comportement par défaut</li>
     *   <li>{@code false}  → génère uniquement les commissions de <b>fin de mois</b> (AWB, BMCI, SAHAM)</li>
     *   <li>{@code true}   → génère uniquement les commissions de <b>début de mois</b> (CDM)</li>
     * </ul>
     *
     * <p>Exemples :
     * <pre>
     *   POST /cautions/generer-commissions?annee=2026&amp;mois=6             → les deux lots
     *   POST /cautions/generer-commissions?annee=2026&amp;mois=6&amp;debutMois=false → fin de mois uniquement (AWB, BMCI, SAHAM)
     *   POST /cautions/generer-commissions?annee=2026&amp;mois=6&amp;debutMois=true  → début de mois uniquement (CDM)
     * </pre>
     */
    @PostMapping("/generer-commissions")
    public ResponseEntity<ResultatGenerationCommissionDto> genererCommissions(
            @RequestParam int annee,
            @RequestParam int mois,
            @RequestParam(required = false) Boolean debutMois) {

        logger.info("POST /cautions/generer-commissions — {}-{}, debutMois={}",
                annee, mois, debutMois == null ? "both" : debutMois);

        YearMonth moisReference = YearMonth.of(annee, mois);

        if (debutMois != null) {
            // Lot ciblé : uniquement fin de mois OU début de mois
            ResultatGenerationCommissionDto result = generationService.generer(moisReference, debutMois);
            logger.info("Génération ciblée ({}) terminée — {} créée(s), {} ignorée(s)",
                    debutMois ? "début de mois" : "fin de mois",
                    result.generees(), result.ignorees());
            return ResponseEntity.ok(result);
        }

        // Les deux lots : comportement historique
        ResultatGenerationCommissionDto finDeMois   = generationService.generer(moisReference, false);
        ResultatGenerationCommissionDto debutDeMois = generationService.generer(moisReference, true);

        ResultatGenerationCommissionDto result = new ResultatGenerationCommissionDto(
                finDeMois.generees() + debutDeMois.generees(),
                finDeMois.ignorees() + debutDeMois.ignorees(),
                moisReference
        );

        logger.info("Génération complète terminée — {} créée(s), {} ignorée(s)",
                result.generees(), result.ignorees());
        return ResponseEntity.ok(result);
    }
}
