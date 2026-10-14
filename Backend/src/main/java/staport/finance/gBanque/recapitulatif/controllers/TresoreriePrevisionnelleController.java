package staport.finance.gBanque.recapitulatif.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import staport.finance.gBanque.recapitulatif.dtos.response.PlanTresorerieDto;
import staport.finance.gBanque.recapitulatif.enums.PeriodeRecap;
import staport.finance.gBanque.recapitulatif.services.interfaces.ITresoreriePrevisionnelleService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Trésorerie prévisionnelle : plan de trésorerie du solde actuel jusqu'à la fin du mois (défaut)
 * ou la fin du trimestre, par banque et consolidé, et son export PDF.
 */
@Tag(name = "Trésorerie prévisionnelle", description = "Plan de trésorerie jusqu'à fin de mois ou fin de trimestre, par banque et consolidé")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/tresorerie-previsionnelle")
@PreAuthorize(TresoreriePrevisionnelleController.FINANCE_OR_ADMIN)
public class TresoreriePrevisionnelleController {

    static final String FINANCE_OR_ADMIN =
            "hasAnyAuthority('DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR', 'SYSTEM_ADMIN')";

    private final ITresoreriePrevisionnelleService tresorerieService;

    public TresoreriePrevisionnelleController(ITresoreriePrevisionnelleService tresorerieService) {
        this.tresorerieService = tresorerieService;
    }

    @Operation(summary = "Plan de trésorerie prévisionnel",
            description = "periode=FIN_DE_MOIS (défaut) : colonnes par semaine jusqu'à la fin du mois ; " +
                    "periode=FIN_DE_TRIMESTRE : colonnes par mois jusqu'à la fin du trimestre civil. " +
                    "La première colonne regroupe les encours non validés dont la date est atteinte.")
    @GetMapping
    public ResponseEntity<PlanTresorerieDto> getPlan(
            @RequestParam(defaultValue = "FIN_DE_MOIS") PeriodeRecap periode) {
        return ResponseEntity.ok(tresorerieService.getPlan(periode));
    }

    @Operation(summary = "Exporter le plan de trésorerie prévisionnel (PDF)",
            description = "Sans banqueId : toutes les banques consolidées. Avec banqueId : la banque demandée.")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    @ApiResponse(responseCode = "404", description = "Banque introuvable")
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(defaultValue = "FIN_DE_MOIS") PeriodeRecap periode,
            @RequestParam(required = false) Long banqueId) {

        byte[] pdf = tresorerieService.exportPdf(periode, banqueId);
        String filename = "Plan_Tresorerie_" + (banqueId == null ? "toutes_banques" : "banque_" + banqueId)
                + "_" + periode.name().toLowerCase() + "_"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdf.length);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
