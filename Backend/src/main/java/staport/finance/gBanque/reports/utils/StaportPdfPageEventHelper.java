package staport.finance.gBanque.reports.utils;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;

/**
 * Helper pour gérer les en-têtes et pieds de page professionnels STAPORT
 * Version optimisée : header réduit de 150pt → 68pt, footer de 80pt → 50pt
 */
public class StaportPdfPageEventHelper extends PdfPageEventHelper {

    private PdfTemplate totalPages;

    // --- INFOS STAPORT ---
    private static final String RAISON_SOCIALE = "Société des Travaux d'Assainissement, Ponts et Routes";
    private static final String LOGO_PATH = "/logos/staportLogo.png";

    // --- INFOS PIED DE PAGE ---
    private static final String FOOTER_LINE1 = "Siège Social : Ensemble Résidentiel « AL BADR » 1er Étage N°4 - Ain Sebaa - Casablanca - capital : 70 000 000,00Dh";
    private static final String FOOTER_LINE2 = "R.C : 96 537 - C.N.S.S : 606 7111 Patente : 30318873 - I.F : 2201604 - ICE : 001515324000076";
    private static final String FOOTER_LINE3 = "Tél(Casablanca) : 0522 34 46 63 - Fax(Casablanca) : 0522 35 17 87";

    // --- COULEURS ET POLICES ---
    private static final Color STAPORT_BLUE = Color.decode("#1a5490");
    private static final Font COMPANY_TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font DETAILED_FOOTER_FONT = FontFactory.getFont(FontFactory.HELVETICA, 6.5f, STAPORT_BLUE);

    // --- DIMENSIONS HEADER (optimisées) ---
    private static final float LOGO_HEIGHT_PX = 30f;      // était 45
    private static final float LINE_SPACING = 8f;          // était 18
    private static final float HEADER_START_OFFSET = 15f;  // était 40 (currentY = pageHeight - offset)
    private static final float SEPARATOR_OFFSET = 10f;     // était 15

    // --- DIMENSIONS FOOTER (optimisées) ---
    private static final float BASE_FOOTER_Y = 10f;        // était 20
    private static final float FOOTER_LINE_SPACING = 8f;   // était 10

    public StaportPdfPageEventHelper() {
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        totalPages = writer.getDirectContent().createTemplate(30, 16);
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        PdfContentByte canvas = writer.getDirectContent();
        Rectangle pageSize = document.getPageSize();

        float leftMargin = 40;
        float rightMargin = pageSize.getWidth() - 40;
        float centerX = pageSize.getWidth() / 2;
        float currentY = pageSize.getHeight() - HEADER_START_OFFSET;

        try {
            // ====================== EN-TÊTE ======================

            // 1. Logo centré
            try (InputStream logoStream = getClass().getResourceAsStream(LOGO_PATH)) {
                if (logoStream != null) {
                    byte[] bytes = logoStream.readAllBytes();
                    Image logo = Image.getInstance(bytes);

                    float logoWidth = (logo.getWidth() / logo.getHeight()) * LOGO_HEIGHT_PX;
                    logo.scaleAbsolute(logoWidth, LOGO_HEIGHT_PX);

                    float logoX = centerX - (logoWidth / 2);
                    logo.setAbsolutePosition(logoX, currentY - LOGO_HEIGHT_PX);
                    canvas.addImage(logo);

                    currentY -= LOGO_HEIGHT_PX;
                } else {
                    ColumnText.showTextAligned(
                            canvas, Element.ALIGN_CENTER,
                            new Phrase("STAPORT", COMPANY_TITLE_FONT),
                            centerX, currentY - 12, 0
                    );
                    currentY -= 15;
                }
            } catch (IOException | BadElementException e) {
                System.err.println("Erreur chargement logo: " + e.getMessage());
                currentY -= 15;
            }

            // 2. Raison sociale centrée
            currentY -= LINE_SPACING;
            ColumnText.showTextAligned(
                    canvas, Element.ALIGN_CENTER,
                    new Phrase(RAISON_SOCIALE, COMPANY_TITLE_FONT),
                    centerX, currentY, 0
            );

            // 3. Ligne de séparation header
            currentY -= SEPARATOR_OFFSET;
            canvas.setLineWidth(1f);
            canvas.setColorStroke(STAPORT_BLUE);
            canvas.moveTo(leftMargin, currentY);
            canvas.lineTo(rightMargin, currentY);
            canvas.stroke();

            // ====================== PIED DE PAGE ======================

            float footerLineSpacing = FOOTER_LINE_SPACING;
            float baseY = BASE_FOOTER_Y;

            // Ligne 3 — Tél/Fax (la plus basse)
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                    new Phrase(FOOTER_LINE3, DETAILED_FOOTER_FONT),
                    centerX, baseY, 0);

            // Ligne 2 — RC, CNSS, etc.
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                    new Phrase(FOOTER_LINE2, DETAILED_FOOTER_FONT),
                    centerX, baseY + footerLineSpacing, 0);

            // Ligne 1 — Siège Social
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                    new Phrase(FOOTER_LINE1, DETAILED_FOOTER_FONT),
                    centerX, baseY + (2 * footerLineSpacing), 0);

            // Ligne de séparation footer
            float footerSeparatorY = baseY + (3 * footerLineSpacing) + 1f;
            canvas.setLineWidth(1f);
            canvas.setColorStroke(STAPORT_BLUE);
            canvas.moveTo(leftMargin, footerSeparatorY);
            canvas.lineTo(rightMargin, footerSeparatorY);
            canvas.stroke();

            // Numérotation — au-dessus de la ligne séparatrice
            float pageNumY = footerSeparatorY + 4f;
            String pageText = "Page " + writer.getPageNumber() + " sur ";
            ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT,
                    new Phrase(pageText, DETAILED_FOOTER_FONT),
                    rightMargin - 15, pageNumY, 0);

            // Template total pages
            canvas.addTemplate(totalPages, rightMargin - 15, pageNumY);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        ColumnText.showTextAligned(
                totalPages,
                Element.ALIGN_LEFT,
                new Phrase(String.valueOf(writer.getPageNumber()), DETAILED_FOOTER_FONT),
                0, 0, 0
        );
    }
}