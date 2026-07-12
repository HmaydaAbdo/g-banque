package staport.finance.gBanque.cautions.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.cautions.commissions.service.IGenerationCommissionCautionService;
import staport.finance.gBanque.cautions.commissions.service.ResultatGenerationCommissionDto;
import staport.finance.gBanque.cautions.dtos.request.CautionCreateDto;
import staport.finance.gBanque.cautions.dtos.response.CautionResponseDto;
import staport.finance.gBanque.cautions.dtos.request.CautionSearchCriteria;
import staport.finance.gBanque.cautions.dtos.request.CautionUpdateDto;
import staport.finance.gBanque.cautions.dtos.request.CloturerCautionRequest;
import staport.finance.gBanque.cautions.dtos.response.CloturerCautionResponse;
import staport.finance.gBanque.cautions.exceptions.CautionExceptionHandler;
import staport.finance.gBanque.cautions.services.ICautionService;
import staport.finance.gBanque.common.PageableUtils;

import java.time.LocalDate;
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
 * <p>Autorisations (cf. {@code docs/rbac/caution.md}) :
 * <ul>
 *   <li>Lecture, création, modification, clôture, export, génération manuelle des
 *       commissions — Finance (membre ou directeur) ou {@code SYSTEM_ADMIN}.</li>
 *   <li>Suppression définitive — {@code SYSTEM_ADMIN} uniquement (règle transverse,
 *       {@code conventions/security.md}).</li>
 * </ul>
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
@Tag(name = "Cautions", description = "Gestion des cautions bancaires")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/cautions")
public class CautionController {

    private static final Logger log = LoggerFactory.getLogger(CautionController.class);

    private static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
    private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";

    private final IGenerationCommissionCautionService generationService;
    private final ICautionService cautionService;

    public CautionController(IGenerationCommissionCautionService generationService, ICautionService cautionService) {
        this.generationService = generationService;
        this.cautionService = cautionService;
    }

    // ── Création ──────────────────────────────────────────────────────────────

    @Operation(summary = "Créer une caution")
    @PostMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<CautionResponseDto> creer(@Valid @RequestBody CautionCreateDto dto) {
        log.info("POST /cautions — numeroCaution: {}", dto.numeroCaution());
        CautionResponseDto created = cautionService.creer(dto);
        log.info("Caution créée — id: {}", created.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── Lecture par ID ────────────────────────────────────────────────────────

    @Operation(summary = "Récupérer une caution par ID")
    @GetMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<CautionResponseDto> getById(@PathVariable Long id) {
        log.debug("GET /cautions/{}", id);
        return ResponseEntity.ok(cautionService.getById(id));
    }

    // ── Recherche paginée ─────────────────────────────────────────────────────

    @Operation(summary = "Recherche paginée des cautions")
    @GetMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<Page<CautionResponseDto>> rechercher(
            @RequestParam(defaultValue = "0")              int page,
            @RequestParam(defaultValue = "10")             int size,
            @RequestParam(defaultValue = "dateDePrise")    String[] sort,
            @ModelAttribute CautionSearchCriteria criteres) {

        String sortField       = sort[0];
        Sort.Direction direction = (sort.length > 1)
                ? Sort.Direction.fromOptionalString(sort[1]).orElse(Sort.Direction.DESC)
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, PageableUtils.clampSize(size), Sort.by(direction, sortField));

        log.debug("GET /cautions — page={}, size={}, sort={} {}, critères={}",
                page, size, sortField, direction, criteres);

        return ResponseEntity.ok(cautionService.rechercher(criteres, pageable));
    }

    // ── Export PDF ────────────────────────────────────────────────────────────

    /**
     * Exporte en PDF l'intégralité des cautions correspondant aux critères fournis
     * (mêmes filtres que {@code GET /cautions}) — sans pagination : le fichier contient
     * tous les résultats filtrés, pas seulement la page actuellement affichée à l'écran.
     */
    @Operation(summary = "Exporter les cautions en PDF",
            description = "Ignore la pagination — exporte tous les résultats filtrés.")
    @GetMapping("/export/pdf")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<byte[]> exporterPdf(@ModelAttribute CautionSearchCriteria criteres) {
        log.info("GET /cautions/export/pdf — critères: {}", criteres);

        byte[] pdf = cautionService.exporterPdf(criteres);
        String filename = "cautions_" + LocalDate.now() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ── Modification ──────────────────────────────────────────────────────────

    @Operation(summary = "Modifier une caution")
    @PutMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<CautionResponseDto> modifier(
            @PathVariable Long id,
            @Valid @RequestBody CautionUpdateDto dto) {

        log.info("PUT /cautions/{}", id);
        return ResponseEntity.ok(cautionService.modifier(id, dto));
    }

    // ── Suppression (soft-delete) ─────────────────────────────────────────────

    @Operation(summary = "Supprimer définitivement une caution",
            description = "Réservé SYSTEM_ADMIN. Bloqué si la caution est archivée (actif = false).")
    @DeleteMapping("/{id}")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        log.info("DELETE /cautions/{}", id);
        cautionService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    // ── Clôture (main levée) ──────────────────────────────────────────────────

    @Operation(summary = "Clôturer une caution (main levée)",
            description = "Génère la commission de clôture (et la TVA si la stratégie de la banque l'exige) puis marque la caution inactive.")
    @PostMapping("/{id}/cloturer")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<CloturerCautionResponse> cloturer(
            @PathVariable Long id,
            @Valid @RequestBody CloturerCautionRequest request) {

        log.info("POST /cautions/{}/cloturer — dateDeMainLevee: {}", id, request.dateDeMainLevee());
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
    @Operation(summary = "Générer manuellement les commissions du mois",
            description = "debutMois absent = les deux lots ; false = fin de mois (AWB, BMCI, SAHAM) ; true = début de mois (CDM).")
    @PostMapping("/generer-commissions")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<ResultatGenerationCommissionDto> genererCommissions(
            @RequestParam int annee,
            @RequestParam int mois,
            @RequestParam(required = false) Boolean debutMois) {

        log.info("POST /cautions/generer-commissions — {}-{}, debutMois={}",
                annee, mois, debutMois == null ? "both" : debutMois);

        YearMonth moisReference = YearMonth.of(annee, mois);

        if (debutMois != null) {
            // Lot ciblé : uniquement fin de mois OU début de mois
            ResultatGenerationCommissionDto result = generationService.generer(moisReference, debutMois);
            log.info("Génération ciblée ({}) terminée — {} créée(s), {} ignorée(s)",
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

        log.info("Génération complète terminée — {} créée(s), {} ignorée(s)",
                result.generees(), result.ignorees());
        return ResponseEntity.ok(result);
    }
}
