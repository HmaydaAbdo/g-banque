package staport.finance.gBanque.reports.utils;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;

/**
 * Helper pour gérer les en-têtes et pieds de page professionnels STAPORT
 */
public class StaportPdfPageEventHelper extends PdfPageEventHelper {

    private PdfTemplate totalPages;

    // --- INFOS STAPORT ---
    private static final String RAISON_SOCIALE = "Société des Travaux d'Assainissement, Ponts et Routes";
    private static final String LOGO_PATH = "/logos/staportLogo.png";

    // --- NOUVELLES INFOS PIED DE PAGE DÉTAILLÉES ---
    private static final String FOOTER_LINE1 = "Siège Social : Ensemble Résidentiel « AL BADR » 1er Étage N°4 - Ain Sebaa - Casablanca - capital : 70 000 000,00Dh";
    private static final String FOOTER_LINE2 = "R.C : 96 537 - C.N.S.S : 606 7111 Patente : 30318873 - I.F : 2201604 - ICE : 001515324000076";
    private static final String FOOTER_LINE3 = "Tél(Casablanca) : 0522 34 46 63 - Fax(Casablanca) : 0522 35 17 87";

    // Polices réutilisables
    private static final Color STAPORT_BLUE = Color.decode("#1a5490");
    private static final Font COMPANY_TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
    private static final Font DETAILED_FOOTER_FONT = FontFactory.getFont(FontFactory.HELVETICA, 7, STAPORT_BLUE); // Taille réduite et couleur bleue pour le détail

    // Taille cible du logo
    private static final float LOGO_HEIGHT_PX = 45f;
    private static final float LINE_SPACING = 18;


    public StaportPdfPageEventHelper( ) {

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
        float currentY = pageSize.getHeight() - 40; // Position de départ

        try {
            // ====================== EN-TÊTE CENTRÉ ======================

            // 1. Logo Centré
            try (InputStream logoStream = getClass().getResourceAsStream(LOGO_PATH)) {
                if (logoStream != null) {
                    byte[] bytes = logoStream.readAllBytes();
                    Image logo = Image.getInstance(bytes);

                    // Redimensionnement
                    float logoWidth = (logo.getWidth() / logo.getHeight()) * LOGO_HEIGHT_PX;
                    logo.scaleAbsolute(logoWidth, LOGO_HEIGHT_PX);

                    // Positionnement centré
                    float logoX = centerX - (logoWidth / 2);
                    logo.setAbsolutePosition(logoX, currentY - LOGO_HEIGHT_PX);
                    canvas.addImage(logo);

                    currentY -= LOGO_HEIGHT_PX;
                } else {
                    // Secours si le logo n'est pas trouvé
                    ColumnText.showTextAligned(
                            canvas, Element.ALIGN_CENTER,
                            new Phrase("STAPORT (Logo introuvable)", COMPANY_TITLE_FONT),
                            centerX, currentY - 15, 0
                    );
                    currentY -= 20;
                }
            } catch (IOException | BadElementException e) {
                // Gestion des erreurs de chargement/format
                System.err.println("Erreur critique de chargement/format du logo: " + e.getMessage());
                currentY -= 20;
            }

            // 2. Nom de l'entreprise centré
            currentY -= LINE_SPACING;
            ColumnText.showTextAligned(
                    canvas, Element.ALIGN_CENTER,
                    new Phrase(RAISON_SOCIALE, COMPANY_TITLE_FONT),
                    centerX, currentY, 0
            );

            // 3. Ligne de séparation de l'en-tête
            currentY -= 15;
            canvas.setLineWidth(1f);
            canvas.setColorStroke(STAPORT_BLUE);
            canvas.moveTo(leftMargin, currentY);
            canvas.lineTo(rightMargin, currentY);
            canvas.stroke();


            // ====================== PIED DE PAGE DÉTAILLÉ ======================
            // La numérotation sera placée au-dessus des informations pour éviter le chevauchement
            float baseFooterY = 20; // Ligne la plus basse du texte détaillé
            float detailedLineSpacing = 10;
            float pageNumY = baseFooterY + (3 * detailedLineSpacing) + 5; // Au-dessus de toutes les lignes

            // 1. Lignes d'information (Centrées et en bleu)
            // Ligne 3 (Tél/Fax)
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                    new Phrase(FOOTER_LINE3, DETAILED_FOOTER_FONT), centerX, baseFooterY, 0);

            // Ligne 2 (R.C, C.N.S.S, etc.)
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                    new Phrase(FOOTER_LINE2, DETAILED_FOOTER_FONT), centerX, baseFooterY + detailedLineSpacing, 0);

            // Ligne 1 (Siège Social, Capital)
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                    new Phrase(FOOTER_LINE1, DETAILED_FOOTER_FONT), centerX, baseFooterY + (2 * detailedLineSpacing), 0);


            // 2. Ligne de séparation du pied de page
            float footerSeparatorY = baseFooterY + (3 * detailedLineSpacing) + 1; // Juste au-dessus de la ligne 1
            canvas.setLineWidth(1f);
            canvas.setColorStroke(STAPORT_BLUE);
            canvas.moveTo(leftMargin, footerSeparatorY);
            canvas.lineTo(rightMargin, footerSeparatorY);
            canvas.stroke();


            // 3. Numérotation de page (Placée au-dessus de la ligne de séparation du pied de page)
            String pageText = "Page " + writer.getPageNumber() + " sur ";

            ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT,
                    new Phrase(pageText, DETAILED_FOOTER_FONT), rightMargin - 15, pageNumY, 0);

            // Template pour le nombre total
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
                new Phrase(String.valueOf(writer.getPageNumber()), DETAILED_FOOTER_FONT), // Utiliser la police du pied de page
                0, 0, 0
        );
    }
}