package staport.finance.gBanque.reports.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.stereotype.Service;
import staport.finance.gBanque.rapprochement.dtos.ReportDto;
import staport.finance.gBanque.recapitulatif.dtos.RecaputilatifGlobal;
import staport.finance.gBanque.recapitulatif.dtos.RecaputilatifItem;
import staport.finance.gBanque.reports.utils.StaportPdfPageEventHelper;
import staport.finance.gBanque.transactions.dtos.TransactionResponseDto;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class StaportPdfReportService {

    // Constantes de style pour les rapports
    private static final Color STAPORT_BLUE = Color.decode("#1a5490");
    private static final Font TITLE_FONT_BLACK = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, STAPORT_BLUE);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font DATA_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font TRANSACTION_HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
    private static final Font TRANSACTION_DATA_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Génère un rapport PDF générique avec en-tête et pied de page STAPORT automatiques.
     */
    public byte[] generateReport(String reportTitle, BiConsumer<Document, PdfWriter> contentGenerator)
            throws DocumentException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Marge supérieure à 120, inférieure à 80
        Document document = new Document(PageSize.A4, 20, 20, 120, 80);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);

            writer.setPageEvent(new StaportPdfPageEventHelper());

            document.open();
            contentGenerator.accept(document, writer);
            document.close();
            return baos.toByteArray();

        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }


    /**
     * Génère un rapport PDF spécifique au rapprochement bancaire à partir du DTO.
     * La signature est mise à jour pour utiliser ReportDto.
     */
    public byte[] generateRapprochementReport(ReportDto dto) throws DocumentException {

        String reportTitle = "Rapprochement Bancaire";

        return generateReport(
                reportTitle,
                (document, writer) -> {
                    try {
                        // ----------------------------------------------------
                        // SECTION 1 : SYNTHÈSE ET EN-TÊTES
                        // ----------------------------------------------------
                        Paragraph title = new Paragraph(reportTitle, TITLE_FONT_BLACK);
                        title.setAlignment(Element.ALIGN_CENTER);
                        title.setSpacingAfter(10);
                        document.add(Chunk.NEWLINE);
                        document.add(Chunk.NEWLINE);
                        document.add(title);

                        // --- Tableau d'en-tête (Banque, Date, Solde Initial) ---
                        PdfPTable headerTable = new PdfPTable(3);
                        headerTable.setWidthPercentage(100);
                        headerTable.setWidths(new float[]{3f, 2f, 3f});
                        headerTable.setSpacingAfter(10);

                        // Cellule 1: Banque
                        PdfPCell bankCell = new PdfPCell(new Phrase("Banque: " + dto.getBanqueNom(), BOLD_FONT));
                        bankCell.setBorder(Rectangle.NO_BORDER);
                        bankCell.setPadding(3);
                        headerTable.addCell(bankCell);

                        // Cellule 2: Date
                        PdfPCell dateCell = new PdfPCell(new Phrase("Date: " + dto.getDateRapprochement().format(DATE_FORMATTER), BOLD_FONT));
                        dateCell.setBorder(Rectangle.NO_BORDER);
                        dateCell.setPadding(3);
                        headerTable.addCell(dateCell);

                        // Cellule 3: Solde Initial (Ajouté ici)
                        PdfPCell soldeDebutCell = new PdfPCell(new Phrase("Solde Initial: " + formatCurrency(dto.getSoldeDebut()), BOLD_FONT));
                        soldeDebutCell.setBorder(Rectangle.NO_BORDER);
                        soldeDebutCell.setPadding(3);
                        soldeDebutCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        headerTable.addCell(soldeDebutCell);

                        document.add(headerTable);


                        // ----------------------------------------------------
                        // SECTION 2 : TABLEAU DES OPÉRATIONS (Transactions)
                        // ----------------------------------------------------


                        if (dto.getTransactions() != null && !dto.getTransactions().isEmpty()) {
                            // Appel de la méthode avec la LISTE de transactions (getContent() n'est plus nécessaire)
                            generateTransactionsTable(document, dto.getTransactions());
                        } else {
                            document.add(new Paragraph("Aucune transaction trouvée pour cette période.", SUBTITLE_FONT));
                        }

                        // ----------------------------------------------------
                        //  SECTION : TOTAUX DÉBIT/CRÉDIT/TVA (Style final)
                        // ----------------------------------------------------


                        // Tableau pour les totaux (4 colonnes: Label, Value, Label, Value)
                        PdfPTable totalsTable = new PdfPTable(4);
                        totalsTable.setWidthPercentage(100);
                        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        totalsTable.setWidths(new float[]{3f, 2f, 3f, 2f});

                        // Ligne 1 : Total Débit / Total Crédit

                        // Total Débit Label
                        PdfPCell totalDebitLabel = new PdfPCell(new Phrase("TOTAL DÉBIT :", BOLD_FONT));
                        totalDebitLabel.setBorder(Rectangle.NO_BORDER);
                        totalDebitLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        totalDebitLabel.setBackgroundColor(Color.decode("#f0f0f0"));
                        totalsTable.addCell(totalDebitLabel);

                        // Total Débit Value
                        PdfPCell totalDebitValue = new PdfPCell(new Phrase(formatCurrency(dto.getTotalDebit()), BOLD_FONT));
                        totalDebitValue.setBorder(Rectangle.NO_BORDER);
                        totalDebitValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        totalDebitValue.setBackgroundColor(Color.decode("#f0f0f0"));
                        totalsTable.addCell(totalDebitValue);

                        // Total Crédit Label
                        PdfPCell totalCreditLabel = new PdfPCell(new Phrase("TOTAL CRÉDIT :", BOLD_FONT));
                        totalCreditLabel.setBorder(Rectangle.NO_BORDER);
                        totalCreditLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        totalCreditLabel.setBackgroundColor(Color.decode("#f0f0f0"));
                        totalsTable.addCell(totalCreditLabel);

                        // Total Crédit Value
                        PdfPCell totalCreditValue = new PdfPCell(new Phrase(formatCurrency(dto.getTotalCredit()), BOLD_FONT));
                        totalCreditValue.setBorder(Rectangle.NO_BORDER);
                        totalCreditValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        totalCreditValue.setBackgroundColor(Color.decode("#f0f0f0"));
                        totalsTable.addCell(totalCreditValue);

                        // Ligne 2 : TVA Déductible / TVA Collectée

                        // TVA Déductible Label
                        PdfPCell tvaDedLabel = new PdfPCell(new Phrase("TOTAL TVA DÉDUCTIBLE :", BOLD_FONT));
                        tvaDedLabel.setBorder(Rectangle.NO_BORDER);
                        tvaDedLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        tvaDedLabel.setBackgroundColor(Color.decode("#f0f0f0"));
                        totalsTable.addCell(tvaDedLabel);

                        // TVA Déductible Value
                        PdfPCell tvaDedValue = new PdfPCell(new Phrase(formatCurrency(dto.getTvaDeductible()), BOLD_FONT));
                        tvaDedValue.setBorder(Rectangle.NO_BORDER);
                        tvaDedValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        tvaDedValue.setBackgroundColor(Color.decode("#f0f0f0"));
                        totalsTable.addCell(tvaDedValue);

                        // TVA Collectée Label
                        PdfPCell tvaCollLabel = new PdfPCell(new Phrase("TOTAL TVA COLLECTÉE :", BOLD_FONT));
                        tvaCollLabel.setBorder(Rectangle.NO_BORDER);
                        tvaCollLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        tvaCollLabel.setBackgroundColor(Color.decode("#f0f0f0"));
                        totalsTable.addCell(tvaCollLabel);

                        // TVA Collectée Value
                        PdfPCell tvaCollValue = new PdfPCell(new Phrase(formatCurrency(dto.getTvaCollectee()), BOLD_FONT));
                        tvaCollValue.setBorder(Rectangle.NO_BORDER);
                        tvaCollValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        tvaCollValue.setBackgroundColor(Color.decode("#f0f0f0"));
                        totalsTable.addCell(tvaCollValue);

                        document.add(totalsTable);

                        // ----------------------------------------------------
                        // SECTION 3 : MONTANTS FINAUX (Solde Fin et TVA Due)
                        // ----------------------------------------------------

                        PdfPTable finalTable = new PdfPTable(4);
                        finalTable.setWidthPercentage(100);
                        finalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        finalTable.setWidths(new float[]{3f, 2f, 3f, 2f});

                        // Ligne 1 : Solde Fin
                        PdfPCell soldeFinLabel = new PdfPCell(new Phrase("SOLDE FINAL :", BOLD_FONT));
                        soldeFinLabel.setBorder(Rectangle.NO_BORDER);
                        soldeFinLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        soldeFinLabel.setBackgroundColor(Color.decode("#f0f0f0"));
                        finalTable.addCell(soldeFinLabel);

                        PdfPCell soldeFinValue = new PdfPCell(new Phrase(formatCurrency(dto.getSoldeFin()), BOLD_FONT));
                        soldeFinValue.setBorder(Rectangle.NO_BORDER);
                        soldeFinValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        soldeFinValue.setBackgroundColor(Color.decode("#f0f0f0"));
                        finalTable.addCell(soldeFinValue);

                        // Ligne 1 suite: TVA Due
                        PdfPCell tvaDueLabel = new PdfPCell(new Phrase("TVA DUE :", BOLD_FONT));
                        tvaDueLabel.setBorder(Rectangle.NO_BORDER);
                        tvaDueLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        tvaDueLabel.setBackgroundColor(Color.decode("#f0f0f0"));
                        finalTable.addCell(tvaDueLabel);

                        PdfPCell tvaDueValue = new PdfPCell(new Phrase(formatCurrency(dto.getTvaDue()), BOLD_FONT));
                        tvaDueValue.setBorder(Rectangle.NO_BORDER);
                        tvaDueValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        tvaDueValue.setBackgroundColor(Color.decode("#f0f0f0"));
                        finalTable.addCell(tvaDueValue);

                        document.add(finalTable);

                    } catch (DocumentException e) {
                        throw new RuntimeException("Erreur lors de la génération du contenu du rapport de rapprochement.", e);
                    }
                }
        );
    }

    /**
     * Génère le tableau des transactions (sans ligne de totaux interne).
     */
    private void generateTransactionsTable(Document document, List<TransactionResponseDto> transactions) throws DocumentException {
        // Nouvelle configuration : 9 colonnes
        PdfPTable table = new PdfPTable(9);
        table.setWidthPercentage(100);

        // Largeurs ajustées pour éviter l'enveloppement excessif des dates/montants
        table.setWidths(new float[]{2.5f, 2.5f, 2.5f, 3f, 3f, 3f, 3f, 3f, 2.5f});
        table.setSpacingBefore(3);
        table.setSpacingAfter(3);

        // --- 1. Répéter l'en-tête sur chaque page ---
        table.setHeaderRows(1);

        // Headers pour 9 colonnes (Autorisé à wrapper par défaut)
        String[] headers = {
                "Date d'émission", "Date d'opération", "Date de valeur", "Libellé", "Débit", "Crédit",
                "TVA Déductible", "TVA Collectée", "Observation"
        };
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, TRANSACTION_HEADER_FONT));
            cell.setBackgroundColor(STAPORT_BLUE);
            cell.setNoWrap(false);
            cell.setPadding(2);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell);
        }

        // --- 2. Données ---
        for (TransactionResponseDto tx : transactions) {

            // 1. Date Émission (NoWrap)
            PdfPCell dateEmCell = createCell(tx.getDateEmission() != null ? tx.getDateEmission().format(DATETIME_FORMATTER) : "-", TRANSACTION_DATA_FONT, Element.ALIGN_CENTER);
            dateEmCell.setNoWrap(true);
            table.addCell(dateEmCell);

            // 2. Date Opérations (NoWrap)
            PdfPCell dateOpCell = createCell(tx.getDateOperations() != null ? tx.getDateOperations().format(DATETIME_FORMATTER) : "-", TRANSACTION_DATA_FONT, Element.ALIGN_CENTER);
            dateOpCell.setNoWrap(true);
            table.addCell(dateOpCell);

            // 3. Date Valeur (NoWrap)
            PdfPCell dateValCell = createCell(tx.getDateValeur() != null ? tx.getDateValeur().format(DATETIME_FORMATTER) : "-", TRANSACTION_DATA_FONT, Element.ALIGN_CENTER);
            dateValCell.setNoWrap(true);
            table.addCell(dateValCell);

            // 4. Libellé (Wrapped)
            table.addCell(createCell(tx.getLibelle(), TRANSACTION_DATA_FONT, Element.ALIGN_LEFT));

            // 5. Débit (NoWrap)
            PdfPCell debitCell = createCell(formatCurrency(tx.getDebit()), TRANSACTION_DATA_FONT, Element.ALIGN_RIGHT);
            debitCell.setNoWrap(true);
            table.addCell(debitCell);

            // 6. Crédit (NoWrap)
            PdfPCell creditCell = createCell(formatCurrency(tx.getCredit()), TRANSACTION_DATA_FONT, Element.ALIGN_RIGHT);
            creditCell.setNoWrap(true);
            table.addCell(creditCell);

            // 7. TVA Déductible (NoWrap)
            PdfPCell tvaDedCell = createCell(formatCurrency(tx.getTvaDeductible()), TRANSACTION_DATA_FONT, Element.ALIGN_RIGHT);
            tvaDedCell.setNoWrap(true);
            table.addCell(tvaDedCell);

            // 8. TVA Collectée (NoWrap)
            PdfPCell tvaCollCell = createCell(formatCurrency(tx.getTvaCollectee()), TRANSACTION_DATA_FONT, Element.ALIGN_RIGHT);
            tvaCollCell.setNoWrap(true);
            table.addCell(tvaCollCell);

            // 9. Observation (Wrapped)
            table.addCell(createCell(tx.getObservation(), TRANSACTION_DATA_FONT, Element.ALIGN_LEFT));
        }

        document.add(table);
    }

    // Utilitaires de formatage
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "-";
        }

        // Utiliser un locale français pour le format monétaire
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');   // Séparateur de milliers
        symbols.setDecimalSeparator(',');    // Séparateur décimal

        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);

        return formatter.format(amount);
    }

    private PdfPCell createCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(4);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private Map<TransactionSource, List<RecaputilatifItem>> groupItemsBySource(List<RecaputilatifItem> items) {
        if (items == null) return Map.of();
        return items.stream()
                .collect(Collectors.groupingBy(RecaputilatifItem::getSource));
    }

    /**
     * Utilitaire pour créer une cellule d'en-tête avec configuration de fusion (colspan/rowspan).
     */
    private PdfPCell createHeaderCell(String text, Font font, Color bgColor, int alignment, int colspan, int rowspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(3);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setColspan(colspan);
        cell.setRowspan(rowspan);
        return cell;
    }

    /**
     * Génère le tableau de synthèse consolidé pour toutes les banques (Nouveau format demandé).
     */

    private void addAggregatedSummaryTable(Document document, List<RecaputilatifGlobal> dtos) throws DocumentException {
        // 7 colonnes pour correspondre au nombre de colonnes réelles
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        // 7 valeurs de largeur pour 7 colonnes
        table.setWidths(new float[]{2f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f});
        table.setSpacingAfter(10);
        table.setHeaderRows(2); // Headers sur 2 lignes

        // --- LIGNE 1 des Headers ---
        // Colonne 1: Banque (Rowspan 2)
        table.addCell(createHeaderCell("Banque", TRANSACTION_HEADER_FONT, STAPORT_BLUE, Element.ALIGN_CENTER, 1, 2));

        // Colonne 2: Solde Actuel (Rowspan 2)
        table.addCell(createHeaderCell("Solde Actuel", TRANSACTION_HEADER_FONT, STAPORT_BLUE, Element.ALIGN_CENTER, 1, 2));

        // Colonnes 3-4: Encours (Colspan 2, Rowspan 1)
        table.addCell(createHeaderCell("Encours", TRANSACTION_HEADER_FONT, STAPORT_BLUE, Element.ALIGN_CENTER, 2, 1));

        // Colonne 5: Solde fin du mois (Rowspan 2)
        table.addCell(createHeaderCell("Solde\nfin du mois", TRANSACTION_HEADER_FONT, STAPORT_BLUE, Element.ALIGN_CENTER, 1, 2));

        // Colonne 6: Plafond FC (Rowspan 2)
        table.addCell(createHeaderCell("Plafond FC", TRANSACTION_HEADER_FONT, STAPORT_BLUE, Element.ALIGN_CENTER, 1, 2));

        // Colonne 7: Reste fin du mois (Rowspan 2)
        table.addCell(createHeaderCell("Reste\nfin du mois", TRANSACTION_HEADER_FONT, STAPORT_BLUE, Element.ALIGN_CENTER, 1, 2));

        // --- LIGNE 2 des Headers (sous Encours) ---
        // Colonne 3: Total Encaissements
        table.addCell(createHeaderCell("Total\nEncaissements", TRANSACTION_HEADER_FONT, STAPORT_BLUE, Element.ALIGN_CENTER, 1, 1));

        // Colonne 4: Total Décaissements
        table.addCell(createHeaderCell("Total\nDécaissements", TRANSACTION_HEADER_FONT, STAPORT_BLUE, Element.ALIGN_CENTER, 1, 1));

        // --- DONNÉES pour chaque banque (7 colonnes) ---
        for (RecaputilatifGlobal dto : dtos) {
            // Colonne 1: Banque
            table.addCell(createCell(dto.getBanqueName() != null ? dto.getBanqueName() : "", DATA_FONT, Element.ALIGN_LEFT));

            // Colonne 2: Solde Actuel
            table.addCell(createCell(formatCurrency(dto.getSoldeActuel()), DATA_FONT, Element.ALIGN_RIGHT));

            // Colonne 3: Total Encaissements
            table.addCell(createCell(formatCurrency(dto.getTotalEncaissements()), DATA_FONT, Element.ALIGN_RIGHT));

            // Colonne 4: Total Décaissements
            table.addCell(createCell(formatCurrency(dto.getTotalDecaissements()), DATA_FONT, Element.ALIGN_RIGHT));

            // Colonne 5: Solde fin du mois
            table.addCell(createCell(formatCurrency(dto.getSoldefin()), DATA_FONT, Element.ALIGN_RIGHT));

            // Colonne 6: Plafond FC
            table.addCell(createCell(formatCurrency(dto.getPlafondFC()), DATA_FONT, Element.ALIGN_RIGHT));

            // Colonne 7: Reste fin du mois
            table.addCell(createCell(formatCurrency(dto.getReste()), DATA_FONT, Element.ALIGN_RIGHT));
        }

        document.add(table);
    }
    /**
     * Génère le tableau détaillé des Encaissements agrégé pour toutes les banques (incluant la colonne Banque).
     */
    private void generateAggregatedRecapItemTable(Document document, List<RecaputilatifGlobal> dtos, boolean isEncaissement) throws DocumentException {
        // Cette méthode se concentre uniquement sur les encaissements pour ce rapport
        if (!isEncaissement) return;

        String title = "DÉTAIL DES ENCAISSEMENTS PRÉVUS ";
        Paragraph titleParagraph = new Paragraph(title, TITLE_FONT_BLACK);
        titleParagraph.setAlignment(Element.ALIGN_CENTER);
        titleParagraph.setSpacingAfter(10);
        document.add(titleParagraph);

        // 8 Colonnes : Banque (NOUVEAU), Date Em, Date Ech, Libelle, Débit, Crédit, Observations, Source
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);

        table.setWidths(new float[]{2.5f, 2.5f, 5f, 3f, 5f, 3f});
        table.setSpacingBefore(3);
        table.setHeaderRows(1);

        // --- Headers ---
        String[] headers = {
                "Banque", "Date d'émission", "Libellé", "Crédit", "Observations", "Source"
        };

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, TRANSACTION_HEADER_FONT));
            cell.setBackgroundColor(STAPORT_BLUE);
            cell.setPadding(2);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        // --- Données agrégées ---
        for (RecaputilatifGlobal dto : dtos) {
            List<RecaputilatifItem> items = dto.getEncaissement();
            if (items == null) continue;

            for (RecaputilatifItem item : items) {
                // 1. Banque
                table.addCell(createCell(dto.getBanqueName(), TRANSACTION_DATA_FONT, Element.ALIGN_LEFT));
                // 2. Date d'émission
                table.addCell(createCell(item.getDateEmission() != null ? DATETIME_FORMATTER.format(item.getDateEmission()) : "-", TRANSACTION_DATA_FONT, Element.ALIGN_CENTER));
                // 3. Libellé
                table.addCell(createCell(item.getLibelle(), TRANSACTION_DATA_FONT, Element.ALIGN_LEFT));

                // 4. Crédit
                PdfPCell creditCell = createCell(formatCurrency(item.getCredit()), TRANSACTION_DATA_FONT, Element.ALIGN_RIGHT);
                creditCell.setNoWrap(true);
                table.addCell(creditCell);
                // 5. Observations
                table.addCell(createCell(item.getObservations(), TRANSACTION_DATA_FONT, Element.ALIGN_LEFT));
                // 6. Source
                table.addCell(createCell(item.getSource().name(), TRANSACTION_DATA_FONT, Element.ALIGN_CENTER));
            }
        }

        document.add(table);
    }


    public byte[] generateGlobalRecapForMultipleBanks(List<RecaputilatifGlobal> dtos) throws DocumentException {

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        String reportTitle = "Récapitulatif global :  "+ today;

        return generateReport(
                reportTitle,
                (document, writer) -> {
                    try {
                        // --- 1. TITRE DU RAPPORT ---
                        Paragraph title = new Paragraph(reportTitle, TITLE_FONT_BLACK);
                        title.setAlignment(Element.ALIGN_CENTER);
                        title.setSpacingAfter(10);
                        document.add(title);

                        // ====================================================
                        // 2. SYNTHÈSE AGRÉGÉE POUR TOUTES LES BANQUES (NOUVEAU)
                        // ====================================================
                        addAggregatedSummaryTable(document, dtos);

                        // ====================================================
                        // 3. DÉTAIL ENCAISSEMENTS AGRÉGÉ (NOUVEAU)
                        // ====================================================
                        generateAggregatedRecapItemTable(document, dtos, true);

                        // Saut de page pour séparer les totaux de la banque.
                        document.newPage();

                    } catch (DocumentException e) {
                        throw new RuntimeException("Erreur lors de la génération du contenu du récapitulatif global multi-banques.", e);
                    }
                }
        );
    }

    // La méthode generateRecapGlobalReport (mono-banque) est conservée intacte.
    public byte[] generateRecapGlobalReport(RecaputilatifGlobal dto) throws DocumentException {

        String reportTitle = "Journal banque ";

        return generateReport(
                reportTitle,
                (document, writer) -> {
                    try {
                        // --- 1. TITRE DU RAPPORT (Ajouter un peu plus d'espace) ---
                        Paragraph title = new Paragraph(reportTitle, TITLE_FONT_BLACK);
                        title.setAlignment(Element.ALIGN_CENTER);
                        title.setSpacingAfter(20);
                        document.add(Chunk.NEWLINE);
                        document.add(Chunk.NEWLINE);
                        document.add(title);

                        // --- 2. SYNTHÈSE FINANCIÈRE GLOBALE ---
                        addGlobalSummaryTable(document, dto);
                        document.add(Chunk.NEWLINE);

                        // --- 3. DÉTAIL DES ENCAISSEMENTS ---
                        document.add(new Paragraph("DÉTAIL DES ENCAISSEMENTS PRÉVUS", TITLE_FONT));
                        document.add(new LineSeparator(1f, 100f, STAPORT_BLUE, Element.ALIGN_CENTER, -2));
                        document.add(Chunk.NEWLINE);

                        Map<TransactionSource, List<RecaputilatifItem>> groupedEncaissements = groupItemsBySource(dto.getEncaissement());
                        for (Map.Entry<TransactionSource, List<RecaputilatifItem>> entry : groupedEncaissements.entrySet()) {
                            document.add(new Paragraph("Source: " + entry.getKey(), BOLD_FONT));
                            generateRecapItemTable(document, entry.getValue(), true); // true = Encaissement
                            document.add(Chunk.NEWLINE);
                        }

                        // --- 4. DÉTAIL DES DÉCAISSEMENTS ---
                        document.add(new Paragraph("DÉTAIL DES DÉCAISSEMENTS PRÉVUS", TITLE_FONT));
                        document.add(new LineSeparator(1f, 100f, STAPORT_BLUE, Element.ALIGN_CENTER, -2));
                        document.add(Chunk.NEWLINE);

                        Map<TransactionSource, List<RecaputilatifItem>> groupedDecaissements = groupItemsBySource(dto.getDecaissement());
                        for (Map.Entry<TransactionSource, List<RecaputilatifItem>> entry : groupedDecaissements.entrySet()) {
                            document.add(new Paragraph("Source: " + entry.getKey(), BOLD_FONT));
                            generateRecapItemTable(document, entry.getValue(), false); // false = Décaissement
                            document.add(Chunk.NEWLINE);
                        }

                    } catch (DocumentException e) {
                        throw new RuntimeException("Erreur lors de la génération du contenu du récapitulatif global.", e);
                    }
                }
        );
    }

    /**
     * Méthode corrigée pour respecter la structure 4x4, sans bordure/background, et avec alignement extrême.
     */
    private void addGlobalSummaryTable(Document document, RecaputilatifGlobal dto) throws DocumentException {

        // Le tableau reste à 4 colonnes.
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);

        // Largeurs ajustées: [Label (3f), Value (2f), Label (3f), Value (2f)]
        table.setWidths(new float[]{3f, 2f, 3f, 2f});
        table.setSpacingAfter(10);

        // --- Ligne 1 : Date — Banque ---

        // Col 1 : Label "Date du Rapport:" (Gauche)
        addSummaryDataCell(table, "Date:", BOLD_FONT, Element.ALIGN_LEFT);
        // Col 2 : Valeur Date (Droite)
        addSummaryDataCell(table, DATETIME_FORMATTER.format(java.time.OffsetDateTime.now()), DATA_FONT, Element.ALIGN_RIGHT);

        // Col 3 : Label "Banque:" (Gauche)
        addSummaryDataCell(table, "Banque:", BOLD_FONT, Element.ALIGN_LEFT);
        // Col 4 : Valeur Banque (Droite)
        addSummaryDataCell(table, dto.getBanqueName(), DATA_FONT, Element.ALIGN_RIGHT);


        // --- Ligne 2 : Solde actuel — Solde fin du mois ---

        // Col 1 : Label "Solde Actuel:" (Gauche)
        addSummaryDataCell(table, "Solde Actuel:", BOLD_FONT, Element.ALIGN_LEFT);
        // Col 2 : Valeur Solde Actuel (Droite)
        addSummaryDataCell(table, formatCurrency(dto.getSoldeActuel()), DATA_FONT, Element.ALIGN_RIGHT);

        // Col 3 : Label "Solde Fin Prévu:" (Gauche)
        addSummaryDataCell(table, "Solde Fin du mois:", BOLD_FONT, Element.ALIGN_LEFT);
        // Col 4 : Valeur Solde Fin (Droite)
        addSummaryDataCell(table, formatCurrency(dto.getSoldefin()), DATA_FONT, Element.ALIGN_RIGHT);


        // --- Ligne 3 : Plafond FC — Reste fin du mois ---

        // Col 1 : Label "Plafond FC:" (Gauche)
        addSummaryDataCell(table, "Plafond FC:", BOLD_FONT, Element.ALIGN_LEFT);
        // Col 2 : Valeur Plafond FC (Droite)
        addSummaryDataCell(table, formatCurrency(dto.getPlafondFC()), DATA_FONT, Element.ALIGN_RIGHT);

        // Col 3 : Label "Reste Disponible:" (Gauche)
        addSummaryDataCell(table, "Reste fin du mois:", BOLD_FONT, Element.ALIGN_LEFT);
        // Col 4 : Valeur Reste (Droite)
        addSummaryDataCell(table, formatCurrency(dto.getReste()), DATA_FONT, Element.ALIGN_RIGHT);


        // --- Ligne 4 : Total décaissements — Total encaissements ---

        // Col 1 : Label "Total Décaissements:" (Gauche)
        addSummaryDataCell(table, "Total Décaissements:", BOLD_FONT, Element.ALIGN_LEFT);
        // Col 2 : Valeur Décaissements (Droite)
        addSummaryDataCell(table, formatCurrency(dto.getTotalDecaissements()), DATA_FONT, Element.ALIGN_RIGHT);

        // Col 3 : Label "Total Encaissements:" (Gauche)
        addSummaryDataCell(table, "Total Encaissements:", BOLD_FONT, Element.ALIGN_LEFT);
        // Col 4 : Valeur Encaissements (Droite)
        addSummaryDataCell(table, formatCurrency(dto.getTotalEncaissements()), DATA_FONT, Element.ALIGN_RIGHT);


        document.add(table);
    }

    /**
     * Utilitaire pour ajouter une seule cellule à la table de synthèse (sans bordure ni background).
     */
    private void addSummaryDataCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = createCell(text, font, alignment);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    /**
     * Nouvelle méthode pour imprimer les détails des RecaputilatifItem
     */
    private void generateRecapItemTable(Document document, List<RecaputilatifItem> items, boolean isEncaissement) throws DocumentException {

        // 7 Colonnes : Date Em, Date Ech, Libelle, Débit, Crédit, Observations, Source
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);

        // Largeurs ajustées (poids total ~25f)
        table.setWidths(new float[]{3f, 3f, 5f, 3f, 3f, 5f, 3f});
        table.setSpacingBefore(3);
        table.setHeaderRows(1);

        // --- Headers ---
        String[] headers = {
                "Date d'émission", "Date d'échéance", "Libellé",
                "Débit", "Crédit", "Observations", "Source"
        };

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, TRANSACTION_HEADER_FONT));
            cell.setBackgroundColor(STAPORT_BLUE);
            cell.setPadding(2);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        // --- Données ---
        for (RecaputilatifItem item : items) {
            // 1. Date d'émission
            table.addCell(createCell(item.getDateEmission() != null ? DATETIME_FORMATTER.format(item.getDateEmission()) : "-", TRANSACTION_DATA_FONT, Element.ALIGN_CENTER));

            // 2. Date d'échéance
            table.addCell(createCell(item.getDateEcheance() != null ? DATETIME_FORMATTER.format(item.getDateEcheance()) : "-", TRANSACTION_DATA_FONT, Element.ALIGN_CENTER));

            // 3. Libellé (Aligné à gauche)
            table.addCell(createCell(item.getLibelle(), TRANSACTION_DATA_FONT, Element.ALIGN_LEFT));

            // 4. Débit (Affiché même si null/zero si c'est un encaissement)
            PdfPCell debitCell = createCell(formatCurrency(item.getDebit()), TRANSACTION_DATA_FONT, Element.ALIGN_RIGHT);
            debitCell.setNoWrap(true);
            table.addCell(debitCell);

            // 5. Crédit (Affiché même si null/zero si c'est un décaissement)
            PdfPCell creditCell = createCell(formatCurrency(item.getCredit()), TRANSACTION_DATA_FONT, Element.ALIGN_RIGHT);
            creditCell.setNoWrap(true);
            table.addCell(creditCell);

            // 6. Observations (Aligné à gauche)
            table.addCell(createCell(item.getObservations(), TRANSACTION_DATA_FONT, Element.ALIGN_LEFT));

            // 7. Source
            table.addCell(createCell(item.getSource().name(), TRANSACTION_DATA_FONT, Element.ALIGN_CENTER));
        }

        document.add(table);
    }
}