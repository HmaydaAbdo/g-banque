package staport.finance.gBanque.reports;

import com.lowagie.text.Chunk;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import staport.finance.gBanque.reports.service.StaportPdfReportService;

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
}
