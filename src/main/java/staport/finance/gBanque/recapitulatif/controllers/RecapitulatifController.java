package staport.finance.gBanque.recapitulatif.controllers;


import com.lowagie.text.DocumentException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.recapitulatif.dtos.request.DecaissementSearchDto;
import staport.finance.gBanque.recapitulatif.dtos.response.AgregatedRecapDto;
import staport.finance.gBanque.recapitulatif.dtos.response.RecaputilatifGlobal;
import staport.finance.gBanque.recapitulatif.services.interfaces.IRecapitulatifService;
import staport.finance.gBanque.reports.service.StaportPdfReportService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * REST Controller for the disbursement summary.
 * Exposes a paginated and sortable endpoint to retrieve a consolidated view
 * of all expected payments for the current month.
 */
@Tag(name = "Récapitulatif", description = "Vue consolidée des décaissements/encaissements prévisionnels du mois par banque")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/recapitulatif")
@PreAuthorize(RecapitulatifController.FINANCE_OR_ADMIN)
public class RecapitulatifController {

    static final String FINANCE_OR_ADMIN =
            "hasAnyAuthority('DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR', 'SYSTEM_ADMIN')";

    private final IRecapitulatifService recapitulatifService;
    private final StaportPdfReportService staportPdfReportService;

    public RecapitulatifController(IRecapitulatifService recapitulatifService, StaportPdfReportService staportPdfReportService) {
        this.recapitulatifService = recapitulatifService;
        this.staportPdfReportService = staportPdfReportService;
    }

    /**
     * Endpoint to retrieve the list of upcoming disbursements for a specific bank.
     * The banqueId is a mandatory path variable.
     * @param banqueId The ID of the bank to filter by.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of disbursement items.
     */
    @Operation(summary = "Décaissements prévisionnels du mois")
    @GetMapping("/decaissement/banque/{banqueId}")
    public ResponseEntity<AgregatedRecapDto> getRecapitulatifDecaissement(
            @PageableDefault(sort = "dateEmission", direction = Sort.Direction.ASC) Pageable pageable,
            @PathVariable Long banqueId, @ModelAttribute DecaissementSearchDto searchDto) {

        AgregatedRecapDto res =
                recapitulatifService.getRecapitulatifDecaissementForCurrentMonthForGivenBanque(pageable, searchDto, banqueId);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    /**
     * Endpoint to retrieve the list of upcoming encaissement for a specific bank.
     * The banqueId is a mandatory path variable.
     * @param banqueId The ID of the bank to filter by.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of disbursement items.
     */
    @Operation(summary = "Encaissements prévisionnels du mois")
    @GetMapping("/encaissement/banque/{banqueId}")
    public ResponseEntity<AgregatedRecapDto> getRecapitulatifEncaissement(
            @PageableDefault(sort = "dateEmission", direction = Sort.Direction.ASC) Pageable pageable,
            @PathVariable Long banqueId, @ModelAttribute DecaissementSearchDto searchDto) {

        AgregatedRecapDto res = recapitulatifService.getRecapitulatifEncaissementForCurrentMonthForGivenBanque(pageable, searchDto, banqueId);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }


    @Operation(summary = "Exporter en PDF le récapitulatif des décaissements prévisionnels du mois",
            description = "Mêmes critères que GET /recapitulatif/decaissement/banque/{banqueId} — sans pagination")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    @GetMapping("/decaissement/banque/{banqueId}/export/pdf")
    public ResponseEntity<byte[]> exportRecapitulatifDecaissementPdf(
            @PathVariable Long banqueId, @ModelAttribute DecaissementSearchDto searchDto) {

        byte[] pdf = recapitulatifService.exportRecapitulatifDecaissementPdf(searchDto, banqueId);
        String filename = "recapitulatif_decaissement_" + banqueId + "_" + LocalDate.now() + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @Operation(summary = "Exporter en PDF le récapitulatif des encaissements prévisionnels du mois",
            description = "Mêmes critères que GET /recapitulatif/encaissement/banque/{banqueId} — sans pagination")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    @GetMapping("/encaissement/banque/{banqueId}/export/pdf")
    public ResponseEntity<byte[]> exportRecapitulatifEncaissementPdf(
            @PathVariable Long banqueId, @ModelAttribute DecaissementSearchDto searchDto) {

        byte[] pdf = recapitulatifService.exportRecapitulatifEncaissementPdf(searchDto, banqueId);
        String filename = "recapitulatif_encaissement_" + banqueId + "_" + LocalDate.now() + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @Operation(summary = "Télécharger le récapitulatif global (PDF)")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    @GetMapping("/{banqueId}/pdf")
    public ResponseEntity<byte[]> getGlobalRecapReport(@PathVariable Long banqueId) throws DocumentException {

        // 1. Récupérer le DTO complet et calculé
        RecaputilatifGlobal recapGlobalDto = recapitulatifService.getRecapitulatifGlobal(banqueId);

        // 2. Générer le fichier PDF
        byte[] pdfBytes = staportPdfReportService.generateRecapGlobalReport(recapGlobalDto);

        // 3. Préparer les en-têtes de la réponse HTTP pour le téléchargement
        String banqueName = recapGlobalDto.banqueName().replaceAll("\\s+", "_");
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String filename = "Recapitulatif_Global_" + banqueName + "_" + date + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        // Indiquer le type de contenu (MIME Type)
        headers.setContentType(MediaType.APPLICATION_PDF);
        // Forcer le téléchargement avec le nom de fichier spécifié
        headers.setContentDispositionFormData("attachment", filename);
        // Définir la taille du fichier
        headers.setContentLength(pdfBytes.length);

        // 4. Retourner le fichier dans la réponse
        return new ResponseEntity<>(
                pdfBytes,
                headers,
                HttpStatus.OK
        );
    }

    @Operation(summary = "Télécharger le récapitulatif global consolidé (PDF, toutes banques)")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    @ApiResponse(responseCode = "204", description = "Aucune banque à récapituler")
    @GetMapping("/global/pdf")
    public ResponseEntity<byte[]> getGlobalRecapReportForAllBanks() throws DocumentException {

        // 1. Récupérer la liste des DTOs complets pour toutes les banques.
        List<RecaputilatifGlobal> recapsGlobaux = recapitulatifService.getAllRecapitulatifsGlobaux();

        if (recapsGlobaux.isEmpty()) {
            // Optionnel : Retourner une 404 ou 204 si aucune donnée n'est trouvée.
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        // 2. Générer le fichier PDF consolidé
        byte[] pdfBytes = staportPdfReportService.generateGlobalRecapForMultipleBanks(recapsGlobaux);

        // 3. Préparer les en-têtes de la réponse HTTP pour le téléchargement
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String filename = "Journal_Tresorerie_Global_" + date + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdfBytes.length);

        // 4. Retourner le fichier dans la réponse
        return new ResponseEntity<>(
                pdfBytes,
                headers,
                HttpStatus.OK
        );
    }
}
