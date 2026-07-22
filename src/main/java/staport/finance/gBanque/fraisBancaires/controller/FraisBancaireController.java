package staport.finance.gBanque.fraisBancaires.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.fraisBancaires.dtos.*;
import staport.finance.gBanque.fraisBancaires.service.IFraisBancaireService;

import java.time.LocalDate;

/**
 * Injecte {@link IFraisBancaireService} (DIP — jamais la classe concrète).
 *
 * <p>Autorisations (cf. {@code docs/rbac/frais-bancaires.md}) : réservé Finance/SYSTEM_ADMIN sur
 * les actions courantes (donnée financière propre au département Finance, mirroring
 * {@code ImpayeeController}/{@code ImpotEtTaxeController}), suppression et changement de statut
 * (PREVU/PAYE) réservés SYSTEM_ADMIN.
 */
@Tag(name = "Frais bancaires", description = "Gestion des frais bancaires (charges fixes et ponctuelles) rattachés aux banques")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/frais-bancaire")
public class FraisBancaireController {

    private static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
    private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";

    private static final Logger log = LoggerFactory.getLogger(FraisBancaireController.class);
    private final IFraisBancaireService fraisBancaireService;

    public FraisBancaireController(IFraisBancaireService fraisBancaireService) {
        this.fraisBancaireService = fraisBancaireService;
    }

    @Operation(summary = "Créer un frais bancaire")
    @PostMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<FraisBancaireResponseDto> createFraisBancaire(
            @Validated @RequestBody FraisBancaireRequestDto requestDto) {
        log.info("Requête REST pour créer un frais bancaire - Libellé: '{}'", requestDto.libelle());
        FraisBancaireResponseDto response = fraisBancaireService.createFraisBancaire(requestDto);
        log.info("Frais bancaire créé avec succès - ID: {}", response.id());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Récupérer un frais bancaire par ID")
    @GetMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<FraisBancaireResponseDto> getById(@PathVariable Long id) {
        log.info("Requête REST pour récupérer le frais bancaire ID: {}", id);
        return ResponseEntity.ok(fraisBancaireService.getById(id));
    }

    @Operation(summary = "Recherche paginée des frais bancaires")
    @PostMapping("/search")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<Page<FraisBancaireResponseDto>> searchFraisBancaire(
            @RequestBody FraisBancaireSearchDto searchDto, Pageable pageable) {
        log.info("Requête REST pour rechercher des frais bancaires - Critères: {}", searchDto);
        Page<FraisBancaireResponseDto> page = fraisBancaireService.searchFraisBancaire(searchDto, pageable);
        log.info("Recherche terminée - {} résultat(s)", page.getTotalElements());
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Exporter en PDF les frais bancaires",
            description = "Mêmes critères que POST /frais-bancaire/search — sans pagination")
    @GetMapping("/export/pdf")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<byte[]> exportFraisBancairePdf(FraisBancaireSearchDto searchDto) {
        log.info("Requête REST pour exporter en PDF les frais bancaires - Critères: {}", searchDto);
        byte[] pdf = fraisBancaireService.exportFraisBancairePdf(searchDto);
        String filename = "frais_bancaires_" + LocalDate.now() + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @Operation(summary = "Modifier un frais bancaire")
    @PutMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<FraisBancaireResponseDto> updateFraisBancaire(
            @PathVariable Long id, @Validated @RequestBody FraisBancaireRequestDto requestDto) {
        log.info("Requête REST pour mettre à jour le frais bancaire ID: {}", id);
        FraisBancaireResponseDto response = fraisBancaireService.updateFraisBancaire(id, requestDto);
        log.info("Frais bancaire ID: {} mis à jour avec succès", id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Supprimer un frais bancaire", description = "Réservé SYSTEM_ADMIN. Refusé si le frais bancaire n'est plus au statut PREVU.")
    @DeleteMapping("/{id}")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<Void> deleteFraisBancaire(@PathVariable Long id) {
        log.info("Requête REST pour supprimer le frais bancaire ID: {}", id);
        fraisBancaireService.deleteFraisBancaire(id);
        log.info("Frais bancaire ID: {} supprimé avec succès", id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Consulter les encours de frais bancaires d'une banque")
    @GetMapping("/encours/{banqueId}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<EncoursFraisBancaireDto> getOutstandingFraisBancaire(
            @PathVariable Long banqueId,
            @ModelAttribute EncoursFraisBancaireSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("Requête REST pour récupérer les frais bancaires en cours - Banque ID: {}", banqueId);
        EncoursFraisBancaireDto result = fraisBancaireService.getOutstandingFraisBancaire(searchDto, banqueId, pageable);
        log.info("Frais bancaires en cours récupérés - Total: {}", result.totalDesEncours());
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Changer le statut PREVU/PAYE", description = "Réservé SYSTEM_ADMIN.")
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<FraisBancaireResponseDto> toggleStatus(@PathVariable Long id) {
        log.info("Requête REST pour basculer le statut du frais bancaire ID: {}", id);
        FraisBancaireResponseDto response = fraisBancaireService.toggleStatus(id);
        log.info("Statut du frais bancaire ID: {} basculé vers {}", id, response.status());
        return ResponseEntity.ok(response);
    }
}
