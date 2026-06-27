package staport.finance.gBanque.Bank.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.multipart.MultipartFile;
import staport.finance.gBanque.Bank.dtos.request.BanqueRequest;
import staport.finance.gBanque.Bank.dtos.request.BanqueSearchCriteria;
import staport.finance.gBanque.Bank.dtos.response.BankAcceuilResponse;
import staport.finance.gBanque.Bank.dtos.response.BanqueResponse;
import staport.finance.gBanque.Bank.exceptions.BanqueExceptionHandler;
import staport.finance.gBanque.Bank.services.BankAccueilAggregatorService;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.common.PageableUtils;
import java.util.List;

/**
 * REST controller for the Banque module.
 *
 * Injects {@link IBanqueService} (DIP — never the concrete implementation).
 * Exception handling is delegated to {@link BanqueExceptionHandler}.
 * No business logic lives here.
 *
 * <p>RBAC par méthode (pas au niveau classe — restrictions non uniformes) :
 * <ul>
 *   <li>Lecture liste/détail ({@code GET /banques}, {@code GET /banques/{id}}) — tout authentifié :
 *       données de référence utilisées par d'autres modules (dropdowns).</li>
 *   <li>Accueil info ({@code GET /banques/accueil-info[/{id}]}) — Finance ou SYSTEM_ADMIN :
 *       tableau de bord Finance uniquement.</li>
 *   <li>Mutations (create/update/delete) — Finance ou SYSTEM_ADMIN.</li>
 * </ul>
 */
@Tag(name = "Comptes bancaires", description = "Gestion des comptes bancaires et tableau de bord Finance")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/banques")
public class BanqueController {

    private static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";

    private final IBanqueService banqueService;
    private final BankAccueilAggregatorService accueilAggregatorService;

    public BanqueController(IBanqueService banqueService, BankAccueilAggregatorService accueilAggregatorService) {
        this.banqueService = banqueService;
        this.accueilAggregatorService = accueilAggregatorService;
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @Operation(summary = "Créer une banque", description = "Requiert un logo (multipart). Réservé Finance / SYSTEM_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Banque créée"),
            @ApiResponse(responseCode = "400", description = "Données invalides ou logo manquant"),
            @ApiResponse(responseCode = "409", description = "Numéro de compte déjà existant")
    })
    @PostMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<BanqueResponse> createBanque(
            @Valid @RequestPart("banque") BanqueRequest request,
            @RequestPart("logoFile") MultipartFile logoFile) {

        BanqueResponse created = banqueService.createBanque(request, logoFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Détail d'une banque par ID")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Banque introuvable")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BanqueResponse> getBanqueById(@PathVariable Long id) {
        return ResponseEntity.ok(banqueService.getBanqueById(id));
    }

    @Operation(summary = "Liste paginée des banques")
    @Parameter(name = "nom", description = "Filtre LIKE sur le nom")
    @GetMapping
    public ResponseEntity<Page<BanqueResponse>> getAllBanques(
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "10")   int size,
            @RequestParam(defaultValue = "id")   String[] sort,
            @ModelAttribute BanqueSearchCriteria searchCriteria) {

        String         sortField = sort[0];
        Sort.Direction direction = (sort.length > 1)
                ? Sort.Direction.fromOptionalString(sort[1]).orElse(Sort.Direction.ASC)
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, PageableUtils.clampSize(size), Sort.by(direction, sortField));
        return ResponseEntity.ok(banqueService.getAllBanques(searchCriteria, pageable));
    }

    @Operation(
            summary = "Tableau de bord Finance — toutes les banques",
            description = "Agrège solde, transactions en attente, LCN et virements échus par banque. Réservé Finance / SYSTEM_ADMIN."
    )
    @GetMapping("/accueil-info")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<List<BankAcceuilResponse>> getAccueilBankInformation() {
        return ResponseEntity.ok(accueilAggregatorService.getAccueilBankInfo());
    }

    @Operation(
            summary = "Tableau de bord Finance — une banque",
            description = "Même agrégation que /accueil-info, restreinte à la banque indiquée."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Banque introuvable")
    })
    @GetMapping("/accueil-info/{banqueId}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<BankAcceuilResponse> getAccueilBankInformationById(
            @PathVariable Long banqueId) {
        return ResponseEntity.ok(accueilAggregatorService.getAccueilBankInfoById(banqueId));
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @Operation(summary = "Mettre à jour une banque", description = "logoFile optionnel — omis = logo inchangé.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "404", description = "Banque introuvable")
    })
    @PutMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<BanqueResponse> updateBanque(
            @PathVariable Long id,
            @Valid @RequestPart("banque") BanqueRequest request,
            @RequestPart(value = "logoFile", required = false) MultipartFile logoFile) {

        return ResponseEntity.ok(banqueService.updateBanque(id, request, logoFile));
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Operation(
            summary = "Supprimer une banque",
            description = "Bloqué si des lignes fonctionnelles actives existent, si des transactions sont en attente, ou si le solde est non nul."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Banque supprimée"),
            @ApiResponse(responseCode = "404", description = "Banque introuvable"),
            @ApiResponse(responseCode = "409", description = "Transactions en attente ou solde non nul")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<Void> deleteBanque(@PathVariable Long id) {
        banqueService.deleteBanque(id);
        return ResponseEntity.noContent().build();
    }
}
