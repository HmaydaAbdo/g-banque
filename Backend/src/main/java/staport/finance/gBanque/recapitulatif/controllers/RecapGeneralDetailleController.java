package staport.finance.gBanque.recapitulatif.controllers;

import com.lowagie.text.DocumentException;
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
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.recapitulatif.dtos.response.RecapGeneralDetailleAllBanquesDto;
import staport.finance.gBanque.recapitulatif.dtos.response.RecapGeneralDetailleDto;
import staport.finance.gBanque.recapitulatif.services.interfaces.IRecapGeneralDetailleService;
import staport.finance.gBanque.reports.service.StaportPdfReportService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Tag(name = "Récapitulatif général détaillé", description = "Résumé du jour + décaissements/encaissements du reste du mois, par catégorie")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/recap-general-detaille")
@PreAuthorize(RecapGeneralDetailleController.FINANCE_OR_ADMIN)
public class RecapGeneralDetailleController {

    static final String FINANCE_OR_ADMIN =
            "hasAnyAuthority('DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR', 'SYSTEM_ADMIN')";

    private final IRecapGeneralDetailleService recapGeneralDetailleService;
    private final StaportPdfReportService pdfReportService;

    public RecapGeneralDetailleController(IRecapGeneralDetailleService recapGeneralDetailleService, StaportPdfReportService pdfReportService) {
        this.recapGeneralDetailleService = recapGeneralDetailleService;
        this.pdfReportService = pdfReportService;
    }

    @Operation(summary = "Récapitulatif général détaillé d'une banque")
    @GetMapping("/banque/{banqueId}")
    public ResponseEntity<RecapGeneralDetailleDto> getRecapForBanque(@PathVariable Long banqueId) {
        return new ResponseEntity<>(
                recapGeneralDetailleService.getRecapForBanque(banqueId),
                HttpStatus.OK
        );
    }

    @Operation(summary = "Récapitulatif général détaillé, toutes banques")
    @GetMapping("/all")
    public ResponseEntity<RecapGeneralDetailleAllBanquesDto> getRecapAllBanques() {
        return new ResponseEntity<>(
                recapGeneralDetailleService.getRecapAllBanques(),
                HttpStatus.OK
        );
    }

    @Operation(summary = "Télécharger le récapitulatif détaillé d'une banque (PDF)")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    @GetMapping("/banque/{banqueId}/pdf")
    public ResponseEntity<byte[]> getRecapDetailleReportForBanque(@PathVariable Long banqueId) throws DocumentException {
        RecapGeneralDetailleDto dto = recapGeneralDetailleService.getRecapForBanque(banqueId);
        byte[] pdfBytes = pdfReportService.generateRecapGeneralDetailleReport(dto);

        String filename = "Recap_Detaille_" + dto.banqueNom().replaceAll("\\s+", "_")
                + "_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @Operation(summary = "Télécharger le récapitulatif détaillé consolidé (PDF, toutes banques)")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    @GetMapping("/all/pdf")
    public ResponseEntity<byte[]> getRecapDetailleReportAllBanques() throws DocumentException {
        RecapGeneralDetailleAllBanquesDto dto = recapGeneralDetailleService.getRecapAllBanques();
        byte[] pdfBytes = pdfReportService.generateRecapGeneralDetailleAllBanquesReport(dto);

        String filename = "Recap_Detaille_AllBanques_"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
