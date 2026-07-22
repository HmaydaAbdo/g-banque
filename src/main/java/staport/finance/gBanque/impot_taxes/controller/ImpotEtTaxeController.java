package staport.finance.gBanque.impot_taxes.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.impot_taxes.dtos.request.EncoursImpotEtTaxeSearchDto;
import staport.finance.gBanque.impot_taxes.dtos.request.ImpotEtTaxeFusionRequest;
import staport.finance.gBanque.impot_taxes.dtos.request.ImpotEtTaxeRequestDto;
import staport.finance.gBanque.impot_taxes.dtos.request.ImpotEtTaxeSearchDto;
import staport.finance.gBanque.impot_taxes.dtos.response.EncoursImpotEtTaxeDto;
import staport.finance.gBanque.impot_taxes.dtos.response.ImpotEtTaxeResponseDto;
import staport.finance.gBanque.impot_taxes.service.IImpotEtTaxeService;

import java.time.LocalDate;

/**
 * Injecte {@link IImpotEtTaxeService} (DIP — jamais la classe concrète).
 *
 * <p>Autorisations (cf. {@code docs/rbac/impots-taxes.md}) : réservé Finance/SYSTEM_ADMIN sur
 * toutes les actions (donnée financière propre au département Finance, mirroring
 * {@code CautionController}/{@code DecompteController}), suppression et changement de statut
 * (PREVU/PAYE) réservés SYSTEM_ADMIN.
 */
@Tag(name = "Impôts et Taxes", description = "Gestion des impôts et taxes (TVA, IR, IS, pénalités...) rattachés aux banques")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/impots-taxes")
public class ImpotEtTaxeController {

    private static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
    private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";

    private final IImpotEtTaxeService impotEtTaxeService;

    public ImpotEtTaxeController(IImpotEtTaxeService impotEtTaxeService) {
        this.impotEtTaxeService = impotEtTaxeService;
    }

    @Operation(summary = "Créer un impôt/taxe")
    @PostMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<ImpotEtTaxeResponseDto> createImpotEtTaxe(@Valid @RequestBody ImpotEtTaxeRequestDto requestDto) {
        return ResponseEntity.ok(impotEtTaxeService.createImpotEtTaxe(requestDto));
    }

    @Operation(summary = "Récupérer un impôt/taxe par ID")
    @GetMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<ImpotEtTaxeResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(impotEtTaxeService.getById(id));
    }

    @Operation(summary = "Recherche paginée des impôts/taxes")
    @PostMapping("/search")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<EncoursImpotEtTaxeDto> searchImpotEtTaxes(@RequestBody ImpotEtTaxeSearchDto searchDto, Pageable pageable) {
        return ResponseEntity.ok(impotEtTaxeService.searchImpotEtTaxes(searchDto, pageable));
    }

    /**
     * Exporte en PDF l'intégralité des impôts/taxes correspondant aux critères fournis (mêmes
     * filtres que {@code POST /impots-taxes/search}) — sans pagination : le fichier contient tous
     * les résultats filtrés, pas seulement la page actuellement affichée à l'écran. Réutilisé par
     * l'écran "TVA sur cautions" (filtre {@code motif=TVA_COMMISSION_CAUTION}).
     */
    @Operation(summary = "Exporter les impôts/taxes filtrés en PDF",
            description = "Ignore la pagination — exporte tous les résultats filtrés.")
    @GetMapping("/export/pdf")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<byte[]> exporterPdf(@ModelAttribute ImpotEtTaxeSearchDto searchDto) {
        byte[] pdf = impotEtTaxeService.exporterPdf(searchDto);
        String filename = "impots_taxes_" + LocalDate.now() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @Operation(summary = "Modifier un impôt/taxe")
    @PutMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<ImpotEtTaxeResponseDto> updateImpotEtTaxe(@PathVariable Long id, @Valid @RequestBody ImpotEtTaxeRequestDto requestDto) {
        return ResponseEntity.ok(impotEtTaxeService.updateImpotEtTaxe(id, requestDto));
    }

    @Operation(summary = "Supprimer un impôt/taxe", description = "Réservé SYSTEM_ADMIN.")
    @DeleteMapping("/{id}")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<Void> deleteImpotEtTaxe(@PathVariable Long id) {
        impotEtTaxeService.deleteImpotEtTaxe(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Consulter les encours d'impôts/taxes d'une banque")
    @GetMapping("/encours/{banqueId}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<EncoursImpotEtTaxeDto> getOutstandingImpotEtTaxes(
            @PathVariable Long banqueId,
            @ModelAttribute EncoursImpotEtTaxeSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        return ResponseEntity.ok(impotEtTaxeService.getOutstandingImpotEtTaxes(searchDto, banqueId, pageable));
    }

    @Operation(summary = "Changer le statut PREVU/PAYE", description = "Réservé SYSTEM_ADMIN.")
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<ImpotEtTaxeResponseDto> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(impotEtTaxeService.toggleStatus(id));
    }

    @Operation(summary = "Fusionner plusieurs TVA en une seule",
            description = "Contraintes : même banque, même dateEmission, même motif, status=PREVU.")
    @PostMapping("/fusionner")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<ImpotEtTaxeResponseDto> fusionner(@Valid @RequestBody ImpotEtTaxeFusionRequest request) {
        return ResponseEntity.ok(impotEtTaxeService.fusionner(request));
    }
}
