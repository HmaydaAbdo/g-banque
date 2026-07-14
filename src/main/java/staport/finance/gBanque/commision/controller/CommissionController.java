package staport.finance.gBanque.commision.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.commision.dtos.*;
import staport.finance.gBanque.commision.service.ICommissionService;

import java.time.LocalDate;

/**
 * Autorisations (cf. {@code docs/rbac/commission.md}) : réservé Finance/SYSTEM_ADMIN sur toutes
 * les actions (donnée financière propre au département Finance, mirroring
 * {@code CautionController}/{@code ImpotEtTaxeController}), suppression et changement de statut
 * (PREVU/PAYE) réservés SYSTEM_ADMIN.
 */
@Tag(name = "Commissions", description = "Gestion des commissions bancaires générées manuellement ou automatiquement")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/commissions")
public class CommissionController {

    private static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
    private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";

    private static final Logger log = LogManager.getLogger(CommissionController.class);
    private final ICommissionService commissionService;

    public CommissionController(ICommissionService commissionService) {
        this.commissionService = commissionService;
    }

    // --- Create ---
    @Operation(summary = "Créer une commission")
    @PostMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<CommissionResponseDto> createCommission(
            @Validated @RequestBody CommissionRequestDto requestDto) {
        log.info("REST request to create commission: {}", requestDto);
        CommissionResponseDto responseDto = commissionService.createCommission(requestDto);
        return ResponseEntity.ok(responseDto);
    }

    // --- Get by ID ---
    @Operation(summary = "Récupérer une commission par ID")
    @GetMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<CommissionResponseDto> getById(@PathVariable Long id) {
        log.info("REST request to get commission with id={}", id);
        CommissionResponseDto responseDto = commissionService.getById(id);
        return ResponseEntity.ok(responseDto);
    }

    // --- Search with pagination + aggregate total ---
    @Operation(summary = "Recherche paginée des commissions")
    @PostMapping("/search")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<EncoursCommissionDto> searchCommissions(
            @RequestBody CommissionSearchDto searchDto,
            Pageable pageable) {
        log.info("REST request to search commissions with criteria: {}", searchDto);
        return ResponseEntity.ok(commissionService.searchCommissions(searchDto, pageable));
    }

    // --- Update ---
    @Operation(summary = "Modifier une commission")
    @PutMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<CommissionResponseDto> updateCommission(
            @PathVariable Long id,
            @Validated @RequestBody CommissionRequestDto requestDto) {
        log.info("REST request to update commission id={}", id);
        CommissionResponseDto responseDto = commissionService.updateCommission(id, requestDto);
        return ResponseEntity.ok(responseDto);
    }

    // --- Delete ---
    @Operation(summary = "Supprimer une commission", description = "Réservé SYSTEM_ADMIN.")
    @DeleteMapping("/{id}")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<Void> deleteCommission(@PathVariable Long id) {
        log.info("REST request to delete commission id={}", id);
        commissionService.deleteCommission(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves a paginated list of outstanding commissions and their total amount for a specific bank.
     *
     * @param banqueId  The ID of the bank to filter by.
     * @param searchDto Optional search criteria (e.g., for filtering by libelle, etc.).
     * @param pageable  Pagination and sorting information.
     * @return An EncoursCommissionDto containing the page of commissions and the total outstanding amount.
     */
    @Operation(summary = "Consulter les encours de commissions d'une banque")
    @GetMapping("/encours/{banqueId}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<EncoursCommissionDto> getOutstandingCommissions(
            @PathVariable Long banqueId,
            @ModelAttribute CommissionSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("REST request to get outstanding commissions for banqueId={}", banqueId);
        searchDto.setBanqueId(banqueId); // Set the bank ID from the path variable
        EncoursCommissionDto result = commissionService.getOutstandingCommissions(searchDto, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * Exporte en PDF l'intégralité des commissions correspondant aux critères fournis
     * (mêmes filtres que {@code POST /commissions/search}, {@code natureId} inclus) — sans
     * pagination : le fichier contient tous les résultats filtrés, pas seulement la page
     * actuellement affichée à l'écran.
     */
    @Operation(summary = "Exporter les commissions filtrées en PDF")
    @GetMapping("/export/pdf")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<byte[]> exporterPdf(@ModelAttribute CommissionSearchDto searchDto) {
        log.info("GET /commissions/export/pdf — critères: {}", searchDto);

        byte[] pdf = commissionService.exporterPdf(searchDto);
        String filename = "commissions_" + LocalDate.now() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @Operation(summary = "Changer le statut PREVU/PAYE", description = "Réservé SYSTEM_ADMIN.")
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<Void> toggleCommissionStatus(@PathVariable Long id) {
        log.info("REST request pour basculer le statut de la commission avec l'id={}", id);
        commissionService.toggleCommisionStatus(id);
        return ResponseEntity.noContent().build();
    }

    // --- Fusion ---
    /**
     * Fusionne plusieurs commissions sur cautions en une seule.
     * Contraintes : même banque, même dateEmission, status=PREVU, operation=CAUTION.
     */
    @Operation(summary = "Fusionner plusieurs commissions sur cautions",
            description = "Contraintes : même banque, même dateEmission, status=PREVU, operation=CAUTION.")
    @PostMapping("/fusionner")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<CommissionFusionResponse> fusionner(
            @Validated @RequestBody CommissionFusionRequest request) {
        log.info("REST request to fusionner {} commissions, libellé='{}'",
                request.getIds().size(), request.getLibelle());
        CommissionFusionResponse response = commissionService.fusionner(request);
        return ResponseEntity.ok(response);
    }

}
