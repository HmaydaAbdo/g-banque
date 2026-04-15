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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import staport.finance.gBanque.recapitulatif.dtos.CategorieAmountDto;
import staport.finance.gBanque.recapitulatif.dtos.RecapGeneralDetailleAllBanquesDto;
import staport.finance.gBanque.recapitulatif.dtos.RecapGeneralDetailleDto;
import staport.finance.gBanque.recapitulatif.dtos.ResumeSectionDto;

@Service
public class StaportPdfReportService {

    // Constantes de style pour les rapports
    private static final Color STAPORT_BLUE = Color.decode("#1a5490");
    private static final Font TITLE_FONT_BLACK = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, STAPORT_BLUE);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font DATA_FONT = FontFactory.getFont(FontFactory.HELVETICA, 12);
    private static final Font TRANSACTION_HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6, Color.WHITE);
    private static final Font TRANSACTION_DATA_FONT = FontFactory.getFont(FontFactory.HELVETICA, 6);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Génère un rapport PDF générique avec en-tête et pied de page STAPORT automatiques.
     */
    public byte[] generateReport(String reportTitle, BiConsumer<Document, PdfWriter> contentGenerator)
            throws DocumentException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 20, 20, 80, 50);

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
     * Surcharge avec PageSize paramétrable (ex: A4 paysage pour multi-banques).
     */
    public byte[] generateReport(String reportTitle, Rectangle pageSize, BiConsumer<Document, PdfWriter> contentGenerator)
            throws DocumentException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(pageSize, 20, 20, 80, 50);

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
     */
    public byte[] generateRapprochementReport(ReportDto dto, ReportType reportType) throws DocumentException {

        String reportTitle = "Rapprochement Bancaire";
        boolean isFinance = reportType == ReportType.FINANCE;

        return generateReport(
                reportTitle,
                (document, writer) -> {
                    try {
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

                        if (dto.getTransactions() != null && !dto.getTransactions().isEmpty()) {
                            generateTransactionsTable(document, dto.getTransactions(), isFinance);
                        } else {
                            document.add(new Paragraph("Aucune transaction trouvée pour cette période.", SUBTITLE_FONT));
                        }

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
    }

    private void generateTransactionsTable(Document document,
                                           List<TransactionResponseDto> transactions,
                                           boolean isFinance) throws DocumentException {

        PdfPTable table = new PdfPTable(isFinance ? 10 : 7);
        table.setWidthPercentage(100);
        table.setSpacingBefore(3);
        table.setSpacingAfter(3);
        table.setHeaderRows(1);

        if (isFinance) {
            table.setWidths(new float[]{2.5f, 2.5f, 2.5f, 3f, 3f, 3f, 3f, 3f, 3f,4f});
        } else {
            table.setWidths(new float[]{2.5f, 2.5f, 3f, 3f, 3f, 3f,4f});
        }

        String[] headersFinance = {
                "Date d'émission", "Date d'opération", "Date de valeur",
                "Libellé", "Débit", "Crédit",
                "TVA Déductible", "TVA Collectée", "Observation","Source"
        };
        String[] headersCompta = {
                "Date d'opération", "Date de valeur",
                "Libellé", "Débit", "Crédit", "Observation","Source"
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

        for (TransactionResponseDto tx : transactions) {
            if (isFinance) {
                PdfPCell dateEmCell = createCell(
                        tx.getDateEmission() != null ? tx.getDateEmission().format(DATETIME_FORMATTER) : "-",
                        TRANSACTION_DATA_FONT, Element.ALIGN_CENTER);
                dateEmCell.setNoWrap(true);
                table.addCell(dateEmCell);
            }

            PdfPCell dateOpCell = createCell(
                    tx.getDateOperations() != null ? tx.getDateOperations().format(DATETIME_FORMATTER) : "-",
                    TRANSACTION_DATA_FONT, Element.ALIGN_CENTER);
            dateOpCell.setNoWrap(true);
            table.addCell(dateOpCell);

            PdfPCell dateValCell = createCell(
                    tx.getDateValeur() != null ? tx.getDateValeur().format(DATETIME_FORMATTER) : "-",
                    TRANSACTION_DATA_FONT, Element.ALIGN_CENTER);
            dateValCell.setNoWrap(true);
            table.addCell(dateValCell);

            PdfPCell libelleCell=createCell(tx.getLibelle(), TRANSACTION_DATA_FONT, Element.ALIGN_LEFT);
            table.addCell(libelleCell);

            PdfPCell debitCell = createCell(formatCurrency(tx.getDebit()), TRANSACTION_DATA_FONT, Element.ALIGN_RIGHT);
            debitCell.setNoWrap(true);
            table.addCell(debitCell);

            PdfPCell creditCell = createCell(formatCurrency(tx.getCredit()), TRANSACTION_DATA_FONT, Element.ALIGN_RIGHT);
            creditCell.setNoWrap(true);
            table.addCell(creditCell);

            if (isFinance) {
                PdfPCell tvaDedCell = createCell(formatCurrency(tx.getTvaDeductible()), TRANSACTION_DATA_FONT, Element.ALIGN_RIGHT);
                tvaDedCell.setNoWrap(true);
                table.addCell(tvaDedCell);

                PdfPCell tvaCollCell = createCell(formatCurrency(tx.getTvaCollectee()), TRANSACTION_DATA_FONT, Element.ALIGN_RIGHT);
                tvaCollCell.setNoWrap(true);
                table.addCell(tvaCollCell);
            }

            table.addCell(createCell(tx.getObservation(), TRANSACTION_DATA_FONT, Element.ALIGN_LEFT));
            PdfPCell sourceCell= createCell(
                    tx.getTransactionSource() != null ? tx.getTransactionSource().name() : "-",
                    TRANSACTION_DATA_FONT,
                    Element.ALIGN_CENTER
            );
            sourceCell.setNoWrap(true);
            table.addCell(sourceCell);
        }

        document.add(table);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "-";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
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

    private void addAggregatedSummaryTable(Document document, List<RecaputilatifGlobal> dtos) throws DocumentException {
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f});
        table.setSpacingAfter(10);
        table.setHeaderRows(2);

        table.addCell(createHeaderCell("Banque", TRANSACTION_HEADER_FONT, STAPORT_BLUE, Element.ALIGN_CENTER, 1, 2));
        table.addCell(createHeaderCell("Solde Actuel", TRANSACTION_HEADER_FONT, STAPORT_BLUE, Element.ALIGN_CENTER, 1, 2));
        table.addCell(createHeaderCell("Encours", TRANSACTION_HEADER_FONT, STAPORT_BLUE, Element.ALIGN_CENTER, 2, 1));
        table.addCell(createHeaderCell("Solde\nfin du mois", TRANSACTION_HEADER_FONT, STAPORT_BLUE, Element.ALIGN_CENTER, 1, 2));
        table.addCell(createHeaderCell("Plafond FC", TRANSACTION_HEADER_FONT, STAPORT_BLUE, Element.ALIGN_CENTER, 1, 2));
        table.addCell(createHeaderCell("Reste\nfin du mois", TRANSACTION_HEADER_FONT, STAPORT_BLUE, Element.ALIGN_CENTER, 1, 2));
        table.addCell(createHeaderCell("Total\nEncaissements", TRANSACTION_HEADER_FONT, STAPORT_BLUE, Element.ALIGN_CENTER, 1, 1));
        table.addCell(createHeaderCell("Total\nDécaissements", TRANSACTION_HEADER_FONT, STAPORT_BLUE, Element.ALIGN_CENTER, 1, 1));

        for (RecaputilatifGlobal dto : dtos) {
            table.addCell(createCell(dto.getBanqueName() != null ? dto.getBanqueName() : "", DATA_FONT, Element.ALIGN_LEFT));
            table.addCell(createCell(formatCurrency(dto.getSoldeActuel()), DATA_FONT, Element.ALIGN_RIGHT));
            table.addCell(createCell(formatCurrency(dto.getTotalEncaissements()), DATA_FONT, Element.ALIGN_RIGHT));
            table.addCell(createCell(formatCurrency(dto.getTotalDecaissements()), DATA_FONT, Element.ALIGN_RIGHT));
            table.addCell(createCell(formatCurrency(dto.getSoldefin()), DATA_FONT, Element.ALIGN_RIGHT));
            table.addCell(createCell(formatCurrency(dto.getPlafondFC()), DATA_FONT, Element.ALIGN_RIGHT));
            table.addCell(createCell(formatCurrency(dto.getReste()), DATA_FONT, Element.ALIGN_RIGHT));
        }

        document.add(table);
    }

    private void generateAggregatedRecapItemTable(Document document, List<RecaputilatifGlobal> dtos, boolean isEncaissement) throws DocumentException {
        if (!isEncaissement) return;

        String title = "DÉTAIL DES ENCAISSEMENTS PRÉVUS ";
        Paragraph titleParagraph = new Paragraph(title, TITLE_FONT_BLACK);
        titleParagraph.setAlignment(Element.ALIGN_CENTER);
        titleParagraph.setSpacingAfter(10);
        document.add(titleParagraph);

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.5f, 2.5f, 5f, 3f, 5f, 3f});
        table.setSpacingBefore(3);
        table.setHeaderRows(1);

        String[] headers = { "Banque", "Date d'émission", "Libellé", "Crédit", "Observations", "Source" };
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, TRANSACTION_HEADER_FONT));
            cell.setBackgroundColor(STAPORT_BLUE);
            cell.setPadding(2);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        for (RecaputilatifGlobal dto : dtos) {
            List<RecaputilatifItem> items = dto.getEncaissement();
            if (items == null) continue;
            for (RecaputilatifItem item : items) {
                table.addCell(createCell(dto.getBanqueName(), TRANSACTION_DATA_FONT, Element.ALIGN_LEFT));
                table.addCell(createCell(item.getDateEmission() != null ? DATETIME_FORMATTER.format(item.getDateEmission()) : "-", TRANSACTION_DATA_FONT, Element.ALIGN_CENTER));
                table.addCell(createCell(item.getLibelle(), TRANSACTION_DATA_FONT, Element.ALIGN_LEFT));
                PdfPCell creditCell = createCell(formatCurrency(item.getCredit()), TRANSACTION_DATA_FONT, Element.ALIGN_RIGHT);
                creditCell.setNoWrap(true);
                table.addCell(creditCell);
                table.addCell(createCell(item.getObservations(), TRANSACTION_DATA_FONT, Element.ALIGN_LEFT));
                table.addCell(createCell(item.getSource().name(), TRANSACTION_DATA_FONT, Element.ALIGN_CENTER));
            }
        }

        document.add(table);
    }

    public byte[] generateGlobalRecapForMultipleBanks(List<RecaputilatifGlobal> dtos) throws DocumentException {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String reportTitle = "Récapitulatif global :  " + today;

        return generateReport(reportTitle, (document, writer) -> {
            try {
                Paragraph title = new Paragraph(reportTitle, TITLE_FONT_BLACK);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(10);
                document.add(title);

                addAggregatedSummaryTable(document, dtos);
                generateAggregatedRecapItemTable(document, dtos, true);
                document.newPage();

            } catch (DocumentException e) {
                throw new RuntimeException("Erreur lors de la génération du contenu du récapitulatif global multi-banques.", e);
            }
        });
    }

    public byte[] generateRecapGlobalReport(RecaputilatifGlobal dto) throws DocumentException {
        String reportTitle = "Journal banque ";

        return generateReport(reportTitle, (document, writer) -> {
            try {
                Paragraph title = new Paragraph(reportTitle, TITLE_FONT_BLACK);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(20);
                document.add(Chunk.NEWLINE);
                document.add(Chunk.NEWLINE);
                document.add(title);

                addGlobalSummaryTable(document, dto);
                document.add(Chunk.NEWLINE);

                document.add(new Paragraph("DÉTAIL DES ENCAISSEMENTS PRÉVUS", TITLE_FONT));
                document.add(new LineSeparator(1f, 100f, STAPORT_BLUE, Element.ALIGN_CENTER, -2));
                document.add(Chunk.NEWLINE);

                Map<TransactionSource, List<RecaputilatifItem>> groupedEncaissements = groupItemsBySource(dto.getEncaissement());
                for (Map.Entry<TransactionSource, List<RecaputilatifItem>> entry : groupedEncaissements.entrySet()) {
                    document.add(new Paragraph("Source: " + entry.getKey(), BOLD_FONT));
                    generateRecapItemTable(document, entry.getValue(), true);
                    document.add(Chunk.NEWLINE);
                }

                document.add(new Paragraph("DÉTAIL DES DÉCAISSEMENTS PRÉVUS", TITLE_FONT));
                document.add(new LineSeparator(1f, 100f, STAPORT_BLUE, Element.ALIGN_CENTER, -2));
                document.add(Chunk.NEWLINE);

                Map<TransactionSource, List<RecaputilatifItem>> groupedDecaissements = groupItemsBySource(dto.getDecaissement());
                for (Map.Entry<TransactionSource, List<RecaputilatifItem>> entry : groupedDecaissements.entrySet()) {
                    document.add(new Paragraph("Source: " + entry.getKey(), BOLD_FONT));
                    generateRecapItemTable(document, entry.getValue(), false);
                    document.add(Chunk.NEWLINE);
                }

            } catch (DocumentException e) {
                throw new RuntimeException("Erreur lors de la génération du contenu du récapitulatif global.", e);
            }
        });
    }

    private void addGlobalSummaryTable(Document document, RecaputilatifGlobal dto) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 2f, 3f, 2f});
        table.setSpacingAfter(10);

        addSummaryDataCell(table, "Date:", BOLD_FONT, Element.ALIGN_LEFT);
        addSummaryDataCell(table, DATETIME_FORMATTER.format(java.time.OffsetDateTime.now()), DATA_FONT, Element.ALIGN_RIGHT);
        addSummaryDataCell(table, "Banque:", BOLD_FONT, Element.ALIGN_LEFT);
        addSummaryDataCell(table, dto.getBanqueName(), DATA_FONT, Element.ALIGN_RIGHT);

        addSummaryDataCell(table, "Solde Actuel:", BOLD_FONT, Element.ALIGN_LEFT);
        addSummaryDataCell(table, formatCurrency(dto.getSoldeActuel()), DATA_FONT, Element.ALIGN_RIGHT);
        addSummaryDataCell(table, "Solde Fin du mois:", BOLD_FONT, Element.ALIGN_LEFT);
        addSummaryDataCell(table, formatCurrency(dto.getSoldefin()), DATA_FONT, Element.ALIGN_RIGHT);

        addSummaryDataCell(table, "Plafond FC:", BOLD_FONT, Element.ALIGN_LEFT);
        addSummaryDataCell(table, formatCurrency(dto.getPlafondFC()), DATA_FONT, Element.ALIGN_RIGHT);
        addSummaryDataCell(table, "Reste fin du mois:", BOLD_FONT, Element.ALIGN_LEFT);
        addSummaryDataCell(table, formatCurrency(dto.getReste()), DATA_FONT, Element.ALIGN_RIGHT);

        addSummaryDataCell(table, "Total Décaissements:", BOLD_FONT, Element.ALIGN_LEFT);
        addSummaryDataCell(table, formatCurrency(dto.getTotalDecaissements()), DATA_FONT, Element.ALIGN_RIGHT);
        addSummaryDataCell(table, "Total Encaissements:", BOLD_FONT, Element.ALIGN_LEFT);
        addSummaryDataCell(table, formatCurrency(dto.getTotalEncaissements()), DATA_FONT, Element.ALIGN_RIGHT);

        document.add(table);
    }

    private void addSummaryDataCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = createCell(text, font, alignment);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    private void generateRecapItemTable(Document document, List<RecaputilatifItem> items, boolean isEncaissement) throws DocumentException {
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 3f, 5f, 3f, 3f, 5f, 3f});
        table.setSpacingBefore(3);
        table.setHeaderRows(1);

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

        for (RecaputilatifItem item : items) {
            table.addCell(createCell(item.getDateEmission() != null ? DATETIME_FORMATTER.format(item.getDateEmission()) : "-", TRANSACTION_DATA_FONT, Element.ALIGN_CENTER));
            table.addCell(createCell(item.getDateEcheance() != null ? DATETIME_FORMATTER.format(item.getDateEcheance()) : "-", TRANSACTION_DATA_FONT, Element.ALIGN_CENTER));
            table.addCell(createCell(item.getLibelle(), TRANSACTION_DATA_FONT, Element.ALIGN_LEFT));

            PdfPCell debitCell = createCell(formatCurrency(item.getDebit()), TRANSACTION_DATA_FONT, Element.ALIGN_RIGHT);
            debitCell.setNoWrap(true);
            table.addCell(debitCell);

            PdfPCell creditCell = createCell(formatCurrency(item.getCredit()), TRANSACTION_DATA_FONT, Element.ALIGN_RIGHT);
            creditCell.setNoWrap(true);
            table.addCell(creditCell);

            table.addCell(createCell(item.getObservations(), TRANSACTION_DATA_FONT, Element.ALIGN_LEFT));
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


    // ========================== RÉCAPITULATIF GÉNÉRAL DÉTAILLÉ ==========================

    /**
     * PDF mono-banque : même format que le multi-banques, mais avec une seule colonne banque.
     */
    public byte[] generateRecapGeneralDetailleReport(RecapGeneralDetailleDto dto) throws DocumentException {
        String today = dto.getDateRecap().format(DATETIME_FORMATTER);
        String reportTitle = "Récapitulation Générale Détaillée — " + dto.getBanqueNom() + " — " + today;

        List<RecapGeneralDetailleDto> banques = List.of(dto);

        return generateReport(reportTitle, (document, writer) -> {
            try {
                Paragraph title = new Paragraph(reportTitle, TITLE_FONT_BLACK);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(15);
                document.add(Chunk.NEWLINE);
                document.add(title);

                document.add(buildRecapConsolideTable(banques));

            } catch (DocumentException e) {
                throw new RuntimeException("Erreur génération PDF recap détaillé.", e);
            }
        });
    }

    /**
     * PDF toutes banques : tableau consolidé multi-colonnes.
     */
    public byte[] generateRecapGeneralDetailleAllBanquesReport(RecapGeneralDetailleAllBanquesDto dto) throws DocumentException {
        String today = dto.getDateRecap().format(DATETIME_FORMATTER);
        String reportTitle = "Récapitulation Générale Détaillée — Toutes Banques — " + today;
        List<RecapGeneralDetailleDto> banques = dto.getBanques();
        int nbBanques = banques.size();
        Rectangle pageSize = nbBanques > 3 ? PageSize.A4.rotate() : PageSize.A4;

        return generateReport(reportTitle, pageSize, (document, writer) -> {
            try {
                Paragraph title = new Paragraph(reportTitle, TITLE_FONT_BLACK);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(15);
                document.add(Chunk.NEWLINE);
                document.add(title);

                document.add(buildRecapConsolideTable(banques));

            } catch (DocumentException e) {
                throw new RuntimeException("Erreur génération PDF recap détaillé multi-banques.", e);
            }
        });
    }

    /**
     * Construit le tableau consolidé commun (mono ou multi-banques).
     */
    private PdfPTable buildRecapConsolideTable(List<RecapGeneralDetailleDto> banques) throws DocumentException {
        int nbBanques = banques.size();
        int totalCols = 1 + nbBanques;

        PdfPTable table = new PdfPTable(totalCols);
        table.setWidthPercentage(100);

        float[] widths = new float[totalCols];
        widths[0] = 3.5f;
        for (int i = 1; i < totalCols; i++) widths[i] = 2f;
        table.setWidths(widths);
        table.setSpacingAfter(10);

        // ── Résumé du Jour J ─────────────────────────────────────────────────
        addRgdSectionHeader(table, "Résumé du Jour J", totalCols, STAPORT_BLUE);
        addRgdLabelCell(table, "");
        for (RecapGeneralDetailleDto b : banques) addRgdBankNameCell(table, b.getBanqueNom());

        addRgdDataRow(table, "Solde actuel",  banques, b -> b.getResume().getSoldeActuel(),   false);
        addRgdDataRow(table, "Plafond ligne", banques, b -> b.getResume().getPlafondLigne(),  false);
        addRgdDataRow(table, "Disponibilité", banques, b -> b.getResume().getDisponibilite(), false);
        addRgdDataRow(table, "Encours +",     banques, b -> b.getResume().getEncoursPlus(),   false);
        addRgdDataRow(table, "Encours -",     banques, b -> b.getResume().getEncoursMoins(),  false);
        addRgdDataRow(table, "Solde Projeté", banques, b -> b.getResume().getSoldeProjete(),  true);

        // ── Récapitulatif Mensuel (J+1 → Fin de mois) ────────────────────────
        addRgdSectionHeader(table, "Récapitulatif Mensuel (J+1 → Fin de mois)", totalCols, STAPORT_BLUE);

        // Décaissements
        addRgdSectionHeader(table, "Décaissements", totalCols, Color.decode("#2e6da4"));
        addRgdDataRow(table, "Total", banques, b -> b.getDecaissements().getTotal(), true);
        if (!banques.isEmpty() && banques.get(0).getDecaissements().getCategories() != null) {
            List<String> decLabels = banques.get(0).getDecaissements().getCategories()
                    .stream().map(CategorieAmountDto::getCategorie).collect(Collectors.toList());
            for (int i = 0; i < decLabels.size(); i++) {
                final int idx = i;
                addRgdDataRow(table, decLabels.get(i), banques, b -> {
                    List<CategorieAmountDto> cats = b.getDecaissements().getCategories();
                    return idx < cats.size() ? cats.get(idx).getMontant() : BigDecimal.ZERO;
                }, false);
            }
        }

        // Encaissements
        addRgdSectionHeader(table, "Encaissements", totalCols, Color.decode("#2e6da4"));
        addRgdDataRow(table, "Total", banques, b -> b.getEncaissements().getTotal(), true);
        if (!banques.isEmpty() && banques.get(0).getEncaissements().getCategories() != null) {
            List<String> encLabels = banques.get(0).getEncaissements().getCategories()
                    .stream().map(CategorieAmountDto::getCategorie).collect(Collectors.toList());
            for (int i = 0; i < encLabels.size(); i++) {
                final int idx = i;
                addRgdDataRow(table, encLabels.get(i), banques, b -> {
                    List<CategorieAmountDto> cats = b.getEncaissements().getCategories();
                    return idx < cats.size() ? cats.get(idx).getMontant() : BigDecimal.ZERO;
                }, false);
            }
        }

        // Solde fin du mois
        addRgdDataRow(table, "Solde fin du mois", banques,
                RecapGeneralDetailleDto::getSoldePrevisionnelFinMois, true);

        return table;
    }

    // ─── PRIVATE HELPERS : tableau consolidé multi-banques ───────────────────

    private void addRgdSectionHeader(PdfPTable table, String label, int colspan, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(label, TRANSACTION_HEADER_FONT));
        cell.setColspan(colspan);
        cell.setBackgroundColor(bgColor);
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private void addRgdLabelCell(PdfPTable table, String label) {
        PdfPCell cell = new PdfPCell(new Phrase(label, BOLD_FONT));
        cell.setBackgroundColor(Color.decode("#d0dff0"));
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    private void addRgdBankNameCell(PdfPTable table, String bankName) {
        PdfPCell cell = new PdfPCell(new Phrase(bankName, BOLD_FONT));
        cell.setBackgroundColor(Color.decode("#d0dff0"));
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addRgdDataRow(PdfPTable table, String label,
                               List<RecapGeneralDetailleDto> banques,
                               Function<RecapGeneralDetailleDto, BigDecimal> extractor,
                               boolean highlighted) {
        Color bg = highlighted ? Color.decode("#e8f0fe") : Color.WHITE;
        Font labelFont = highlighted ? BOLD_FONT : TRANSACTION_DATA_FONT;
        Font valueFont = highlighted ? BOLD_FONT : TRANSACTION_DATA_FONT;

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBackgroundColor(bg);
        labelCell.setPadding(4);
        labelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(labelCell);

        for (RecapGeneralDetailleDto b : banques) {
            BigDecimal val = extractor.apply(b);
            if (val == null) val = BigDecimal.ZERO;
            PdfPCell valueCell = new PdfPCell(new Phrase(formatCurrency(val), valueFont));
            valueCell.setBackgroundColor(bg);
            valueCell.setPadding(4);
            valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            valueCell.setNoWrap(true);
            table.addCell(valueCell);
        }
    }
}