package staport.finance.gBanque.Agios.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Agios.dtos.AgiosRequestDto;
import staport.finance.gBanque.Agios.dtos.AgiosResponseDto;
import staport.finance.gBanque.Agios.dtos.AgiosSearchDto;
import staport.finance.gBanque.Agios.entities.Agios;
import staport.finance.gBanque.Agios.exceptions.AgiosBusinessException;
import staport.finance.gBanque.Agios.exceptions.AgiosNotFoundException;
import staport.finance.gBanque.Agios.mapper.AgiosMapper;
import staport.finance.gBanque.Agios.repository.AgiosRepository;
import staport.finance.gBanque.Agios.spec.AgiosSpec;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.reports.service.StaportPdfReportService;

import java.awt.Color;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class AgiosService implements IAgiosService {

    private static final Logger log = LoggerFactory.getLogger(AgiosService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color MED_BLUE = Color.decode("#2563eb");
    private static final Color LIGHT_BLUE = Color.decode("#dbeafe");
    private static final Color ROW_ALT = Color.decode("#f8fafc");
    private static final Color BORDER_GRAY = Color.decode("#e5e7eb");

    private final AgiosRepository agiosRepository;
    private final IBanqueService banqueService;
    private final AgiosMapper agiosMapper;
    private final StaportPdfReportService staportPdfReportService;

    public AgiosService(AgiosRepository agiosRepository,
                        IBanqueService banqueService,
                        AgiosMapper agiosMapper,
                        StaportPdfReportService staportPdfReportService) {
        this.agiosRepository = agiosRepository;
        this.banqueService   = banqueService;
        this.agiosMapper     = agiosMapper;
        this.staportPdfReportService = staportPdfReportService;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsChargeFixeGeneree(Long banqueId, String libelle, LocalDate debut, LocalDate fin) {
        return agiosRepository.existsByBanqueIdAndLibelleAndDateEmissionBetween(banqueId, libelle, debut, fin);
    }

    // ── Création ──────────────────────────────────────────────────────────────

    @Override
    @AuditableAction(operation = AuditOperation.CREATE, entity = Agios.class)
    @Transactional
    public AgiosResponseDto createAgios(AgiosRequestDto dto) {
        log.info("Création d'un agios — banqueId: {}, montant: {}", dto.banqueId(), dto.montant());

        Banque banque = findBanqueOuLeverException(dto.banqueId());

        Agios agios = new Agios();
        agios.setLibelle(dto.libelle());
        agios.setObservation(dto.observation());
        agios.setMontant(dto.montant());
        agios.setBanque(banque);
        agios.setDateEmission(dto.dateEmission());

        Agios saved = agiosRepository.save(agios);
        log.info("Agios créé — id: {}, statut: {}", saved.getId(), saved.getStatus());
        return agiosMapper.toResponseDto(saved);
    }

    // ── Lecture ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AgiosResponseDto getById(Long id) {
        log.info("Récupération de l'agios — id: {}", id);
        return agiosMapper.toResponseDto(findAgiosOuLeverException(id));
    }

    @Transactional(readOnly = true)
    public Page<AgiosResponseDto> searchAgios(AgiosSearchDto dto, Pageable pageable) {
        log.info("Recherche d'agios — page: {}, taille: {}", pageable.getPageNumber(), pageable.getPageSize());
        return agiosRepository.findAll(AgiosSpec.searchByCriteria(dto), pageable)
                .map(agiosMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public byte[] exportAgiosPdf(AgiosSearchDto dto) {
        log.info("Export PDF des agios ponctuels - Critères: {}", dto);

        List<AgiosResponseDto> agios = agiosRepository
                .findAll(AgiosSpec.searchByCriteria(dto), Sort.by(Sort.Direction.DESC, "dateEmission"))
                .stream()
                .map(agiosMapper::toResponseDto)
                .toList();

        return generateAgiosListPdf("Agios", agios, buildFilterSummary(dto), null);
    }

    // ── Modification ──────────────────────────────────────────────────────────

    @AuditableAction(operation = AuditOperation.UPDATE, entity = Agios.class)
    @Transactional
    public AgiosResponseDto updateAgios(Long id, AgiosRequestDto dto) {
        log.info("Mise à jour de l'agios — id: {}", id);

        Agios agios  = findAgiosOuLeverException(id);
        Banque banque = findBanqueOuLeverException(dto.banqueId());

        agios.setLibelle(dto.libelle());
        agios.setMontant(dto.montant());
        agios.setBanque(banque);
        agios.setDateEmission(dto.dateEmission());
        agios.setObservation(dto.observation());

        Agios updated = agiosRepository.save(agios);
        log.info("Agios mis à jour — id: {}", updated.getId());
        return agiosMapper.toResponseDto(updated);
    }

    // ── Suppression ───────────────────────────────────────────────────────────

    @AuditableAction(operation = AuditOperation.DELETE, entity = Agios.class)
    @Transactional
    public void deleteAgios(Long id) {
        log.info("Suppression de l'agios — id: {}", id);
        Agios agios = findAgiosOuLeverException(id);

        if (agios.getStatus() != Status.PREVU) {
            log.warn("Tentative de suppression d'un agios déjà réglé — id: {}", id);
            throw new AgiosBusinessException("L'agios id: " + id + " est déjà réglé et ne peut plus être supprimé.");
        }

        agiosRepository.deleteById(id);
        log.info("Agios supprimé — id: {}", id);
    }

    // ── Toggle statut ─────────────────────────────────────────────────────────

    @AuditableAction(operation = AuditOperation.UPDATE, entity = Agios.class)
    @Transactional
    public AgiosResponseDto toggleStatus(Long id) {
        log.info("Basculement du statut de l'agios — id: {}", id);

        Agios agios     = findAgiosOuLeverException(id);
        Status newStatus = agios.getStatus() == Status.PAYE ? Status.PREVU : Status.PAYE;
        agios.setStatus(newStatus);

        Agios updated = agiosRepository.save(agios);
        log.info("Statut basculé — id: {}, nouveau statut: {}", id, newStatus);
        return agiosMapper.toResponseDto(updated);
    }

    // ── Méthodes privées ──────────────────────────────────────────────────────

    private Agios findAgiosOuLeverException(Long id) {
        return agiosRepository.findById(id)
                .orElseThrow(() -> new AgiosNotFoundException(id));
    }

    private Banque findBanqueOuLeverException(Long banqueId) {
        return banqueService.findOrThrow(banqueId);
    }

    // ── Export PDF ───────────────────────────────────────────────────────────

    /**
     * Génère le PDF partagé par la liste d'agios ponctuels et l'encours d'une banque — mêmes
     * colonnes que l'écran (Libellé, Banque, Montant, Date d'émission, Statut), plus un résumé des
     * filtres appliqués et le montant total.
     *
     * @param scopeLabel {@code "encours"} pour libeller le total en conséquence, {@code null} sinon
     */
    private byte[] generateAgiosListPdf(String title, List<AgiosResponseDto> agios,
                                        List<String> filterLines, String scopeLabel) {
        BigDecimal totalMontant = agios.stream()
                .map(AgiosResponseDto::montant)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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
                            + "  •  " + agios.size() + " agios";
                    if (!filterLines.isEmpty()) {
                        exportInfo += "  •  Filtres : " + String.join("  |  ", filterLines);
                    }
                    Paragraph sub = new Paragraph(exportInfo, subFont);
                    sub.setAlignment(Element.ALIGN_CENTER);
                    sub.setSpacingAfter(8f);
                    doc.add(sub);

                    float[] colWidths = {26f, 20f, 16f, 16f, 14f};
                    PdfPTable table = new PdfPTable(colWidths);
                    table.setWidthPercentage(100f);
                    table.setSpacingBefore(8f);
                    table.setHeaderRows(1);

                    for (String h : new String[]{"Libellé", "Banque", "Montant (MAD)", "Date d'émission", "Statut"}) {
                        PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                        c.setBackgroundColor(MED_BLUE);
                        c.setPadding(5f);
                        c.setHorizontalAlignment(Element.ALIGN_CENTER);
                        c.setBorderColor(Color.WHITE);
                        c.setBorderWidth(0.5f);
                        table.addCell(c);
                    }

                    for (int i = 0; i < agios.size(); i++) {
                        AgiosResponseDto a = agios.get(i);
                        Color rowBg = i % 2 == 0 ? Color.WHITE : ROW_ALT;

                        addAgiosPdfCell(table, a.libelle(), cellFont, rowBg, Element.ALIGN_LEFT);
                        addAgiosPdfCell(table, a.banqueNom(), cellFont, rowBg, Element.ALIGN_LEFT);
                        addAgiosPdfCell(table, formatMontantAgios(a.montant()), cellFont, rowBg, Element.ALIGN_RIGHT);
                        addAgiosPdfCell(table, a.dateEmission() != null ? a.dateEmission().format(DATE_FMT) : "—", cellFont, rowBg, Element.ALIGN_CENTER);

                        PdfPCell statusCell = new PdfPCell(new Phrase(
                                a.status() != null ? a.status().name() : "—",
                                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
                        statusCell.setBackgroundColor(a.status() == Status.PAYE ? Color.decode("#16a34a") : Color.decode("#ea580c"));
                        statusCell.setPadding(4f);
                        statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        statusCell.setBorderColor(BORDER_GRAY);
                        statusCell.setBorderWidth(0.5f);
                        table.addCell(statusCell);
                    }

                    String totalLabelText = "encours".equals(scopeLabel)
                            ? "TOTAL DES ENCOURS — " + agios.size() + " agios"
                            : "TOTAL — " + agios.size() + " agios";

                    PdfPCell totalLabel = new PdfPCell(new Phrase(totalLabelText, totalFont));
                    totalLabel.setColspan(2);
                    totalLabel.setBackgroundColor(LIGHT_BLUE);
                    totalLabel.setPadding(5f);
                    totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalLabel.setBorderColor(MED_BLUE);
                    table.addCell(totalLabel);

                    PdfPCell totalVal = new PdfPCell(new Phrase(formatMontantAgios(totalMontant) + " MAD", totalFont));
                    totalVal.setBackgroundColor(LIGHT_BLUE);
                    totalVal.setPadding(5f);
                    totalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalVal.setBorderColor(MED_BLUE);
                    table.addCell(totalVal);

                    for (int j = 0; j < 2; j++) {
                        PdfPCell empty = new PdfPCell(new Phrase(""));
                        empty.setBackgroundColor(LIGHT_BLUE);
                        empty.setBorderColor(MED_BLUE);
                        table.addCell(empty);
                    }

                    doc.add(table);
                } catch (DocumentException e) {
                    throw new RuntimeException("Erreur lors de la génération du contenu du PDF des agios.", e);
                }
            });
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des agios.", e);
        }
    }

    private List<String> buildFilterSummary(AgiosSearchDto dto) {
        List<String> lines = new ArrayList<>();
        if (dto == null) return lines;

        if (dto.libelle() != null && !dto.libelle().isBlank()) {
            lines.add("Libellé : " + dto.libelle());
        }
        if (dto.banqueId() != null) {
            try {
                lines.add("Banque : " + banqueService.findOrThrow(dto.banqueId()).getNom());
            } catch (ResourceNotFoundException e) {
                log.debug("Banque id={} introuvable pour le résumé de filtre export — ignorée", dto.banqueId());
            }
        }
        if (dto.status() != null) {
            lines.add("Statut : " + dto.status().name());
        }
        if (dto.montant() != null) {
            lines.add("Montant : " + formatMontantAgios(dto.montant()));
        }
        if (dto.dateEmissionDebut() != null) {
            lines.add("Date d'émission min : " + dto.dateEmissionDebut().format(DATE_FMT));
        }
        if (dto.dateEmissionFin() != null) {
            lines.add("Date d'émission max : " + dto.dateEmissionFin().format(DATE_FMT));
        }
        if (dto.dateOperationDebut() != null) {
            lines.add("Date d'opération min : " + dto.dateOperationDebut().format(DATE_FMT));
        }
        if (dto.dateOperationFin() != null) {
            lines.add("Date d'opération max : " + dto.dateOperationFin().format(DATE_FMT));
        }
        if (dto.dateValeurDebut() != null) {
            lines.add("Date de valeur min : " + dto.dateValeurDebut().format(DATE_FMT));
        }
        if (dto.dateValeurFin() != null) {
            lines.add("Date de valeur max : " + dto.dateValeurFin().format(DATE_FMT));
        }
        return lines;
    }

    private void addAgiosPdfCell(PdfPTable table, String text, Font font, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", font));
        cell.setBackgroundColor(bg);
        cell.setPadding(4f);
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(BORDER_GRAY);
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
    }

    private String formatMontantAgios(BigDecimal montant) {
        if (montant == null) return "—";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        return formatter.format(montant);
    }
}
