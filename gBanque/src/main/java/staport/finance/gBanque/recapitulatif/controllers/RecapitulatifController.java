package staport.finance.gBanque.recapitulatif.controllers;


import com.lowagie.text.DocumentException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.recapitulatif.dtos.AgregatedRecapDto;
import staport.finance.gBanque.recapitulatif.dtos.DecaissementSearchDto;
import staport.finance.gBanque.recapitulatif.dtos.RecaputilatifGlobal;
import staport.finance.gBanque.recapitulatif.services.RecapitulatifService;
import staport.finance.gBanque.reports.service.StaportPdfReportService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * REST Controller for the disbursement summary.
 * Exposes a paginated and sortable endpoint to retrieve a consolidated view
 * of all expected payments for the current month.
 */
@RestController
@RequestMapping("/recapitulatif")
public class RecapitulatifController {

    private final RecapitulatifService recapitulatifService;
    private final StaportPdfReportService staportPdfReportService;

    public RecapitulatifController(RecapitulatifService recapitulatifService, StaportPdfReportService staportPdfReportService) {
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
    @GetMapping("/decaissement/banque/{banqueId}")
    public ResponseEntity<AgregatedRecapDto> getRecapitulatifDecaissement(
            @PageableDefault(sort = "dateEmission", direction = Sort.Direction.ASC) Pageable pageable,
            @PathVariable Long banqueId,@ModelAttribute DecaissementSearchDto searchDto) {

        searchDto.setBanqueId(banqueId);
        AgregatedRecapDto res =
                recapitulatifService.getRecapitulatifDecaissementForCurrentMonthForGivenBanque(pageable,searchDto);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    /**
     * Endpoint to retrieve the list of upcoming encaissement for a specific bank.
     * The banqueId is a mandatory path variable.
     * @param banqueId The ID of the bank to filter by.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of disbursement items.
     */
    @GetMapping("/encaissement/banque/{banqueId}")
    public ResponseEntity<AgregatedRecapDto> getRecapitulatifEncaissement(
            @PageableDefault(sort = "dateEmission", direction = Sort.Direction.ASC) Pageable pageable,
            @PathVariable Long banqueId,@ModelAttribute DecaissementSearchDto searchDto) {

        searchDto.setBanqueId(banqueId);
        AgregatedRecapDto res = recapitulatifService.getRecapitulatifEncaissementForCurrentMonthForGivenBanque(pageable,searchDto);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }


    @GetMapping("/{banqueId}/pdf")
    public ResponseEntity<byte[]> getGlobalRecapReport(@PathVariable Long banqueId) throws DocumentException {

        // 1. Récupérer le DTO complet et calculé
        RecaputilatifGlobal recapGlobalDto = recapitulatifService.getRecapitulatifGlobal(banqueId);

        // 2. Générer le fichier PDF
        byte[] pdfBytes = staportPdfReportService.generateRecapGlobalReport(recapGlobalDto);

        // 3. Préparer les en-têtes de la réponse HTTP pour le téléchargement
        String banqueName = recapGlobalDto.getBanqueName().replaceAll("\\s+", "_");
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

