package staport.finance.gBanque.moyensDePaiements.recap.services;

import com.lowagie.text.Chunk;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.moyensDePaiements.recap.dtos.request.RecapMoyenPaiementSearchDto;
import staport.finance.gBanque.moyensDePaiements.recap.dtos.response.AgregatedRecapMoyenPaiementDto;
import staport.finance.gBanque.moyensDePaiements.recap.dtos.response.RecapMoyenPaiementItem;
import staport.finance.gBanque.moyensDePaiements.recap.entities.RecapMoyenPaiementView;
import staport.finance.gBanque.moyensDePaiements.recap.mappers.RecapMoyenPaiementMapper;
import staport.finance.gBanque.moyensDePaiements.recap.repositories.RecapMoyenPaiementRepository;
import staport.finance.gBanque.moyensDePaiements.recap.services.interfaces.IRecapMoyenPaiementService;
import staport.finance.gBanque.moyensDePaiements.recap.specs.RecapMoyenPaiementSpecification;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.reports.service.StaportPdfReportService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.reports.utils.MontantFormatter;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Vue consolidée Recap Encaissement / Recap Décaissement des moyens de paiement (Chèques, LCN,
 * Virements nationaux, Virements internationaux, Mises à disposition), tous statuts confondus,
 * toutes banques confondues avec filtre banque.
 *
 * <p>Interroge {@link RecapMoyenPaiementRepository}, adossé à la vue SQL
 * {@code v_recap_moyens_paiement} — pagination, tri et agrégation réels en base, même pattern que
 * {@code RecapitulatifServiceImpl} (module États des encours / Récapitulatif).</p>
 */
@Service
@Transactional(readOnly = true)
public class RecapMoyenPaiementServiceImpl implements IRecapMoyenPaiementService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color MED_BLUE = Color.decode("#2563eb");
    private static final Color LIGHT_BLUE = Color.decode("#dbeafe");
    private static final Color ROW_ALT = Color.decode("#f8fafc");
    private static final Color BORDER_GRAY = Color.decode("#e5e7eb");

    private static final Map<TransactionSource, String> SOURCE_LABELS = Map.of(
            TransactionSource.CHEQUE, "Chèque",
            TransactionSource.LCN_ENCAISSEMENT, "LCN Encaissement",
            TransactionSource.LCN_DECAISSEMENT, "LCN Décaissement",
            TransactionSource.LCN_ESCOMPTE, "LCN Escompte",
            TransactionSource.Virement, "Virement",
            TransactionSource.MiseADisposition, "Mise à disposition",
            TransactionSource.VIREMENT_INTERNATIONAL, "Virement international"
    );

    private final RecapMoyenPaiementRepository repository;
    private final RecapMoyenPaiementMapper mapper;
    private final StaportPdfReportService staportPdfReportService;

    public RecapMoyenPaiementServiceImpl(
            RecapMoyenPaiementRepository repository,
            RecapMoyenPaiementMapper mapper,
            StaportPdfReportService staportPdfReportService) {
        this.repository = repository;
        this.mapper = mapper;
        this.staportPdfReportService = staportPdfReportService;
    }

    @Override
    public AgregatedRecapMoyenPaiementDto getEncaissements(Pageable pageable, RecapMoyenPaiementSearchDto searchDto) {
        return buildAgregatedRecap(pageable, searchDto, Type.ENCAISSEMENT);
    }

    @Override
    public AgregatedRecapMoyenPaiementDto getDecaissements(Pageable pageable, RecapMoyenPaiementSearchDto searchDto) {
        return buildAgregatedRecap(pageable, searchDto, Type.DECAISSEMENT);
    }

    private AgregatedRecapMoyenPaiementDto buildAgregatedRecap(
            Pageable pageable, RecapMoyenPaiementSearchDto searchDto, Type type) {

        Specification<RecapMoyenPaiementView> spec = RecapMoyenPaiementSpecification.searchByCriteria(searchDto, type);

        Page<RecapMoyenPaiementItem> page = repository.findAll(spec, pageable).map(mapper::toDto);
        BigDecimal total = repository.sumMontant(spec);

        return new AgregatedRecapMoyenPaiementDto(total, page);
    }

    @Override
    public byte[] exportEncaissementsPdf(RecapMoyenPaiementSearchDto searchDto) {
        return exportPdf(searchDto, Type.ENCAISSEMENT, "Recap Encaissement — Moyens de Paiement");
    }

    @Override
    public byte[] exportDecaissementsPdf(RecapMoyenPaiementSearchDto searchDto) {
        return exportPdf(searchDto, Type.DECAISSEMENT, "Recap Décaissement — Moyens de Paiement");
    }

    private byte[] exportPdf(RecapMoyenPaiementSearchDto searchDto, Type type, String title) {
        Specification<RecapMoyenPaiementView> spec = RecapMoyenPaiementSpecification.searchByCriteria(searchDto, type);

        List<RecapMoyenPaiementItem> items = repository.findAll(spec, Sort.by(Sort.Direction.DESC, "dateValeur")).stream()
                .map(mapper::toDto)
                .toList();

        BigDecimal total = repository.sumMontant(spec);

        List<String> filterLines = buildFilterLines(searchDto);

        try {
            return staportPdfReportService.generateReport(title, (doc, writer) -> {
                try {
                    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
                    Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.decode("#374151"));
                    Font hdrFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Color.WHITE);
                    Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.BLACK);
                    Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.decode("#1e3a8a"));

                    doc.add(Chunk.NEWLINE);
                    doc.add(Chunk.NEWLINE);

                    Paragraph titleParagraph = new Paragraph(title, titleFont);
                    titleParagraph.setAlignment(Element.ALIGN_CENTER);
                    doc.add(titleParagraph);

                    String exportInfo = "Exporté le " + LocalDate.now().format(DATE_FMT) + "  •  " + items.size() + " ligne(s)";
                    if (!filterLines.isEmpty()) {
                        exportInfo += "  •  Filtres : " + String.join("  |  ", filterLines);
                    }
                    Paragraph sub = new Paragraph(exportInfo, subFont);
                    sub.setAlignment(Element.ALIGN_CENTER);
                    sub.setSpacingAfter(8f);
                    doc.add(sub);

                    float[] colWidths = {12f, 10f, 8f, 12f, 10f, 9f, 9f, 9f, 8f, 13f};
                    PdfPTable table = new PdfPTable(colWidths);
                    table.setWidthPercentage(100f);
                    table.setSpacingBefore(8f);
                    table.setHeaderRows(1);

                    String tiersHeader = type == Type.ENCAISSEMENT ? "Émetteur" : "Bénéficiaire";
                    for (String h : new String[]{
                            "Moyen de paiement", "Banque", "Numéro", tiersHeader, "Montant (MAD)",
                            "Date opération", "Date valeur", "Date échéance", "Statut", "Libellé"}) {
                        PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                        c.setBackgroundColor(MED_BLUE);
                        c.setPadding(5f);
                        c.setHorizontalAlignment(Element.ALIGN_CENTER);
                        c.setBorderColor(Color.WHITE);
                        c.setBorderWidth(0.5f);
                        table.addCell(c);
                    }

                    for (int i = 0; i < items.size(); i++) {
                        RecapMoyenPaiementItem it = items.get(i);
                        Color rowBg = i % 2 == 0 ? Color.WHITE : ROW_ALT;
                        String tiers = type == Type.ENCAISSEMENT ? it.emetteur() : it.beneficiaire();

                        addCell(table, SOURCE_LABELS.getOrDefault(it.source(), it.source() != null ? it.source().name() : "—"), cellFont, rowBg, Element.ALIGN_LEFT);
                        addCell(table, it.banqueNom(), cellFont, rowBg, Element.ALIGN_LEFT);
                        addCell(table, it.numero(), cellFont, rowBg, Element.ALIGN_CENTER);
                        addCell(table, tiers, cellFont, rowBg, Element.ALIGN_LEFT);
                        addCell(table, MontantFormatter.format(it.montant()), cellFont, rowBg, Element.ALIGN_RIGHT);
                        addCell(table, it.dateOperation() != null ? it.dateOperation().format(DATE_FMT) : "—", cellFont, rowBg, Element.ALIGN_CENTER);
                        addCell(table, it.dateValeur() != null ? it.dateValeur().format(DATE_FMT) : "—", cellFont, rowBg, Element.ALIGN_CENTER);
                        addCell(table, it.dateEcheance() != null ? it.dateEcheance().format(DATE_FMT) : "—", cellFont, rowBg, Element.ALIGN_CENTER);
                        addCell(table, it.statut() != null ? it.statut().name() : "—", cellFont, rowBg, Element.ALIGN_CENTER);
                        addCell(table, it.libelle(), cellFont, rowBg, Element.ALIGN_LEFT);
                    }

                    PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL — " + items.size() + " ligne(s)", totalFont));
                    totalLabel.setColspan(4);
                    totalLabel.setBackgroundColor(LIGHT_BLUE);
                    totalLabel.setPadding(5f);
                    totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalLabel.setBorderColor(MED_BLUE);
                    table.addCell(totalLabel);

                    PdfPCell totalVal = new PdfPCell(new Phrase(MontantFormatter.format(total) + " MAD", totalFont));
                    totalVal.setBackgroundColor(LIGHT_BLUE);
                    totalVal.setPadding(5f);
                    totalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalVal.setBorderColor(MED_BLUE);
                    table.addCell(totalVal);

                    for (int j = 0; j < 5; j++) {
                        PdfPCell empty = new PdfPCell(new Phrase(""));
                        empty.setBackgroundColor(LIGHT_BLUE);
                        empty.setBorderColor(MED_BLUE);
                        table.addCell(empty);
                    }

                    doc.add(table);
                } catch (DocumentException e) {
                    throw new RuntimeException("Erreur lors de la génération du contenu du PDF du recap moyens de paiement.", e);
                }
            });
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF du recap moyens de paiement.", e);
        }
    }

    private List<String> buildFilterLines(RecapMoyenPaiementSearchDto searchDto) {
        List<String> filterLines = new ArrayList<>();
        if (searchDto == null) {
            return filterLines;
        }
        if (searchDto.banqueId() != null) {
            filterLines.add("Banque ID : " + searchDto.banqueId());
        }
        if (searchDto.sources() != null && !searchDto.sources().isEmpty()) {
            filterLines.add("Moyens de paiement : " + searchDto.sources());
        }
        if (searchDto.statuts() != null && !searchDto.statuts().isEmpty()) {
            filterLines.add("Statuts : " + searchDto.statuts());
        }
        if (searchDto.libelle() != null && !searchDto.libelle().isBlank()) {
            filterLines.add("Libellé : " + searchDto.libelle());
        }
        if (searchDto.numero() != null && !searchDto.numero().isBlank()) {
            filterLines.add("Numéro : " + searchDto.numero());
        }
        return filterLines;
    }

    private void addCell(PdfPTable table, String text, Font font, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", font));
        cell.setBackgroundColor(bg);
        cell.setPadding(4f);
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(BORDER_GRAY);
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
    }
}
