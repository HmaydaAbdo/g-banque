package staport.finance.gBanque.reports.service;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.BiConsumer;

/**
 * Génère des rapports Excel (.xlsx) génériques — pendant de {@link StaportPdfReportService}
 * pour ce format. Chaque module reste responsable de la mise en forme de son propre contenu
 * (styles, en-têtes, totaux) ; ce service ne fournit que l'enveloppe classeur/feuille et la
 * sérialisation, exactement comme {@link StaportPdfReportService#generateReport} pour le PDF.
 */
@Service
public class StaportExcelReportService {

    public byte[] generateReport(String sheetName, BiConsumer<Workbook, Sheet> contentGenerator) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            contentGenerator.accept(workbook, sheet);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Erreur lors de la génération du fichier Excel", e);
        }
    }
}
