package staport.finance.gBanque.leasing.contratCadre.services;

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
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.leasing.contratCadre.dtos.ContratCadreRequestDTO;
import staport.finance.gBanque.leasing.contratCadre.dtos.ContratCadreResponseDTO;
import staport.finance.gBanque.leasing.contratCadre.dtos.ContratCadreSearchDTO;
import staport.finance.gBanque.leasing.contratCadre.dtos.ContratCadreUpdateDTO;
import staport.finance.gBanque.leasing.contratCadre.entities.ContratCadre;
import staport.finance.gBanque.leasing.contratCadre.enums.StatutContratCadre;
import staport.finance.gBanque.leasing.contratCadre.mappers.ContratCadreMapper;
import staport.finance.gBanque.leasing.contratCadre.repositories.ContratCadreRepository;
import staport.finance.gBanque.leasing.contratCadre.specifications.ContratCadreSpecification;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.entities.OrganismeLeasing;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.services.interfaces.OrganismeLeasingService;
import staport.finance.gBanque.reports.service.StaportPdfReportService;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class ContratCadreServiceImpl implements IContratCadreService {

    private static final Logger log = LoggerFactory.getLogger(ContratCadreServiceImpl.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color MED_BLUE = Color.decode("#2563eb");
    private static final Color LIGHT_BLUE = Color.decode("#dbeafe");
    private static final Color ROW_ALT = Color.decode("#f8fafc");
    private static final Color GREEN = Color.decode("#16a34a");
    private static final Color ORANGE = Color.decode("#ea580c");
    private static final Color GRAY = Color.decode("#6b7280");
    private static final Color BORDER_GRAY = Color.decode("#e5e7eb");

    private final ContratCadreRepository repository;
    private final ContratCadreMapper mapper;
    private final OrganismeLeasingService organismeLeasingService;
    private final StaportPdfReportService staportPdfReportService;

    public ContratCadreServiceImpl(ContratCadreRepository repository,
                                   ContratCadreMapper mapper,
                                   OrganismeLeasingService organismeLeasingService,
                                   StaportPdfReportService staportPdfReportService) {
        this.repository             = repository;
        this.mapper                 = mapper;
        this.organismeLeasingService = organismeLeasingService;
        this.staportPdfReportService = staportPdfReportService;
    }

    @AuditableAction(operation = AuditOperation.CREATE, entity = ContratCadre.class)
    @Transactional
    @Override
    public ContratCadreResponseDTO create(ContratCadreRequestDTO dto) {
        if (repository.existsByOrganisme_IdAndStatut(dto.organismeId(), StatutContratCadre.ACTIF)) {
            throw new ValidationException(
                    "L'organisme ID " + dto.organismeId() + " possède déjà un contrat cadre ACTIF. " +
                    "Archivez-le avant d'en créer un nouveau.");
        }
        OrganismeLeasing organisme = organismeLeasingService.findOrThrow(dto.organismeId());
        ContratCadre entity = mapper.toEntity(dto);
        entity.setOrganisme(organisme);
        entity.setStatut(StatutContratCadre.ACTIF);
        ContratCadre saved = repository.save(entity);
        log.info("Contrat cadre créé — ID : {}, organisme : {}", saved.getId(), organisme.getNom());
        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ContratCadreResponseDTO> search(ContratCadreSearchDTO criteria, Pageable pageable) {
        Specification<ContratCadre> spec = ContratCadreSpecification.searchByCriteria(criteria);
        return repository.findAll(spec, pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ContratCadreResponseDTO> findByOrganisme(Long organismeId) {
        return repository.findByOrganisme_IdOrderByDateDebutDesc(organismeId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public ContratCadreResponseDTO getById(Long id) {
        return mapper.toDto(findOrThrow(id));
    }

    @AuditableAction(operation = AuditOperation.UPDATE, entity = ContratCadre.class)
    @Transactional
    @Override
    public ContratCadreResponseDTO update(Long id, ContratCadreUpdateDTO dto) {
        ContratCadre entity = findOrThrow(id);
        mapper.updateEntity(dto, entity);
        ContratCadre saved = repository.save(entity);
        log.info("Contrat cadre mis à jour — ID : {}", id);
        return mapper.toDto(saved);
    }

    @AuditableAction(operation = AuditOperation.UPDATE, entity = ContratCadre.class)
    @Transactional
    @Override
    public ContratCadreResponseDTO archiver(Long id) {
        ContratCadre entity = findOrThrow(id);
        if (entity.getStatut() != StatutContratCadre.ACTIF) {
            throw new ValidationException(
                    "Seul un contrat cadre ACTIF peut être archivé. Statut actuel : " + entity.getStatut());
        }
        entity.setStatut(StatutContratCadre.ARCHIVE);
        ContratCadre saved = repository.save(entity);
        log.info("Contrat cadre archivé — ID : {}", id);
        return mapper.toDto(saved);
    }

    @Transactional
    @Override
    public ContratCadre resolveActif(Long organismeId) {
        ContratCadre cadre = repository
                .findByOrganisme_IdAndStatut(organismeId, StatutContratCadre.ACTIF)
                .orElseThrow(() -> {
                    log.warn("Aucun contrat cadre ACTIF pour l'organisme ID : {}", organismeId);
                    return new ValidationException(
                            "Aucun contrat cadre actif pour l'organisme ID " + organismeId +
                            ". Veuillez en créer un avant de signer un contrat leasing.");
                });

        // Expiration lazy : si la date de fin est dépassée, on passe en EXPIRE et on rejette
        if (cadre.getDateFin().isBefore(LocalDate.now())) {
            cadre.setStatut(StatutContratCadre.EXPIRE);
            repository.save(cadre);
            log.warn("Contrat cadre ID {} expiré (dateFin={}) — statut mis à jour en EXPIRE",
                    cadre.getId(), cadre.getDateFin());
            throw new ValidationException(
                    "Le contrat cadre de l'organisme ID " + organismeId +
                    " est expiré depuis le " + cadre.getDateFin() +
                    ". Veuillez en créer un nouveau contrat cadre.");
        }

        return cadre;
    }

    // --- Méthode interne ---
    private ContratCadre findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("ContratCadre non trouvé — ID : {}", id);
                    return new ResourceNotFoundException("Contrat cadre non trouvé avec l'ID : " + id);
                });
    }

    // ── Export PDF ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Override
    public byte[] exportPdf(ContratCadreSearchDTO criteria) {
        log.info("Export PDF des contrats cadres — critères: {}", criteria);

        Specification<ContratCadre> spec = ContratCadreSpecification.searchByCriteria(criteria);
        List<ContratCadre> contrats = repository.findAll(spec, Sort.by(Sort.Direction.DESC, "dateDebut"));

        BigDecimal totalPlafond = contrats.stream()
                .map(ContratCadre::getPlafond)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        String title = "Contrats Cadres de Leasing";
        List<String> filterLines = buildFilterSummary(criteria);

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
                            + "  •  " + contrats.size() + " contrat(s) cadre(s)";
                    if (!filterLines.isEmpty()) {
                        exportInfo += "  •  Filtres : " + String.join("  |  ", filterLines);
                    }
                    Paragraph sub = new Paragraph(exportInfo, subFont);
                    sub.setAlignment(Element.ALIGN_CENTER);
                    sub.setSpacingAfter(8f);
                    doc.add(sub);

                    float[] colWidths = {25f, 15f, 15f, 15f, 15f};
                    PdfPTable table = new PdfPTable(colWidths);
                    table.setWidthPercentage(100f);
                    table.setSpacingBefore(8f);
                    table.setHeaderRows(1);

                    for (String h : new String[]{"Organisme", "Plafond (MAD)", "Date Début", "Date Fin", "Statut"}) {
                        PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                        c.setBackgroundColor(MED_BLUE);
                        c.setPadding(5f);
                        c.setHorizontalAlignment(Element.ALIGN_CENTER);
                        c.setBorderColor(Color.WHITE);
                        c.setBorderWidth(0.5f);
                        table.addCell(c);
                    }

                    for (int i = 0; i < contrats.size(); i++) {
                        ContratCadre c = contrats.get(i);
                        Color rowBg = i % 2 == 0 ? Color.WHITE : ROW_ALT;

                        addPdfCell(table, c.getOrganisme() != null ? c.getOrganisme().getNom() : "—", cellFont, rowBg, BORDER_GRAY, Element.ALIGN_LEFT);
                        addPdfCell(table, formatMontantPdf(c.getPlafond()), cellFont, rowBg, BORDER_GRAY, Element.ALIGN_RIGHT);
                        addPdfCell(table, c.getDateDebut() != null ? c.getDateDebut().format(DATE_FMT) : "—", cellFont, rowBg, BORDER_GRAY, Element.ALIGN_CENTER);
                        addPdfCell(table, c.getDateFin() != null ? c.getDateFin().format(DATE_FMT) : "—", cellFont, rowBg, BORDER_GRAY, Element.ALIGN_CENTER);

                        Color statusColor = switch (c.getStatut()) {
                            case ACTIF -> GREEN;
                            case ARCHIVE -> GRAY;
                            case EXPIRE -> ORANGE;
                        };
                        PdfPCell statusCell = new PdfPCell(new Phrase(
                                c.getStatut() != null ? c.getStatut().name() : "—",
                                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
                        statusCell.setBackgroundColor(statusColor);
                        statusCell.setPadding(4f);
                        statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        statusCell.setBorderColor(BORDER_GRAY);
                        statusCell.setBorderWidth(0.5f);
                        table.addCell(statusCell);
                    }

                    PdfPCell totalLabel = new PdfPCell(
                            new Phrase("TOTAL — " + contrats.size() + " contrat(s) cadre(s)", totalFont));
                    totalLabel.setBackgroundColor(LIGHT_BLUE);
                    totalLabel.setPadding(5f);
                    totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalLabel.setBorderColor(MED_BLUE);
                    table.addCell(totalLabel);

                    PdfPCell totalVal = new PdfPCell(
                            new Phrase(formatMontantPdf(totalPlafond) + " MAD", totalFont));
                    totalVal.setBackgroundColor(LIGHT_BLUE);
                    totalVal.setPadding(5f);
                    totalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalVal.setBorderColor(MED_BLUE);
                    table.addCell(totalVal);

                    for (int j = 0; j < 3; j++) {
                        PdfPCell empty = new PdfPCell(new Phrase(""));
                        empty.setBackgroundColor(LIGHT_BLUE);
                        empty.setBorderColor(MED_BLUE);
                        table.addCell(empty);
                    }

                    doc.add(table);
                } catch (DocumentException e) {
                    throw new RuntimeException("Erreur lors de la génération du contenu du PDF des contrats cadres.", e);
                }
            });
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des contrats cadres.", e);
        }
    }

    private List<String> buildFilterSummary(ContratCadreSearchDTO criteria) {
        if (criteria == null) return List.of();
        List<String> lines = new java.util.ArrayList<>();
        if (criteria.organismeId() != null) {
            try {
                lines.add("Organisme : " + organismeLeasingService.findOrThrow(criteria.organismeId()).getNom());
            } catch (ResourceNotFoundException e) {
                log.debug("Organisme id={} introuvable pour le résumé de filtre export — ignoré", criteria.organismeId());
            }
        }
        if (criteria.statut() != null) {
            lines.add("Statut : " + criteria.statut().name());
        }
        return lines;
    }

    private void addPdfCell(PdfPTable table, String text, Font font, Color bg, Color border, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", font));
        cell.setBackgroundColor(bg);
        cell.setPadding(4f);
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(border);
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
    }

    private String formatMontantPdf(BigDecimal montant) {
        if (montant == null) return "—";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        return formatter.format(montant);
    }
}
