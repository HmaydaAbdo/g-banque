package staport.finance.gBanque.recapitulatif.controllers;

import com.lowagie.text.DocumentException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.recapitulatif.dtos.RecapGeneralDetailleAllBanquesDto;
import staport.finance.gBanque.recapitulatif.dtos.RecapGeneralDetailleDto;
import staport.finance.gBanque.recapitulatif.services.RecapGeneralDetailleService;
import staport.finance.gBanque.reports.service.StaportPdfReportService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/recap-general-detaille")
public class RecapGeneralDetailleController {

    private final RecapGeneralDetailleService recapGeneralDetailleService;
    private final StaportPdfReportService pdfReportService;

    public RecapGeneralDetailleController(RecapGeneralDetailleService recapGeneralDetailleService, StaportPdfReportService pdfReportService) {
        this.recapGeneralDetailleService = recapGeneralDetailleService;
        this.pdfReportService = pdfReportService;
    }

    @GetMapping("/banque/{banqueId}")
    public ResponseEntity<RecapGeneralDetailleDto> getRecapForBanque(@PathVariable Long banqueId) {
        return new ResponseEntity<>(
                recapGeneralDetailleService.getRecapForBanque(banqueId),
                HttpStatus.OK
        );
    }

    @GetMapping("/all")
    public ResponseEntity<RecapGeneralDetailleAllBanquesDto> getRecapAllBanques() {
        return new ResponseEntity<>(
                recapGeneralDetailleService.getRecapAllBanques(),
                HttpStatus.OK
        );
    }

    @GetMapping("/banque/{banqueId}/pdf")
    public ResponseEntity<byte[]> getRecapDetailleReportForBanque(@PathVariable Long banqueId) throws DocumentException {
        RecapGeneralDetailleDto dto = recapGeneralDetailleService.getRecapForBanque(banqueId);
        byte[] pdfBytes = pdfReportService.generateRecapGeneralDetailleReport(dto);

        String filename = "Recap_Detaille_" + dto.getBanqueNom().replaceAll("\\s+", "_")
                + "_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

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