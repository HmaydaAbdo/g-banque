package staport.finance.gBanque.reports;

import com.lowagie.text.Chunk;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import staport.finance.gBanque.recapitulatif.dtos.response.ColonnePlanDto;
import staport.finance.gBanque.recapitulatif.dtos.response.LignePlanDto;
import staport.finance.gBanque.recapitulatif.dtos.response.PlanBanqueDto;
import staport.finance.gBanque.recapitulatif.dtos.response.PlanTresorerieDto;
import staport.finance.gBanque.recapitulatif.dtos.response.PointBasDto;
import staport.finance.gBanque.recapitulatif.enums.PeriodeRecap;
import staport.finance.gBanque.reports.service.StaportPdfReportService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Garde-fou de la génération PDF (OpenPDF) : un rapport avec tableau et accents se produit et se relit. */
class StaportPdfReportServiceTest {

    @Test
    void genereUnPdfLisibleAvecTableauEtAccents() throws Exception {
        byte[] pdf = new StaportPdfReportService().generateReport("Essai", new Rectangle(842, 595), (doc, writer) -> {
            try {
                doc.add(Chunk.NEWLINE);
                PdfPTable table = new PdfPTable(2);
                table.addCell(new PdfPCell(new Phrase("Bénéficiaire")));
                table.addCell(new PdfPCell(new Phrase("1 234,50 MAD")));
                doc.add(table);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        PdfReader reader = new PdfReader(pdf);
        assertThat(reader.getNumberOfPages()).isGreaterThanOrEqualTo(1);
        assertThat(new PdfTextExtractor(reader).getTextFromPage(1)).contains("Bénéficiaire").contains("1 234,50 MAD");
    }

    @Test
    void genereLePlanDeTresorerieAvecSesColonnesEtSonPointBas() throws Exception {
        List<BigDecimal> deux = List.of(new BigDecimal("1000"), BigDecimal.ZERO);
        PlanBanqueDto banque = new PlanBanqueDto(1L, "Banque Test", new BigDecimal("5000"), new BigDecimal("2000"),
                List.of(new LignePlanDto("Règlements", "REGLEMENT FRNS", deux, new BigDecimal("1000"))),
                List.of(new LignePlanDto("Financements", "Leasing", List.of(BigDecimal.ZERO, new BigDecimal("9000")), new BigDecimal("9000"))),
                deux, List.of(BigDecimal.ZERO, new BigDecimal("9000")),
                List.of(new BigDecimal("6000"), new BigDecimal("-3000")),
                List.of(new BigDecimal("8000"), new BigDecimal("-1000")),
                new BigDecimal("-3000"), new BigDecimal("-1000"),
                new PointBasDto(LocalDate.of(2026, 11, 20), new BigDecimal("-3000"), new BigDecimal("-1000")),
                new BigDecimal("1000"), 11, List.of(), List.of());
        PlanTresorerieDto plan = new PlanTresorerieDto(PeriodeRecap.FIN_DE_TRIMESTRE, LocalDate.of(2026, 10, 14),
                LocalDate.of(2026, 12, 31),
                List.of(new ColonnePlanDto("ECHU", "À ce jour", null, LocalDate.of(2026, 10, 14)),
                        new ColonnePlanDto("P1", "Novembre 2026", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))),
                banque, List.of(banque));

        byte[] pdf = new StaportPdfReportService().generatePlanTresorerieReport(plan, banque);

        String texte = new PdfTextExtractor(new PdfReader(pdf)).getTextFromPage(1);
        assertThat(texte).contains("Plan de trésorerie prévisionnel").contains("Banque Test")
                .contains("jusqu'à fin de trimestre").contains("Novembre 2026").contains("Leasing")
                .contains("Point bas").contains("20/11/2026").contains("Besoin de financement");
    }
}
