package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleRequest;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleResponse;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleSearchCriteria;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.enums.PorteeNature;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.exceptions.NatureLFExceptionHandler;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.INatureLigneFonctionnelleService;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for the NatureLigneFonctionnelle module.
 * Injects {@link INatureLigneFonctionnelleService} (DIP — never the concrete class).
 * Exception handling is delegated to {@link NatureLFExceptionHandler}.
 *
 * <p>RBAC par méthode (pas au niveau classe — restrictions non uniformes) :
 * <ul>
 *   <li>Lecture ({@code GET /{id}}, {@code GET}, {@code GET /list}) — tout authentifié :
 *       référentiel utilisé par d'autres modules (dropdowns générale/spécifique).</li>
 *   <li>Création/mise à jour — Finance ou SYSTEM_ADMIN.</li>
 *   <li>Suppression — SYSTEM_ADMIN uniquement.</li>
 * </ul>
 */
@Tag(name = "Natures de lignes fonctionnelles", description = "Référentiel des natures de lignes fonctionnelles (générales et/ou spécifiques)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/natures-lignes-fonctionnelles")
public class NatureLigneFonctionnelleController {

    private static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
    private static final String SYSTEM_ADMIN  = "hasAuthority('SYSTEM_ADMIN')";
    private static final String AUTHENTICATED = "isAuthenticated()";

    private final INatureLigneFonctionnelleService service;

    public NatureLigneFonctionnelleController(INatureLigneFonctionnelleService service) {
        this.service = service;
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @Operation(summary = "Créer une nature de ligne fonctionnelle")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Nature créée"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "409", description = "Code ou label déjà utilisé")
    })
    @PostMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<NatureLigneFonctionnelleResponse> createNatureLigneFonctionnelle(
            @Valid @RequestBody NatureLigneFonctionnelleRequest request) {

        NatureLigneFonctionnelleResponse created = service.createNatureLigneFonctionnelle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Détail d'une nature par ID")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Nature introuvable")
    })
    @GetMapping("/{id}")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<NatureLigneFonctionnelleResponse> getNatureLigneFonctionnelleById(
            @PathVariable Long id) {

        return ResponseEntity.ok(service.getNatureLigneFonctionnelleById(id));
    }

    @Operation(summary = "Liste paginée des natures")
    @Parameter(name = "portee", description = "Filtre exact sur la portée (GENERALE, SPECIFIQUE, GENERALE_ET_SPECIFIQUE)")
    @GetMapping
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<Page<NatureLigneFonctionnelleResponse>> getAllNatureLigneFonctionnelles(
            @ModelAttribute NatureLigneFonctionnelleSearchCriteria searchCriteria,
            Pageable pageable) {

        return ResponseEntity.ok(service.getAllNatureLigneFonctionnelles(searchCriteria, pageable));
    }

    @Operation(
            summary = "Liste non paginée pour dropdowns",
            description = "Si portee est absent, retourne toutes les natures. Si portee=GENERALE, retourne " +
                    "GENERALE + GENERALE_ET_SPECIFIQUE. Si portee=SPECIFIQUE, retourne SPECIFIQUE + GENERALE_ET_SPECIFIQUE."
    )
    @GetMapping("/list")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<List<NatureLigneFonctionnelleResponse>> getAllNatureLigneFonctionnellesList(
            @RequestParam(required = false) PorteeNature portee) {
        return ResponseEntity.ok(service.getAllNatureLigneFonctionnellesList(portee));
    }

    @Operation(summary = "Exporter en PDF les natures filtrées",
            description = "Mêmes critères que GET /natures-lignes-fonctionnelles — sans pagination, le fichier contient tous les résultats filtrés")
    @GetMapping("/export/pdf")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<byte[]> exportNaturesPdf(@ModelAttribute NatureLigneFonctionnelleSearchCriteria searchCriteria) {
        byte[] pdf = service.exportNaturesPdf(searchCriteria);
        String filename = "natures_lignes_fonctionnelles_" + LocalDate.now() + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @Operation(summary = "Mettre à jour une nature", description = "Le code est immuable et ignoré s'il est fourni.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "404", description = "Nature introuvable"),
            @ApiResponse(responseCode = "409", description = "Label déjà utilisé")
    })
    @PutMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<NatureLigneFonctionnelleResponse> updateNatureLigneFonctionnelle(
            @PathVariable Long id,
            @Valid @RequestBody NatureLigneFonctionnelleRequest request) {

        return ResponseEntity.ok(service.updateNatureLigneFonctionnelle(id, request));
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Operation(
            summary = "Supprimer une nature",
            description = "Réservé SYSTEM_ADMIN. Bloqué si référencée par une ligne fonctionnelle (générale ou spécifique)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Nature supprimée"),
            @ApiResponse(responseCode = "404", description = "Nature introuvable"),
            @ApiResponse(responseCode = "409", description = "Nature référencée par une ligne fonctionnelle")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<Void> deleteNatureLigneFonctionnelle(@PathVariable Long id) {
        service.deleteNatureLigneFonctionnelle(id);
        return ResponseEntity.noContent().build();
    }
}
