package staport.finance.gBanque.reports.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.stereotype.Service;
import staport.finance.gBanque.rapprochement.dtos.ReportDto;
import staport.finance.gBanque.recapitulatif.dtos.RecaputilatifGlobal;
import staport.finance.gBanque.recapitulatif.dtos.RecaputilatifItem;
import staport.finance.gBanque.reports.enums.ReportType;
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
import staport.finance.gBanque.recapitulatif.dtos.CategorieAmountDto;
import staport.finance.gBanque.recapitulatif.dtos.RecapGeneralDetailleAllBanquesDto;
import staport.finance.gBanque.recapitulatif.dtos.RecapGeneralDetailleDto;
import staport.finance.gBanque.recapitulatif.dtos.ResumeSectionDto;

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
    public byte[] generateRapprochementReport(ReportDto dto, ReportType reportType) throws DocumentException {

        String reportTitle = "Rapprochement Bancaire";
        boolean isFinance = reportType == ReportType.FINANCE;

        return generateReport(
                reportTitle,
                (document, writer) -> {
                    try {
                        // ----------------------------------------------------
                        // SECTION 1 : TITRE + EN-TÊTE
                        // ----------------------------------------------------
                        Paragraph title = new Paragraph(reportTitle, TITLE_FONT_BLACK);
                        title.setAlignment(Element.ALIGN_CENTER);
                        title.setSpacingAfter(10);
                        document.add(Chunk.NEWLINE);
                        document.add(Chunk.NEWLINE);
                        document.add(title);

                        PdfPTable headerTable = new PdfPTable(3);
                        headerTable.setWidthPercentage(100);
                        headerTable.setWidths(new float[]{3f, 2f, 3f});
                        headerTable.setSpacingAfter(10);

                        PdfPCell bankCell = new PdfPCell(new Phrase("Banque: " + dto.getBanqueNom(), BOLD_FONT));
                        bankCell.setBorder(Rectangle.NO_BORDER);
                        bankCell.setPadding(3);
                        headerTable.addCell(bankCell);

                        PdfPCell dateCell = new PdfPCell(new Phrase("Date: " + dto.getDateRapprochement().format(DATE_FORMATTER), BOLD_FONT));
                        dateCell.setBorder(Rectangle.NO_BORDER);
                        dateCell.setPadding(3);
                        headerTable.addCell(dateCell);

                        PdfPCell soldeDebutCell = new PdfPCell(new Phrase("Solde Initial: " + formatCurrency(dto.getSoldeDebut()), BOLD_FONT));
                        soldeDebutCell.setBorder(Rectangle.NO_BORDER);
                        soldeDebutCell.setPadding(3);
                        soldeDebutCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        headerTable.addCell(soldeDebutCell);

                        document.add(headerTable);

                        // ----------------------------------------------------
                        // SECTION 2 : TABLEAU DES TRANSACTIONS
                        // ----------------------------------------------------
                        if (dto.getTransactions() != null && !dto.getTransactions().isEmpty()) {
                            generateTransactionsTable(document, dto.getTransactions(), isFinance);
                        } else {
                            document.add(new Paragraph("Aucune transaction trouvée pour cette période.", SUBTITLE_FONT));
                        }

                        // ----------------------------------------------------
                        // SECTION 3 : TOTAUX DÉBIT / CRÉDIT (+ TVA si Finance)
                        // ----------------------------------------------------
                        if (isFinance) {
                            PdfPTable totalsTable = new PdfPTable(4);
                            totalsTable.setWidthPercentage(100);
                            totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
                            totalsTable.setWidths(new float[]{3f, 2f, 3f, 2f});

                            addTotalCell(totalsTable, "TOTAL DÉBIT :", formatCurrency(dto.getTotalDebit()));
                            addTotalCell(totalsTable, "TOTAL CRÉDIT :", formatCurrency(dto.getTotalCredit()));
                            addTotalCell(totalsTable, "TOTAL TVA DÉDUCTIBLE :", formatCurrency(dto.getTvaDeductible()));
                            addTotalCell(totalsTable, "TOTAL TVA COLLECTÉE :", formatCurrency(dto.getTvaCollectee()));

                            document.add(totalsTable);
                        } else {
                            PdfPTable totalsTable = new PdfPTable(2);
                            totalsTable.setWidthPercentage(50);
                            totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
                            totalsTable.setWidths(new float[]{3f, 2f});

                            addTotalCell(totalsTable, "TOTAL DÉBIT :", formatCurrency(dto.getTotalDebit()));
                            addTotalCell(totalsTable, "TOTAL CRÉDIT :", formatCurrency(dto.getTotalCredit()));

                            document.add(totalsTable);
                        }

                        // ----------------------------------------------------
                        // SECTION 4 : SOLDE FINAL (+ TVA Due si Finance)
                        // ----------------------------------------------------
                        if (isFinance) {
                            PdfPTable finalTable = new PdfPTable(4);
                            finalTable.setWidthPercentage(100);
                            finalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
                            finalTable.setWidths(new float[]{3f, 2f, 3f, 2f});

                            addTotalCell(finalTable, "SOLDE FINAL :", formatCurrency(dto.getSoldeFin()));
                            addTotalCell(finalTable, "TVA DUE :", formatCurrency(dto.getTvaDue()));

                            document.add(finalTable);
                        } else {
                            PdfPTable finalTable = new PdfPTable(2);
                            finalTable.setWidthPercentage(50);
                            finalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
                            finalTable.setWidths(new float[]{3f, 2f});

                            addTotalCell(finalTable, "SOLDE FINAL :", formatCurrency(dto.getSoldeFin()));

                            document.add(finalTable);
                        }

                    } catch (DocumentException e) {
                        throw new RuntimeException("Erreur lors de la génération du contenu du rapport de rapprochement.", e);
                    }
                }
        );
    }    /**
     * Génère le tableau des transactions (sans ligne de totaux interne).
     */
    private void generateTransactionsTable(Document document,
                                           List<TransactionResponseDto> transactions,
                                           boolean isFinance) throws DocumentException {

        PdfPTable table = new PdfPTable(isFinance ? 9 : 6);
        table.setWidthPercentage(100);
        table.setSpacingBefore(3);
        table.setSpacingAfter(3);
        table.setHeaderRows(1);

        if (isFinance) {
            table.setWidths(new float[]{2.5f, 2.5f, 2.5f, 3f, 3f, 3f, 3f, 3f, 2.5f});
        } else {
            table.setWidths(new float[]{2.5f, 2.5f, 4f, 3f, 3f, 3f});
        }

        // --- Headers ---
        String[] headersFinance = {
                "Date d'émission", "Date d'opération", "Date de valeur",
                "Libellé", "Débit", "Crédit",
                "TVA Déductible", "TVA Collectée", "Observation"
        };
        String[] headersCompta = {
                "Date d'opération", "Date de valeur",
                "Libellé", "Débit", "Crédit", "Observation"
        };

        for (String header : isFinance ? headersFinance : headersCompta) {
            PdfPCell cell = new PdfPCell(new Phrase(header, TRANSACTION_HEADER_FONT));
            cell.setBackgroundColor(STAPORT_BLUE);
            cell.setNoWrap(false);
            cell.setPadding(2);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell);
        }

        // --- Données ---
        for (TransactionResponseDto tx : transactions) {

            // Date Émission — Finance uniquement
            if (isFinance) {
                PdfPCell dateEmCell = createCell(
                        tx.getDateEmission() != null ? tx.getDateEmission().format(DATETIME_FORMATTER) : "-",
                        TRANSACTION_DATA_FONT, Element.ALIGN_CENTER);
                dateEmCell.setNoWrap(true);
                table.addCell(dateEmCell);
            }

            // Date Opération
            PdfPCell dateOpCell = createCell(
                    tx.getDateOperations() != null ? tx.getDateOperations().format(DATETIME_FORMATTER) : "-",
                    TRANSACTION_DATA_FONT, Element.ALIGN_CENTER);
            dateOpCell.setNoWrap(true);
            table.addCell(dateOpCell);

            // Date Valeur
            PdfPCell dateValCell = createCell(
                    tx.getDateValeur() != null ? tx.getDateValeur().format(DATETIME_FORMATTER) : "-",
                    TRANSACTION_DATA_FONT, Element.ALIGN_CENTER);
            dateValCell.setNoWrap(true);
            table.addCell(dateValCell);

            // Libellé
            table.addCell(createCell(tx.getLibelle(), TRANSACTION_DATA_FONT, Element.ALIGN_LEFT));

            // Débit
            PdfPCell debitCell = createCell(formatCurrency(tx.getDebit()), TRANSACTION_DATA_FONT, Element.ALIGN_RIGHT);
            debitCell.setNoWrap(true);
            table.addCell(debitCell);

            // Crédit
            PdfPCell creditCell = createCell(formatCurrency(tx.getCredit()), TRANSACTION_DATA_FONT, Element.ALIGN_RIGHT);
            creditCell.setNoWrap(true);
            table.addCell(creditCell);

            // TVA Déductible + TVA Collectée — Finance uniquement
            if (isFinance) {
                PdfPCell tvaDedCell = createCell(formatCurrency(tx.getTvaDeductible()), TRANSACTION_DATA_FONT, Element.ALIGN_RIGHT);
                tvaDedCell.setNoWrap(true);
                table.addCell(tvaDedCell);

                PdfPCell tvaCollCell = createCell(formatCurrency(tx.getTvaCollectee()), TRANSACTION_DATA_FONT, Element.ALIGN_RIGHT);
                tvaCollCell.setNoWrap(true);
                table.addCell(tvaCollCell);
            }

            // Observation
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

    private void addTotalCell(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, BOLD_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setBackgroundColor(Color.decode("#f0f0f0"));
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, BOLD_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setBackgroundColor(Color.decode("#f0f0f0"));
        table.addCell(valueCell);
    }


    // ****************************** Récapitulatif général

    public byte[] generateRecapGeneralDetailleReport(RecapGeneralDetailleDto dto) throws DocumentException {
        String today = dto.getDateRecap().format(DATETIME_FORMATTER);
        String reportTitle = "Récapitulation Générale Détaillée — " + dto.getBanqueNom() + " — " + today;

        return generateReport(reportTitle, (document, writer) -> {
            try {
                Paragraph title = new Paragraph(reportTitle, TITLE_FONT_BLACK);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(15);
                document.add(Chunk.NEWLINE);
                document.add(title);

                addRecapResumeSectionTable(document, dto.getResume());
                document.add(Chunk.NEWLINE);

                document.add(new Paragraph("DÉCAISSEMENTS PRÉVUS — J+1 à fin de mois", TITLE_FONT));
                document.add(new LineSeparator(1f, 100f, STAPORT_BLUE, Element.ALIGN_CENTER, -2));
                document.add(Chunk.NEWLINE);
                addRecapCategoriesTable(document, dto.getDecaissements().getCategories(), dto.getDecaissements().getTotal());
                document.add(Chunk.NEWLINE);

                document.add(new Paragraph("ENCAISSEMENTS PRÉVUS — J+1 à fin de mois", TITLE_FONT));
                document.add(new LineSeparator(1f, 100f, STAPORT_BLUE, Element.ALIGN_CENTER, -2));
                document.add(Chunk.NEWLINE);
                addRecapCategoriesTable(document, dto.getEncaissements().getCategories(), dto.getEncaissements().getTotal());
                document.add(Chunk.NEWLINE);

                // Solde prévisionnel fin de mois
                PdfPTable soldeTable = new PdfPTable(2);
                soldeTable.setWidthPercentage(50);
                soldeTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
                soldeTable.setWidths(new float[]{4f, 3f});
                addTotalCell(soldeTable, "SOLDE PRÉVISIONNEL FIN DE MOIS :", formatCurrency(dto.getSoldePrevisionnelFinMois()));
                document.add(soldeTable);

            } catch (DocumentException e) {
                throw new RuntimeException("Erreur génération PDF recap détaillé.", e);
            }
        });
    }

// ─── PUBLIC : toutes les banques ─────────────────────────────────────────────

    public byte[] generateRecapGeneralDetailleAllBanquesReport(RecapGeneralDetailleAllBanquesDto dto) throws DocumentException {
        String today = dto.getDateRecap().format(DATETIME_FORMATTER);
        String reportTitle = "Récapitulation Générale Détaillée — Toutes Banques — " + today;

        return generateReport(reportTitle, (document, writer) -> {
            try {
                Paragraph mainTitle = new Paragraph(reportTitle, TITLE_FONT_BLACK);
                mainTitle.setAlignment(Element.ALIGN_CENTER);
                mainTitle.setSpacingAfter(15);
                document.add(Chunk.NEWLINE);
                document.add(mainTitle);

                for (RecapGeneralDetailleDto banqueDto : dto.getBanques()) {
                    // Titre par banque
                    Paragraph banqueTitre = new Paragraph(banqueDto.getBanqueNom(), SUBTITLE_FONT);
                    banqueTitre.setSpacingAfter(8);
                    document.add(banqueTitre);
                    document.add(new LineSeparator(1f, 100f, STAPORT_BLUE, Element.ALIGN_CENTER, -2));
                    document.add(Chunk.NEWLINE);

                    addRecapResumeSectionTable(document, banqueDto.getResume());
                    document.add(Chunk.NEWLINE);

                    document.add(new Paragraph("DÉCAISSEMENTS — J+1 à fin de mois", BOLD_FONT));
                    addRecapCategoriesTable(document, banqueDto.getDecaissements().getCategories(), banqueDto.getDecaissements().getTotal());
                    document.add(Chunk.NEWLINE);

                    document.add(new Paragraph("ENCAISSEMENTS — J+1 à fin de mois", BOLD_FONT));
                    addRecapCategoriesTable(document, banqueDto.getEncaissements().getCategories(), banqueDto.getEncaissements().getTotal());
                    document.add(Chunk.NEWLINE);

                    PdfPTable soldeTable = new PdfPTable(2);
                    soldeTable.setWidthPercentage(50);
                    soldeTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    soldeTable.setWidths(new float[]{4f, 3f});
                    addTotalCell(soldeTable, "SOLDE PRÉVISIONNEL FIN DE MOIS :", formatCurrency(banqueDto.getSoldePrevisionnelFinMois()));
                    document.add(soldeTable);

                    document.newPage();
                }

            } catch (DocumentException e) {
                throw new RuntimeException("Erreur génération PDF recap détaillé multi-banques.", e);
            }
        });
    }

// ─── PRIVATE : section résumé Jour J ─────────────────────────────────────────

    private void addRecapResumeSectionTable(Document document, ResumeSectionDto resume) throws DocumentException {
        document.add(new Paragraph("RÉSUMÉ — JOUR J", TITLE_FONT));
        document.add(new LineSeparator(1f, 100f, STAPORT_BLUE, Element.ALIGN_CENTER, -2));
        document.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 2f, 3f, 2f});
        table.setSpacingAfter(8);

        addSummaryDataCell(table, "Solde Actuel :",        BOLD_FONT, Element.ALIGN_LEFT);
        addSummaryDataCell(table, formatCurrency(resume.getSoldeActuel()),  DATA_FONT, Element.ALIGN_RIGHT);
        addSummaryDataCell(table, "Plafond Ligne FC :",    BOLD_FONT, Element.ALIGN_LEFT);
        addSummaryDataCell(table, formatCurrency(resume.getPlafondLigne()), DATA_FONT, Element.ALIGN_RIGHT);

        addSummaryDataCell(table, "Disponibilité :",       BOLD_FONT, Element.ALIGN_LEFT);
        addSummaryDataCell(table, formatCurrency(resume.getDisponibilite()), DATA_FONT, Element.ALIGN_RIGHT);
        addSummaryDataCell(table, "Solde Projeté J :",     BOLD_FONT, Element.ALIGN_LEFT);
        addSummaryDataCell(table, formatCurrency(resume.getSoldeProjete()),  DATA_FONT, Element.ALIGN_RIGHT);

        addSummaryDataCell(table, "Encours+ (J) :",        BOLD_FONT, Element.ALIGN_LEFT);
        addSummaryDataCell(table, formatCurrency(resume.getEncoursPlus()),  DATA_FONT, Element.ALIGN_RIGHT);
        addSummaryDataCell(table, "Encours- (J) :",        BOLD_FONT, Element.ALIGN_LEFT);
        addSummaryDataCell(table, formatCurrency(resume.getEncoursMoins()), DATA_FONT, Element.ALIGN_RIGHT);

        document.add(table);
    }

// ─── PRIVATE : tableau catégories (décaissements ou encaissements) ────────────

    private void addRecapCategoriesTable(Document document, List<CategorieAmountDto> categories, BigDecimal total) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(60);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.setWidths(new float[]{4f, 2f});
        table.setSpacingBefore(3);
        table.setSpacingAfter(3);
        table.setHeaderRows(1);

        // Header
        PdfPCell hCategorie = new PdfPCell(new Phrase("Catégorie", TRANSACTION_HEADER_FONT));
        hCategorie.setBackgroundColor(STAPORT_BLUE);
        hCategorie.setPadding(4);
        hCategorie.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(hCategorie);

        PdfPCell hMontant = new PdfPCell(new Phrase("Montant", TRANSACTION_HEADER_FONT));
        hMontant.setBackgroundColor(STAPORT_BLUE);
        hMontant.setPadding(4);
        hMontant.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(hMontant);

        // Lignes
        for (CategorieAmountDto cat : categories) {
            table.addCell(createCell(cat.getCategorie(), TRANSACTION_DATA_FONT, Element.ALIGN_LEFT));
            PdfPCell montantCell = createCell(formatCurrency(cat.getMontant()), TRANSACTION_DATA_FONT, Element.ALIGN_RIGHT);
            montantCell.setNoWrap(true);
            table.addCell(montantCell);
        }

        // Ligne total
        PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL", BOLD_FONT));
        totalLabel.setBackgroundColor(Color.decode("#f0f0f0"));
        totalLabel.setPadding(4);
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(totalLabel);

        PdfPCell totalValue = new PdfPCell(new Phrase(formatCurrency(total), BOLD_FONT));
        totalValue.setBackgroundColor(Color.decode("#f0f0f0"));
        totalValue.setPadding(4);
        totalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValue.setNoWrap(true);
        table.addCell(totalValue);

        document.add(table);
    }
}