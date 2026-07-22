package staport.finance.gBanque.leasing.lesOrganismsLeasing.services;

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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingRequestDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingResponseDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingSearchDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.entities.OrganismeLeasing;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.mappers.OrganismeLeasingMapper;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.repositories.OrganismeLeasingRepository;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.services.interfaces.OrganismeLeasingService;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.specifications.OrganismeLeasingSpecification;
import staport.finance.gBanque.reports.service.StaportPdfReportService;

import java.awt.Color;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class OrganismeLeasingServiceImpl implements OrganismeLeasingService {

    private static final Logger log = LoggerFactory.getLogger(OrganismeLeasingServiceImpl.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color MED_BLUE = Color.decode("#2563eb");
    private static final Color ROW_ALT = Color.decode("#f8fafc");
    private static final Color BORDER_GRAY = Color.decode("#e5e7eb");

    private final OrganismeLeasingRepository repository;
    private final OrganismeLeasingMapper mapper;
    private final StaportPdfReportService staportPdfReportService;

    public OrganismeLeasingServiceImpl(OrganismeLeasingRepository repository, OrganismeLeasingMapper mapper,
                                        StaportPdfReportService staportPdfReportService) {
        this.repository = repository;
        this.mapper = mapper;
        this.staportPdfReportService = staportPdfReportService;
    }

    @AuditableAction(operation = AuditOperation.CREATE, entity = OrganismeLeasing.class)
    @Transactional
    @Override
    public OrganismeLeasingResponseDTO create(OrganismeLeasingRequestDTO dto) {
        OrganismeLeasing entity = mapper.toEntity(dto);
        OrganismeLeasing savedEntity = repository.save(entity);
        log.info("Organisme de leasing créé — ID : {}", savedEntity.getId());
        return mapper.toDto(savedEntity);
    }

    @AuditableAction(operation = AuditOperation.UPDATE, entity = OrganismeLeasing.class)
    @Transactional
    @Override
    public OrganismeLeasingResponseDTO update(Long id, OrganismeLeasingRequestDTO dto) {
        OrganismeLeasing existing = findOrThrow(id);
        mapper.updateOrganismeLeasingFromDto(dto, existing);
        OrganismeLeasing updatedEntity = repository.save(existing);
        log.info("Organisme de leasing mis à jour — ID : {}", id);
        return mapper.toDto(updatedEntity);
    }

    @Transactional(readOnly = true)
    @Override
    public OrganismeLeasingResponseDTO getById(Long id) {
        return mapper.toDto(findOrThrow(id));
    }

    @AuditableAction(operation = AuditOperation.DELETE, entity = OrganismeLeasing.class)
    @Transactional
    @Override
    public void delete(Long id) {
        OrganismeLeasing entity = findOrThrow(id);
        if (!entity.getContratsCadres().isEmpty()) {
            log.warn("Suppression bloquée — organisme ID : {} lié à des contrats cadres existants", id);
            throw new ValidationException(
                    "Impossible de supprimer l'organisme avec l'ID " + id +
                            " car il est lié à des contrats cadres existants.");
        }
        repository.delete(entity);
        log.info("Organisme de leasing supprimé — ID : {}", id);
    }

    @Transactional(readOnly = true)
    @Override
    public OrganismeLeasing findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("OrganismeLeasing non trouvé — ID : {}", id);
                    return new ResourceNotFoundException("OrganismeLeasing non trouvé avec l'ID: " + id);
                });
    }

    @Transactional(readOnly = true)
    @Override
    public Page<OrganismeLeasingResponseDTO> search(OrganismeLeasingSearchDTO criteria, Pageable pageable) {
        Specification<OrganismeLeasing> spec = OrganismeLeasingSpecification.searchByCriteria(criteria);
        Pageable effective = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));
        return repository.findAll(spec, effective).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    @Override
    public byte[] exportPdf(OrganismeLeasingSearchDTO criteria) {
        log.info("Export PDF des organismes de leasing — critères: {}", criteria);

        Specification<OrganismeLeasing> spec = OrganismeLeasingSpecification.searchByCriteria(criteria);
        List<OrganismeLeasing> organismes = repository.findAll(spec, Sort.by(Sort.Direction.ASC, "nom"));

        String title = "Organismes de Leasing";
        List<String> filterLines = (criteria != null && criteria.nom() != null && !criteria.nom().isBlank())
                ? List.of("Nom : " + criteria.nom())
                : List.of();

        try {
            return staportPdfReportService.generateReport(title, (doc, writer) -> {
                try {
                    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
                    Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.decode("#374151"));
                    Font hdrFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
                    Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);

                    doc.add(Chunk.NEWLINE);
                    doc.add(Chunk.NEWLINE);

                    Paragraph titleParagraph = new Paragraph(title, titleFont);
                    titleParagraph.setAlignment(Element.ALIGN_CENTER);
                    doc.add(titleParagraph);

                    String exportInfo = "Exporté le " + LocalDate.now().format(DATE_FMT)
                            + "  •  " + organismes.size() + " organisme(s)";
                    if (!filterLines.isEmpty()) {
                        exportInfo += "  •  Filtres : " + String.join("  |  ", filterLines);
                    }
                    Paragraph sub = new Paragraph(exportInfo, subFont);
                    sub.setAlignment(Element.ALIGN_CENTER);
                    sub.setSpacingAfter(8f);
                    doc.add(sub);

                    float[] colWidths = {50f, 25f, 25f};
                    PdfPTable table = new PdfPTable(colWidths);
                    table.setWidthPercentage(100f);
                    table.setSpacingBefore(8f);
                    table.setHeaderRows(1);

                    for (String h : new String[]{"Nom", "Date de création", "Contrats cadres"}) {
                        PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                        c.setBackgroundColor(MED_BLUE);
                        c.setPadding(5f);
                        c.setHorizontalAlignment(Element.ALIGN_CENTER);
                        c.setBorderColor(Color.WHITE);
                        c.setBorderWidth(0.5f);
                        table.addCell(c);
                    }

                    for (int i = 0; i < organismes.size(); i++) {
                        OrganismeLeasing o = organismes.get(i);
                        Color rowBg = i % 2 == 0 ? Color.WHITE : ROW_ALT;

                        addPdfCell(table, o.getNom(), cellFont, rowBg, BORDER_GRAY, Element.ALIGN_LEFT);
                        addPdfCell(table, o.getDateCreation() != null ? o.getDateCreation().format(DATE_FMT) : "—",
                                cellFont, rowBg, BORDER_GRAY, Element.ALIGN_CENTER);
                        addPdfCell(table, String.valueOf(o.getContratsCadres().size()),
                                cellFont, rowBg, BORDER_GRAY, Element.ALIGN_CENTER);
                    }

                    doc.add(table);
                } catch (DocumentException e) {
                    throw new RuntimeException("Erreur lors de la génération du contenu du PDF des organismes de leasing.", e);
                }
            });
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des organismes de leasing.", e);
        }
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
}
