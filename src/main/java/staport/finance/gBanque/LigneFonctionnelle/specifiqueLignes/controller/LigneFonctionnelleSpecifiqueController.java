package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.INatureLigneFonctionnelleService;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.*;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.services.ILigneFonctionnelleSpecifiqueService;
import staport.finance.gBanque.droitConstate.dtos.AgregatedSuivieDcLigneResponseDto;
import staport.finance.gBanque.factoring.dtos.AgregatedSuivieFactoringLigneResponseDto;
import staport.finance.gBanque.prefinancement.dtos.AgregatedSuiviePrefiLigneResponseDto;
import staport.finance.gBanque.common.PageableUtils;

/**
 * REST controller for the LigneFonctionnelleSpecifique module.
 * Injects {@link ILigneFonctionnelleSpecifiqueService} (DIP — jamais la classe concrète).
 *
 * RBAC identique à {@code LigneFonctionnelleController} (générale) : donnée financière propre au
 * département Finance, pas un référentiel dropdown.
 *
 * <p>Injecte également {@link INatureLigneFonctionnelleService} et {@link IBanqueService} pour
 * résoudre les entités ici, au niveau Controller — mêmes raisons que
 * {@code LigneFonctionnelleController} (générale) : évite le cycle de beans avec
 * {@code NatureLigneFonctionnelleService} (qui injecte déjà {@code ILigneFonctionnelleSpecifiqueService}).
 */
@Tag(name = "Lignes fonctionnelles spécifiques", description = "Gestion des lignes fonctionnelles spécifiques (rattachées à un marché)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/lignes-fonctionnelles-specifiques")
public class LigneFonctionnelleSpecifiqueController {

    private static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
    private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";

    private final ILigneFonctionnelleSpecifiqueService ligneFonctionnelleSpecifiqueService;
    private final INatureLigneFonctionnelleService natureLigneFonctionnelleService;
    private final IBanqueService banqueService;

    public LigneFonctionnelleSpecifiqueController(ILigneFonctionnelleSpecifiqueService ligneFonctionnelleSpecifiqueService,
                                                   INatureLigneFonctionnelleService natureLigneFonctionnelleService,
                                                   IBanqueService banqueService) {
        this.ligneFonctionnelleSpecifiqueService = ligneFonctionnelleSpecifiqueService;
        this.natureLigneFonctionnelleService = natureLigneFonctionnelleService;
        this.banqueService = banqueService;
    }

    @Operation(summary = "Créer une ligne fonctionnelle spécifique")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ligne créée"),
            @ApiResponse(responseCode = "400", description = "Données invalides ou portée de nature incompatible"),
            @ApiResponse(responseCode = "404", description = "Nature ou marché introuvable"),
            @ApiResponse(responseCode = "409", description = "Ligne active en doublon")
    })
    @PostMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<LigneFonctionnelleSpecifiqueResponse> createLigneFonctionnelleSpecifique(@Valid @RequestBody LigneFonctionnelleSpecifiqueRequest request) {
        NatureLigneFonctionnelle nature = natureLigneFonctionnelleService.findOrThrow(request.natureLigneFonctionnelleId());
        LigneFonctionnelleSpecifiqueResponse createdLigne = ligneFonctionnelleSpecifiqueService.createLigneFonctionnelleSpecifique(request, nature);
        return new ResponseEntity<>(createdLigne, HttpStatus.CREATED);
    }

    @Operation(summary = "Détail d'une ligne fonctionnelle spécifique par ID")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Ligne introuvable")
    })
    @GetMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<LigneFonctionnelleSpecifiqueResponse> getLigneFonctionnelleSpecifiqueById(@PathVariable Long id) {
        return ResponseEntity.ok(ligneFonctionnelleSpecifiqueService.getLigneFonctionnelleSpecifiqueById(id));
    }

    @Operation(summary = "Liste paginée des lignes fonctionnelles spécifiques")
    @GetMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<Page<LigneFonctionnelleSpecifiqueResponse>> getLignesFonctionnellesSpecifiques(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort,
            @ModelAttribute LigneFonctionnelleSpecifiqueSearchCriteria searchCriteria) {

        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, PageableUtils.clampSize(size), sortOrder);

        return ResponseEntity.ok(ligneFonctionnelleSpecifiqueService.getLignesFonctionnellesSpecifiques(searchCriteria, pageable));
    }

    @Operation(summary = "Mettre à jour une ligne fonctionnelle spécifique")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Données invalides ou portée de nature incompatible"),
            @ApiResponse(responseCode = "404", description = "Ligne, nature ou marché introuvable"),
            @ApiResponse(responseCode = "409", description = "Ligne active en doublon")
    })
    @PutMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<LigneFonctionnelleSpecifiqueResponse> updateLigneFonctionnelleSpecifique(
            @PathVariable Long id,
            @Valid @RequestBody LigneFonctionnelleSpecifiqueRequest request) {
        NatureLigneFonctionnelle nature = request.natureLigneFonctionnelleId() != null
                ? natureLigneFonctionnelleService.findOrThrow(request.natureLigneFonctionnelleId())
                : null;
        return ResponseEntity.ok(ligneFonctionnelleSpecifiqueService.updateLigneFonctionnelleSpecifique(id, request, nature));
    }

    @Operation(summary = "Lignes fonctionnelles spécifiques d'une banque")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Banque introuvable")
    })
    @GetMapping("/banque/{banqueId}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<Page<LigneFonctionnelleSpecifiqueResponse>> getLignesFonctionnellesSpecifiquesByBanqueId(
            @PathVariable Long banqueId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort,
            @ModelAttribute LigneFonctionnelleSpecifiqueSearchCriteria searchCriteria) {

        banqueService.findOrThrow(banqueId);

        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, PageableUtils.clampSize(size), sortOrder);

        return ResponseEntity.ok(ligneFonctionnelleSpecifiqueService.getLignesFonctionnellesSpecifiquesByBanqueId(banqueId, searchCriteria, pageable));
    }

    @Operation(summary = "Désactiver une ligne fonctionnelle spécifique")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ligne désactivée"),
            @ApiResponse(responseCode = "400", description = "Ligne déjà désactivée"),
            @ApiResponse(responseCode = "404", description = "Ligne introuvable")
    })
    @PutMapping("/deactivate/{ligneId}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<Void> deactivateLigneFonctionnelle(@PathVariable Long ligneId) throws Exception {
        ligneFonctionnelleSpecifiqueService.deactivateLigneFonctionnelleById(ligneId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Supprimer une ligne fonctionnelle spécifique", description = "Réservé SYSTEM_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ligne supprimée"),
            @ApiResponse(responseCode = "404", description = "Ligne introuvable")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<Void> deleteLigneFonctionnelleSpecifique(@PathVariable Long id) {
        ligneFonctionnelleSpecifiqueService.deleteLigneFonctionnelleSpecifique(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Lignes fonctionnelles spécifiques d'un marché")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Marché introuvable")
    })
    @GetMapping("/marche/{marcheId}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<Page<LigneFonctionnelleSpecifiqueResponse>> getLinesByMarche(
            @PathVariable Long marcheId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort) {

        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, PageableUtils.clampSize(size), sortOrder);

        return ResponseEntity.ok(ligneFonctionnelleSpecifiqueService.getLinesByMarche(marcheId, pageable));
    }

    @Operation(summary = "Suivi des préfinancements d'une ligne")
    @GetMapping("/{id}/prefinancements")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<AgregatedSuiviePrefiLigneResponseDto> getPrefinancementsByLineId(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort) {

        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, PageableUtils.clampSize(size), sortOrder);
        return ResponseEntity.ok(ligneFonctionnelleSpecifiqueService.getSuiviePrefiLigneResponse(id, pageable));
    }

    @Operation(summary = "Suivi des droits constatés d'une ligne")
    @GetMapping("/{id}/dc")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<AgregatedSuivieDcLigneResponseDto> getDroitConstateByLineId(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort) {

        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, PageableUtils.clampSize(size), sortOrder);
        return ResponseEntity.ok(ligneFonctionnelleSpecifiqueService.getSuivieDcLigneResponse(id, pageable));
    }

    @Operation(summary = "Suivi du factoring d'une ligne")
    @GetMapping("/{id}/factoring")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<AgregatedSuivieFactoringLigneResponseDto> getFactoringByLineId(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort) {

        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, PageableUtils.clampSize(size), sortOrder);
        return ResponseEntity.ok(ligneFonctionnelleSpecifiqueService.getSuivieFactoringLigneResponse(id, pageable));
    }
}
