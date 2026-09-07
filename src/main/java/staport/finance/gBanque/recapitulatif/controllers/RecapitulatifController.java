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
import staport.finance.gBanque.recapitulatif.dtos.request.RecapitulatifSearchDto;
import staport.finance.gBanque.recapitulatif.dtos.response.AgregatedRecapDto;
import staport.finance.gBanque.recapitulatif.dtos.response.RecaputilatifGlobal;
import staport.finance.gBanque.recapitulatif.enums.PeriodeRecap;
import staport.finance.gBanque.recapitulatif.services.interfaces.IRecapitulatifService;
import staport.finance.gBanque.reports.service.StaportPdfReportService;
import staport.finance.gBanque.transactions.enums.SensOperation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * REST Controller du récapitulatif des encours prévisionnels.
 * Expose un endpoint paginé et triable donnant une vue consolidée des encaissements et
 * décaissements attendus du mois en cours pour une banque, ainsi que son export PDF.
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
     * Endpoint unique du récapitulatif des encours du mois pour une banque : encaissements et
     * décaissements réunis, le sens n'étant plus qu'un critère de recherche optionnel.
     *
     * @param banqueId identifiant de la banque (obligatoire, porté par l'URL).
     * @param searchDto critères de recherche, dont le sens (facultatif).
     * @param pageable pagination et tri.
     */
    @Operation(summary = "Encaissements et décaissements prévisionnels du mois",
            description = "Le paramètre de requête `sens` est optionnel : absent, les deux sens " +
                    "sont retournés entrelacés ; ENCAISSEMENT ou DECAISSEMENT ne retourne que ce " +
                    "sens. Les totaux débit et crédit retournés suivent toujours les critères " +
                    "appliqués — le total du sens exclu vaut donc zéro.")
    @GetMapping("/banque/{banqueId}")
    public ResponseEntity<AgregatedRecapDto> getRecapitulatif(
            @PageableDefault(sort = "dateReference", direction = Sort.Direction.DESC) Pageable pageable,
            @PathVariable Long banqueId, @ModelAttribute RecapitulatifSearchDto searchDto) {

        AgregatedRecapDto res =
                recapitulatifService.getRecapitulatifForCurrentMonthForGivenBanque(pageable, searchDto, banqueId);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Operation(summary = "Exporter en PDF le récapitulatif des encours prévisionnels du mois",
            description = "Mêmes critères que GET /recapitulatif/banque/{banqueId}, filtre de sens compris — sans pagination")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    @GetMapping("/banque/{banqueId}/export/pdf")
    public ResponseEntity<byte[]> exportRecapitulatifPdf(
            @PathVariable Long banqueId, @ModelAttribute RecapitulatifSearchDto searchDto) {

        byte[] pdf = recapitulatifService.exportRecapitulatifPdf(searchDto, banqueId);
        String filename = "recapitulatif_" + segmentSens(searchDto) + "_" + banqueId + "_" + LocalDate.now() + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    /**
     * Segment du nom de fichier reflétant le filtre de sens appliqué à l'export ("tous" lorsque
     * aucun sens n'est filtré), pour que deux exports successifs de sens différents ne se
     * recouvrent pas dans le dossier de téléchargement de l'utilisateur.
     */
    private static String segmentSens(RecapitulatifSearchDto searchDto) {
        SensOperation sens = (searchDto != null) ? searchDto.sens() : null;
        return sens == null ? "tous" : sens.name().toLowerCase(Locale.ROOT);
    }

    @Operation(summary = "Télécharger le récapitulatif global (PDF)",
            description = "periode=A_CE_JOUR limite aux encours déjà échus/émis ; periode=FIN_DE_MOIS " +
                    "(défaut) inclut les encours jusqu'à la fin du mois en cours.")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    @GetMapping("/{banqueId}/pdf")
    public ResponseEntity<byte[]> getGlobalRecapReport(
            @PathVariable Long banqueId,
            @RequestParam(defaultValue = "FIN_DE_MOIS") PeriodeRecap periode) throws DocumentException {

        // 1. Récupérer le DTO complet et calculé
        RecaputilatifGlobal recapGlobalDto = recapitulatifService.getRecapitulatifGlobal(banqueId, periode);

        // 2. Générer le fichier PDF
        byte[] pdfBytes = staportPdfReportService.generateRecapGlobalReport(recapGlobalDto, periode);

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

    @Operation(summary = "Télécharger le récapitulatif global consolidé (PDF, toutes banques)",
            description = "periode=A_CE_JOUR limite aux encours déjà échus/émis ; periode=FIN_DE_MOIS " +
                    "(défaut) inclut les encours jusqu'à la fin du mois en cours.")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    @ApiResponse(responseCode = "204", description = "Aucune banque à récapituler")
    @GetMapping("/global/pdf")
    public ResponseEntity<byte[]> getGlobalRecapReportForAllBanks(
            @RequestParam(defaultValue = "FIN_DE_MOIS") PeriodeRecap periode) throws DocumentException {

        // 1. Récupérer la liste des DTOs complets pour toutes les banques.
        List<RecaputilatifGlobal> recapsGlobaux = recapitulatifService.getAllRecapitulatifsGlobaux(periode);

        if (recapsGlobaux.isEmpty()) {
            // Optionnel : Retourner une 404 ou 204 si aucune donnée n'est trouvée.
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        // 2. Générer le fichier PDF consolidé
        byte[] pdfBytes = staportPdfReportService.generateGlobalRecapForMultipleBanks(recapsGlobaux, periode);

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
