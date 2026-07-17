package staport.finance.gBanque.rapprochement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.rapprochement.dtos.RapprochementActifsDto;
import staport.finance.gBanque.rapprochement.dtos.RapprochementRequestDto;
import staport.finance.gBanque.rapprochement.dtos.RapprochementResponseDto;
import staport.finance.gBanque.rapprochement.dtos.RapprochementSearchDto;
import staport.finance.gBanque.rapprochement.services.IRapprochementService;
import staport.finance.gBanque.reports.enums.ReportType;
import staport.finance.gBanque.transactions.dtos.TransactionSearchDto;

/**
 * Contrôleur REST pour le module Rapprochement.
 *
 * <p>Injecte {@link IRapprochementService} (DIP — jamais la classe concrète). Aucune logique
 * métier ni journalisation ici — les logs métier vivent exclusivement dans le Service
 * (voir {@code conventions/logging.md}).
 *
 * <p>Autorisations (cf. {@code docs/rbac/rapprochement.md}) : lecture et actions de clôture
 * réservées à Finance (membre ou directeur) ou {@code SYSTEM_ADMIN} — y compris la clôture
 * définitive, qui verrouille irréversiblement la période : le garde-fou métier réel est le statut
 * (transitions strictement ordonnées, voir le Service), pas une restriction de rôle supplémentaire.
 */
@Tag(name = "Rapprochement", description = "Gestion des périodes de rapprochement bancaire mensuel")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/rapprochement")
@PreAuthorize(RapprochementController.FINANCE_OR_ADMIN)
public class RapprochementController {

    static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";

    private final IRapprochementService rapprochementService;

    public RapprochementController(IRapprochementService rapprochementService) {
        this.rapprochementService = rapprochementService;
    }

    @Operation(summary = "Snapshot des rapprochements actifs d'une banque")
    @GetMapping("/banque/{banqueId}/actifs")
    public ResponseEntity<RapprochementActifsDto> getActifs(@PathVariable Long banqueId) {
        return ResponseEntity.ok(rapprochementService.getActifs(banqueId));
    }

    @Operation(summary = "Clôturer provisoirement le rapprochement en cours")
    @ApiResponse(responseCode = "400", description = "Une clôture provisoire existe déjà pour cette banque")
    @PostMapping("/cloturer")
    public ResponseEntity<RapprochementResponseDto> cloturerProvisoire(@RequestBody RapprochementRequestDto requestDto) {
        return ResponseEntity.ok(rapprochementService.cloturerProvisoire(requestDto.banqueId()));
    }

    @Operation(summary = "Clôturer définitivement un rapprochement provisoire")
    @ApiResponse(responseCode = "400", description = "Le rapprochement n'est pas en clôture provisoire")
    @ApiResponse(responseCode = "404", description = "Rapprochement introuvable")
    @PostMapping("/{id}/cloturer-definitivement")
    public ResponseEntity<RapprochementResponseDto> cloturerDefinitivement(@PathVariable Long id) {
        return ResponseEntity.ok(rapprochementService.cloturerDefinitivement(id));
    }

    @Operation(summary = "Détail d'un rapprochement (transactions paginées)")
    @ApiResponse(responseCode = "404", description = "Rapprochement introuvable")
    @GetMapping("/{id}")
    public ResponseEntity<RapprochementResponseDto> getRapprochementById(
            @PathVariable Long id,
            @ModelAttribute TransactionSearchDto searchDto,
            Pageable pageable) {

        RapprochementResponseDto dto = rapprochementService.getRapprochementById(id, searchDto, pageable);

        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Rechercher des rapprochements")
    @GetMapping
    public ResponseEntity<Page<RapprochementResponseDto>> searchRapprochements(
            RapprochementSearchDto searchDto,
            Pageable pageable) {

        Page<RapprochementResponseDto> rapprochementPage =
                rapprochementService.searchRapprochements(searchDto, pageable);

        return ResponseEntity.ok(rapprochementPage);
    }

    @Operation(summary = "Télécharger le rapport PDF d'un rapprochement")
    @ApiResponse(responseCode = "404", description = "Rapprochement introuvable")
    @GetMapping("/{id}/report/pdf")
    public ResponseEntity<byte[]> downloadRapprochementReport(
            @PathVariable Long id,
            @RequestParam(defaultValue = "FINANCE") ReportType reportType) {

        // 1. Générer le contenu du rapport (byte[])
        byte[] pdfContent = rapprochementService.generateRapprochementPdf(id, reportType);

        // 2. Définir le nom du fichier pour le téléchargement
        String filename = "Rapport_Rapprochement_" + id + ".pdf";

        // 3. Configurer les En-têtes HTTP
        HttpHeaders headers = new HttpHeaders();

        // Indique le type de contenu (PDF)
        headers.setContentType(MediaType.APPLICATION_PDF);

        // Force le téléchargement plutôt que l'affichage dans le navigateur
        headers.setContentDispositionFormData("attachment", filename);

        // Indique la taille du fichier
        headers.setContentLength(pdfContent.length);

        // 4. Retourner la réponse
        return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
    }
}
