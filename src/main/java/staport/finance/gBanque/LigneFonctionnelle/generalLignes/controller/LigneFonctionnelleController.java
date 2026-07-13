package staport.finance.gBanque.LigneFonctionnelle.generalLignes.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.INatureLigneFonctionnelleService;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.dtos.*;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.services.ILigneFonctionnelleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import staport.finance.gBanque.droitConstate.dtos.response.AgregatedSuivieDcLigneResponseDto;
import staport.finance.gBanque.moyensDePaiements.lcn.dtos.AgregatedSuivieLcnLigneResponseDto;
import staport.finance.gBanque.factoring.dtos.response.AgregatedSuivieFactoringLigneResponseDto;
import staport.finance.gBanque.prefinancement.dtos.response.AgregatedSuiviePrefiLigneResponseDto;
import staport.finance.gBanque.common.PageableUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for the LigneFonctionnelle (générale) module.
 * Injects {@link ILigneFonctionnelleService} (DIP — never la classe concrète).
 *
 * <p>RBAC au niveau classe : ce module expose des données financières propres au département
 * Finance (plafonds, encours, suivi des opérations) — pas un référentiel de dropdown inter-modules
 * comme {@code Nature}. Mirroring {@code CreditMoyenTermeController}/{@code ContratLeasingController}.
 *
 * <p>Injecte également {@link IBanqueService} et {@link INatureLigneFonctionnelleService} pour
 * résoudre les entités Banque/NatureLigneFonctionnelle ici, au niveau Controller — jamais dans
 * {@code LigneFonctionnelleService}. Raison : {@code BanqueServiceImpl} et
 * {@code NatureLigneFonctionnelleService} injectent déjà {@link ILigneFonctionnelleService} ;
 * si {@code LigneFonctionnelleService} injectait ces deux Services en retour, cela créerait un
 * cycle de beans Spring au démarrage. Le Controller n'est jamais injecté ailleurs, donc il peut
 * relier les deux domaines sans risque (architecture.md — encapsulation des domaines).
 */
@Tag(name = "Lignes fonctionnelles", description = "Gestion des lignes fonctionnelles générales (autorisations de financement par banque)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/lignes-fonctionnelles")
public class LigneFonctionnelleController {

    private static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
    private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";

    private final ILigneFonctionnelleService ligneFonctionnelleService;
    private final IBanqueService banqueService;
    private final INatureLigneFonctionnelleService natureLigneFonctionnelleService;

    public LigneFonctionnelleController(ILigneFonctionnelleService ligneFonctionnelleService,
                                        IBanqueService banqueService,
                                        INatureLigneFonctionnelleService natureLigneFonctionnelleService) {
        this.ligneFonctionnelleService = ligneFonctionnelleService;
        this.banqueService = banqueService;
        this.natureLigneFonctionnelleService = natureLigneFonctionnelleService;
    }

    /** Résout, pour chaque {@code natureLigneFonctionnelleId} distinct des sous-lignes fournies, l'entité correspondante. */
    private Map<Long, NatureLigneFonctionnelle> resolveSousLigneNatures(List<SousLigneRequestDto> sousLignes) {
        Map<Long, NatureLigneFonctionnelle> natures = new HashMap<>();
        if (sousLignes == null) {
            return natures;
        }
        for (SousLigneRequestDto sousLigne : sousLignes) {
            natures.computeIfAbsent(sousLigne.natureLigneFonctionnelleId(), natureLigneFonctionnelleService::findOrThrow);
        }
        return natures;
    }

    @Operation(summary = "Créer une ligne fonctionnelle générale")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ligne créée"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "404", description = "Banque, nature ou marché introuvable"),
            @ApiResponse(responseCode = "409", description = "Ligne active en doublon")
    })
    @PostMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<LigneFonctionnelleResponse> createLigneFonctionnelle(@Valid @RequestBody LigneFonctionnelleRequest request) {
        Banque banque = banqueService.findOrThrow(request.banqueId());
        NatureLigneFonctionnelle nature = natureLigneFonctionnelleService.findOrThrow(request.natureLigneFonctionnelleId());
        Map<Long, NatureLigneFonctionnelle> sousLigneNatures = resolveSousLigneNatures(request.sousLignes());

        LigneFonctionnelleResponse createdLigne = ligneFonctionnelleService.createLigneFonctionnelle(request, banque, nature, sousLigneNatures);
        return new ResponseEntity<>(createdLigne, HttpStatus.CREATED);
    }

    @Operation(summary = "Mettre à jour une ligne fonctionnelle générale")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "404", description = "Ligne, banque, nature ou marché introuvable"),
            @ApiResponse(responseCode = "409", description = "Ligne active en doublon")
    })
    @PutMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<LigneFonctionnelleResponse> updateLigneFonctionnelle(
            @PathVariable Long id,
            @Valid @RequestBody LigneFonctionnelleRequest request) {
        Banque banque = banqueService.findOrThrow(request.banqueId());
        NatureLigneFonctionnelle nature = natureLigneFonctionnelleService.findOrThrow(request.natureLigneFonctionnelleId());
        return ResponseEntity.ok(ligneFonctionnelleService.updateLigneFonctionnelle(id, request, banque, nature));
    }

    @Operation(summary = "Détail d'une ligne fonctionnelle par ID")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Ligne introuvable")
    })
    @GetMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<LigneFonctionnelleResponse> getLigneFonctionnelleById(@PathVariable Long id) {
        return ResponseEntity.ok(ligneFonctionnelleService.getLigneFonctionnelleById(id));
    }

    @Operation(summary = "Liste paginée des lignes fonctionnelles (mères/indépendantes)")
    @GetMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<Page<LigneFonctionnelleResponse>> getPaginatedLigneFonctionnelles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort,
            @ModelAttribute LigneFonctionnelleSearchCriteria searchCriteria) {

        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, PageableUtils.clampSize(size), sortOrder);

        return ResponseEntity.ok(ligneFonctionnelleService.getPaginatedLigneFonctionnelles(searchCriteria, pageable));
    }

    @Operation(summary = "Lignes fonctionnelles d'un marché")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Marché introuvable")
    })
    @GetMapping("/marche/{marcheId}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<Page<LigneFonctionnelleResponse>> getPaginatedLignesFonctionnellesByMarcheId(
            @PathVariable Long marcheId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort,
            @ModelAttribute LigneFonctionnelleSearchCriteria searchCriteria) {

        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, PageableUtils.clampSize(size), sortOrder);

        return ResponseEntity.ok(ligneFonctionnelleService.getPaginatedLignesFonctionnellesByMarcheId(marcheId, searchCriteria, pageable));
    }

    @Operation(summary = "Désactiver une ligne fonctionnelle")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ligne désactivée"),
            @ApiResponse(responseCode = "404", description = "Ligne introuvable")
    })
    @PutMapping("/deactivate/{ligneId}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<Void> deactivateLigneFonctionnelle(@PathVariable Long ligneId) {
        ligneFonctionnelleService.deactivateLigneFonctionnelle(ligneId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Supprimer une ligne fonctionnelle", description = "Réservé SYSTEM_ADMIN. Interdit directement sur une sous-ligne — passer par la ligne mère.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ligne supprimée"),
            @ApiResponse(responseCode = "400", description = "Suppression directe d'une sous-ligne interdite"),
            @ApiResponse(responseCode = "404", description = "Ligne introuvable")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<Void> deleteLigneFonctionnelle(@PathVariable Long id) {
        ligneFonctionnelleService.deleteLigneFonctionnelle(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Gérer les sous-lignes d'une ligne mère", description = "Remplace l'intégralité des sous-lignes par la liste fournie (création/mise à jour/suppression).")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "ID de sous-ligne fourni à la création, ou nature non autorisée pour une ligne mère"),
            @ApiResponse(responseCode = "404", description = "Ligne mère ou nature introuvable")
    })
    @PutMapping("/{ligneMereId}/sous-lignes")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<LigneFonctionnelleResponse> manageSousLignes(
            @PathVariable Long ligneMereId,
            @Valid @RequestBody List<SousLigneRequestDto> sousLignesRequests) {
        Map<Long, NatureLigneFonctionnelle> sousLigneNatures = resolveSousLigneNatures(sousLignesRequests);
        return ResponseEntity.ok(ligneFonctionnelleService.manageSousLignes(ligneMereId, sousLignesRequests, sousLigneNatures));
    }

    @Operation(summary = "Lignes fonctionnelles d'une banque")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Banque introuvable")
    })
    @GetMapping("/banque/{banqueId}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<Page<LigneFonctionnelleResponse>> getPaginatedLignesFonctionnellesByBanqueId(
            @PathVariable Long banqueId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort,
            @ModelAttribute LigneFonctionnelleSearchCriteria searchCriteria) {

        banqueService.findOrThrow(banqueId);

        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, PageableUtils.clampSize(size), sortOrder);

        return ResponseEntity.ok(ligneFonctionnelleService.getPaginatedLignesFonctionnellesByBanqueId(banqueId, searchCriteria, pageable));
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
        return ResponseEntity.ok(ligneFonctionnelleService.getSuiviePrefiLigneResponse(id, pageable));
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
        return ResponseEntity.ok(ligneFonctionnelleService.getSuivieDcLigneResponse(id, pageable));
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
        return ResponseEntity.ok(ligneFonctionnelleService.getSuivieFactoringLigneResponse(id, pageable));
    }

    @Operation(summary = "Suivi LCN d'une ligne")
    @GetMapping("/{id}/lcn")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<AgregatedSuivieLcnLigneResponseDto> getLcnByLineId(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort) {

        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, PageableUtils.clampSize(size), sortOrder);
        return ResponseEntity.ok(ligneFonctionnelleService.getSuivieLcnLigneResponse(id, pageable));
    }

    @Operation(
            summary = "Historique taux/plafond pour un graphique",
            description = "Formaté pour un graphique PrimeNG, par banque et nature de ligne fonctionnelle."
    )
    @GetMapping("/history")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<LigneFonctionnelleChartData> getLigneFonctionnelleChartHistory(
            @Parameter(description = "ID de la banque") @RequestParam("banqueId") Long banqueId,
            @Parameter(description = "ID de la nature de ligne fonctionnelle") @RequestParam("natureLigneFonctionnelleId") Long natureLigneFonctionnelleId) {

        banqueService.findOrThrow(banqueId);
        natureLigneFonctionnelleService.findOrThrow(natureLigneFonctionnelleId);

        return ResponseEntity.ok(ligneFonctionnelleService.getHistoricalChartData(banqueId, natureLigneFonctionnelleId));
    }
}
