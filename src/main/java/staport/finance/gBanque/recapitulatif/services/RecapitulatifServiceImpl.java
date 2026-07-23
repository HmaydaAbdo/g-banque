package staport.finance.gBanque.recapitulatif.services;

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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.dtos.response.BanqueResponse;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.recapitulatif.dtos.request.DecaissementSearchDto;
import staport.finance.gBanque.recapitulatif.dtos.response.AgregatedRecapDto;
import staport.finance.gBanque.recapitulatif.dtos.response.RecaputilatifGlobal;
import staport.finance.gBanque.recapitulatif.dtos.response.RecaputilatifItem;
import staport.finance.gBanque.recapitulatif.entities.RecapitulatifEncoursView;
import staport.finance.gBanque.recapitulatif.mappers.RecapitulatifEncoursMapper;
import staport.finance.gBanque.recapitulatif.repositories.RecapitulatifEncoursRepository;
import staport.finance.gBanque.recapitulatif.services.interfaces.IRecapitulatifService;
import staport.finance.gBanque.recapitulatif.specs.RecapitulatifEncoursSpecification;
import staport.finance.gBanque.reports.service.StaportPdfReportService;

import java.awt.Color;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Service pour générer un récapitulatif consolidé de tous les décaissements/encaissements
 * prévisionnels (échéances leasing/CMT, moyens de paiement, agios, commissions, etc.) pour
 * le mois en cours.
 *
 * <p>Interroge {@link RecapitulatifEncoursRepository}, adossé à la vue SQL
 * {@code v_recapitulatif_encours} — pagination, tri et agrégation réels en base, remplaçant
 * l'ancienne implémentation qui chargeait ~17 repositories en mémoire (voir historique dans
 * {@code modules_sweep.md}, sweep États des encours / Récapitulatif, Phase 2, 17/07/2026).</p>
 */
@Service
@Transactional(readOnly = true)
public class RecapitulatifServiceImpl implements IRecapitulatifService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color MED_BLUE = Color.decode("#2563eb");
    private static final Color LIGHT_BLUE = Color.decode("#dbeafe");
    private static final Color ROW_ALT = Color.decode("#f8fafc");
    private static final Color BORDER_GRAY = Color.decode("#e5e7eb");

    private final RecapitulatifEncoursRepository encoursRepository;
    private final RecapitulatifEncoursMapper mapper;
    private final IBanqueService banqueService;
    private final StaportPdfReportService staportPdfReportService;

    private final LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

    public RecapitulatifServiceImpl(
            RecapitulatifEncoursRepository encoursRepository,
            RecapitulatifEncoursMapper mapper,
            IBanqueService banqueService,
            StaportPdfReportService staportPdfReportService) {
        this.encoursRepository = encoursRepository;
        this.mapper = mapper;
        this.banqueService = banqueService;
        this.staportPdfReportService = staportPdfReportService;
    }

    @Override
    public AgregatedRecapDto getRecapitulatifEncaissementForCurrentMonthForGivenBanque(
            Pageable pageable, DecaissementSearchDto searchDto, Long banqueId) {
        return buildAgregatedRecap(pageable, searchDto, banqueId, Type.ENCAISSEMENT);
    }

    @Override
    public AgregatedRecapDto getRecapitulatifDecaissementForCurrentMonthForGivenBanque(
            Pageable pageable, DecaissementSearchDto searchDto, Long banqueId) {
        return buildAgregatedRecap(pageable, searchDto, banqueId, Type.DECAISSEMENT);
    }

    private AgregatedRecapDto buildAgregatedRecap(
            Pageable pageable, DecaissementSearchDto searchDto, Long banqueId, Type sens) {

        Specification<RecapitulatifEncoursView> spec =
                RecapitulatifEncoursSpecification.searchByCriteria(searchDto, banqueId, sens, endOfMonth);

        Page<RecaputilatifItem> page = encoursRepository.findAll(spec, normalizeSort(pageable))
                .map(mapper::toDto);

        BigDecimal total = encoursRepository.sumMontant(spec);
        BigDecimal totalDebit = sens == Type.DECAISSEMENT ? total : BigDecimal.ZERO;
        BigDecimal totalCredit = sens == Type.ENCAISSEMENT ? total : BigDecimal.ZERO;

        return new AgregatedRecapDto(totalDebit, totalCredit, page);
    }

    /**
     * Les colonnes triables côté Angular (recap-decaissement/recap-encaissements) incluent
     * "debit"/"credit"/"dateEcheance", qui n'existent pas comme propriétés distinctes sur
     * {@link RecapitulatifEncoursView} (une seule colonne "montant", direction déjà fixée par le
     * filtre "sens" ; "dateEcheance" est exposée sous "dateEcheanceAffichage"). On les retraduit
     * ici plutôt que de les exposer tel quel côté base pour ne pas changer le contrat frontend.
     */
    private Pageable normalizeSort(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }
        Sort normalized = Sort.by(pageable.getSort().stream()
                .map(order -> new Sort.Order(order.getDirection(), mapSortProperty(order.getProperty())))
                .toList());
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), normalized);
    }

    private String mapSortProperty(String property) {
        return switch (property) {
            case "debit", "credit" -> "montant";
            case "dateEcheance" -> "dateEcheanceAffichage";
            default -> property;
        };
    }

    @Override
    public byte[] exportRecapitulatifDecaissementPdf(DecaissementSearchDto searchDto, Long banqueId) {
        return exportRecapitulatifPdf(searchDto, banqueId, Type.DECAISSEMENT, "Récapitulatif des Décaissements Prévisionnels");
    }

    @Override
    public byte[] exportRecapitulatifEncaissementPdf(DecaissementSearchDto searchDto, Long banqueId) {
        return exportRecapitulatifPdf(searchDto, banqueId, Type.ENCAISSEMENT, "Récapitulatif des Encaissements Prévisionnels");
    }

    private byte[] exportRecapitulatifPdf(DecaissementSearchDto searchDto, Long banqueId, Type sens, String titlePrefix) {
        BanqueResponse banque = banqueService.getBanqueById(banqueId);

        Specification<RecapitulatifEncoursView> spec =
                RecapitulatifEncoursSpecification.searchByCriteria(searchDto, banqueId, sens, endOfMonth);

        List<RecaputilatifItem> items = encoursRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "dateEmission")).stream()
                .map(mapper::toDto)
                .toList();

        BigDecimal total = encoursRepository.sumMontant(spec);

        List<String> filterLines = new ArrayList<>();
        filterLines.add("Banque : " + banque.nom());
        if (searchDto != null) {
            if (searchDto.libelle() != null && !searchDto.libelle().isBlank()) {
                filterLines.add("Libellé : " + searchDto.libelle());
            }
            if (searchDto.observation() != null && !searchDto.observation().isBlank()) {
                filterLines.add("Observation : " + searchDto.observation());
            }
            if (searchDto.transactionSources() != null && !searchDto.transactionSources().isEmpty()) {
                filterLines.add("Source : " + searchDto.transactionSources());
            }
        }

        String title = titlePrefix + " — " + banque.nom();

        try {
            return staportPdfReportService.generateReport(title, (doc, writer) -> {
                try {
                    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
                    Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.decode("#374151"));
                    Font hdrFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
                    Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
                    Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.decode("#1e3a8a"));

                    doc.add(Chunk.NEWLINE);
                    doc.add(Chunk.NEWLINE);

                    Paragraph titleParagraph = new Paragraph(title, titleFont);
                    titleParagraph.setAlignment(Element.ALIGN_CENTER);
                    doc.add(titleParagraph);

                    String exportInfo = "Exporté le " + LocalDate.now().format(DATE_FMT)
                            + "  •  " + items.size() + " ligne(s)";
                    if (!filterLines.isEmpty()) {
                        exportInfo += "  •  Filtres : " + String.join("  |  ", filterLines);
                    }
                    Paragraph sub = new Paragraph(exportInfo, subFont);
                    sub.setAlignment(Element.ALIGN_CENTER);
                    sub.setSpacingAfter(8f);
                    doc.add(sub);

                    float[] colWidths = {8f, 12f, 12f, 22f, 11f, 11f, 12f, 12f};
                    PdfPTable table = new PdfPTable(colWidths);
                    table.setWidthPercentage(100f);
                    table.setSpacingBefore(8f);
                    table.setHeaderRows(1);

                    for (String h : new String[]{
                            "ID", "Date émission", "Date échéance", "Libellé",
                            "Débit (MAD)", "Crédit (MAD)", "Source", "Observations"}) {
                        PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                        c.setBackgroundColor(MED_BLUE);
                        c.setPadding(5f);
                        c.setHorizontalAlignment(Element.ALIGN_CENTER);
                        c.setBorderColor(Color.WHITE);
                        c.setBorderWidth(0.5f);
                        table.addCell(c);
                    }

                    for (int i = 0; i < items.size(); i++) {
                        RecaputilatifItem it = items.get(i);
                        Color rowBg = i % 2 == 0 ? Color.WHITE : ROW_ALT;

                        addRecapPdfCell(table, String.valueOf(it.id()), cellFont, rowBg, Element.ALIGN_CENTER);
                        addRecapPdfCell(table, it.dateEmission() != null ? it.dateEmission().format(DATE_FMT) : "—", cellFont, rowBg, Element.ALIGN_CENTER);
                        addRecapPdfCell(table, it.dateEcheance() != null ? it.dateEcheance().format(DATE_FMT) : "—", cellFont, rowBg, Element.ALIGN_CENTER);
                        addRecapPdfCell(table, it.libelle(), cellFont, rowBg, Element.ALIGN_LEFT);
                        addRecapPdfCell(table, formatMontantRecap(it.debit()), cellFont, rowBg, Element.ALIGN_RIGHT);
                        addRecapPdfCell(table, formatMontantRecap(it.credit()), cellFont, rowBg, Element.ALIGN_RIGHT);
                        addRecapPdfCell(table, it.source() != null ? it.source().name() : "—", cellFont, rowBg, Element.ALIGN_CENTER);
                        addRecapPdfCell(table, it.observations(), cellFont, rowBg, Element.ALIGN_LEFT);
                    }

                    PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL — " + items.size() + " ligne(s)", totalFont));
                    totalLabel.setColspan(sens == Type.DECAISSEMENT ? 4 : 5);
                    totalLabel.setBackgroundColor(LIGHT_BLUE);
                    totalLabel.setPadding(5f);
                    totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalLabel.setBorderColor(MED_BLUE);
                    table.addCell(totalLabel);

                    PdfPCell totalVal = new PdfPCell(new Phrase(formatMontantRecap(total) + " MAD", totalFont));
                    totalVal.setBackgroundColor(LIGHT_BLUE);
                    totalVal.setPadding(5f);
                    totalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalVal.setBorderColor(MED_BLUE);
                    table.addCell(totalVal);

                    int remainingCols = sens == Type.DECAISSEMENT ? 2 : 1;
                    for (int j = 0; j < remainingCols; j++) {
                        PdfPCell empty = new PdfPCell(new Phrase(""));
                        empty.setBackgroundColor(LIGHT_BLUE);
                        empty.setBorderColor(MED_BLUE);
                        table.addCell(empty);
                    }

                    doc.add(table);
                } catch (DocumentException e) {
                    throw new RuntimeException("Erreur lors de la génération du contenu du PDF du récapitulatif.", e);
                }
            });
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF du récapitulatif.", e);
        }
    }

    private void addRecapPdfCell(PdfPTable table, String text, Font font, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", font));
        cell.setBackgroundColor(bg);
        cell.setPadding(4f);
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(BORDER_GRAY);
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
    }

    private String formatMontantRecap(BigDecimal montant) {
        if (montant == null) return "—";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        return formatter.format(montant);
    }

    @Override
    public RecaputilatifGlobal getRecapitulatifGlobal(Long banqueId) {
        BanqueResponse banque = this.banqueService.getBanqueById(banqueId);
        BigDecimal plafondFc = banqueService.getPlafondFC(banqueId);

        List<RecaputilatifItem> decaissements = fetchOutstanding(banqueId, Type.DECAISSEMENT);
        List<RecaputilatifItem> encaissements = fetchOutstanding(banqueId, Type.ENCAISSEMENT);

        return new RecaputilatifGlobal(
                banque.id(),
                banque.nom(),
                banque.soldeActuel(),
                plafondFc.negate(),
                encaissements,
                decaissements
        );
    }

    private List<RecaputilatifItem> fetchOutstanding(Long banqueId, Type sens) {
        Specification<RecapitulatifEncoursView> spec = Specification
                .where(RecapitulatifEncoursSpecification.byBanqueId(banqueId))
                .and(RecapitulatifEncoursSpecification.bySens(sens))
                .and(RecapitulatifEncoursSpecification.outstandingUntil(endOfMonth));

        return encoursRepository.findAll(spec).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public List<RecaputilatifGlobal> getAllRecapitulatifsGlobaux() {
        List<Long> allBankIds = this.banqueService.getAllBanqueIds();

        return allBankIds.stream()
                .map(this::getRecapitulatifGlobal)
                .collect(Collectors.toList());
    }
}
