package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services;

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
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleRequest;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleResponse;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleSearchCriteria;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.enums.PorteeNature;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.exceptions.NatureLFDeletionException;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.mappers.NatureLigneFonctionnelleMapper;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories.NatureLigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.specifications.NatureLigneFonctionnelleSpecification;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.services.ILigneFonctionnelleService;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.services.ILigneFonctionnelleSpecifiqueService;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.reports.service.StaportPdfReportService;

import java.awt.Color;
import java.util.List;
import java.util.Optional;

/**
 * Business logic implementation for the NatureLigneFonctionnelle module.
 *
 * The controller injects {@link INatureLigneFonctionnelleService} (DIP).
 * Audit passe par {@code @Audited} (entité) + {@code @AuditableAction} (méthodes) —
 * {@code GenericAuditAspect}, comme le reste des modules déjà migrés (Bank, Leasing, CMT).
 */
@Service
@Transactional(readOnly = true)
public class NatureLigneFonctionnelleService implements INatureLigneFonctionnelleService {

    private static final Logger log = LoggerFactory.getLogger(NatureLigneFonctionnelleService.class);

    /**
     * Codes système des natures de ligne fonctionnelle nécessitant un traitement métier spécifique
     * (calcul d'encours/solde, association automatique). Le {@code code} est immuable (voir
     * {@link #updateNatureLigneFonctionnelle}) — contrairement à l'ID technique, il peut être
     * référencé en toute sécurité dans le code métier.
     */
    public static final String FC_CODE = "FC";
    public static final String ESC_COM_CODE = "ESC_COM";
    public static final String PREFINANCEMENT_CODE = "PREFINANCEMENT";
    public static final String DROIT_CONSTATE_CODE = "DROIT_CONSTATE";
    public static final String FACTORING_CODE = "FACTORING";
    public static final String CAUT_ADMIN_CODE = "CAUT_ADMIN";

    private final NatureLigneFonctionnelleRepository repository;
    private final NatureLigneFonctionnelleMapper mapper;
    private final ILigneFonctionnelleService ligneFonctionnelleService;
    private final ILigneFonctionnelleSpecifiqueService ligneFonctionnelleSpecifiqueService;
    private final StaportPdfReportService staportPdfReportService;

    public NatureLigneFonctionnelleService(
            NatureLigneFonctionnelleRepository repository,
            NatureLigneFonctionnelleMapper mapper,
            ILigneFonctionnelleService ligneFonctionnelleService,
            ILigneFonctionnelleSpecifiqueService ligneFonctionnelleSpecifiqueService,
            StaportPdfReportService staportPdfReportService) {
        this.repository                   = repository;
        this.mapper                       = mapper;
        this.ligneFonctionnelleService = ligneFonctionnelleService;
        this.ligneFonctionnelleSpecifiqueService = ligneFonctionnelleSpecifiqueService;
        this.staportPdfReportService = staportPdfReportService;
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = NatureLigneFonctionnelle.class)
    public NatureLigneFonctionnelleResponse createNatureLigneFonctionnelle(NatureLigneFonctionnelleRequest request) {
        log.info("Création NatureLigneFonctionnelle — label: {}, code: {}", request.label(), request.code());

        if (repository.existsByLabel(request.label())) {
            throw new DuplicateEntryException("Le label '" + request.label() + "' est déjà utilisé.");
        }

        if (repository.existsByCode(request.code())) {
            throw new DuplicateEntryException("Le code '" + request.code() + "' est déjà utilisé.");
        }

        NatureLigneFonctionnelle saved = repository.save(mapper.toEntity(request));
        return mapper.toDto(saved);
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @Override
    public NatureLigneFonctionnelleResponse getNatureLigneFonctionnelleById(Long id) {
        return mapper.toDto(findOrThrow(id));
    }

    @Override
    public Page<NatureLigneFonctionnelleResponse> getAllNatureLigneFonctionnelles(
            NatureLigneFonctionnelleSearchCriteria criteria, Pageable pageable) {

        Specification<NatureLigneFonctionnelle> spec =
                NatureLigneFonctionnelleSpecification.searchByCriteria(criteria);
        return repository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"))).map(mapper::toDto);
    }

    @Override
    public List<NatureLigneFonctionnelleResponse> getAllNatureLigneFonctionnellesList(PorteeNature portee) {

        if (portee == null) {
            return mapper.toDtoList(repository.findAll());
        }
        List<PorteeNature> porteesFiltrees = switch (portee) {
            case GENERALE   -> List.of(PorteeNature.GENERALE,   PorteeNature.GENERALE_ET_SPECIFIQUE);
            case SPECIFIQUE -> List.of(PorteeNature.SPECIFIQUE, PorteeNature.GENERALE_ET_SPECIFIQUE);
            case GENERALE_ET_SPECIFIQUE -> List.of(PorteeNature.values()); // cas improbable depuis l'UI
        };
        return mapper.toDtoList(repository.findByPorteeIn(porteesFiltrees));
    }

    // ── Update ───────────────────────────────────────────────────────────────

    /**
     * Updates label and description only. {@code code} is immutable — the mapper
     * intentionally ignores any code value present in the request.
     */
    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = NatureLigneFonctionnelle.class)
    public NatureLigneFonctionnelleResponse updateNatureLigneFonctionnelle(Long id, NatureLigneFonctionnelleRequest request) {
        log.info("Mise à jour NatureLigneFonctionnelle — id: {}", id);
        NatureLigneFonctionnelle existing = findOrThrow(id);

        // Business Logic: Check label uniqueness only if the label has actually changed
        if (!existing.getLabel().equals(request.label()) && repository.existsByLabel(request.label())) {
            throw new DuplicateEntryException("Le nouveau label est déjà utilisé par une autre nature.");
        }

        // Explicit updates (code est immuable, portee est modifiable)
        existing.setLabel(request.label());
        existing.setDescription(request.description());
        existing.setPortee(request.portee());

        return mapper.toDto(existing);
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = NatureLigneFonctionnelle.class)
    public void deleteNatureLigneFonctionnelle(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("ID introuvable: " + id);
        }

        if (ligneFonctionnelleService.existsByNatureId(id) || ligneFonctionnelleSpecifiqueService.existsByNatureId(id)) {
            throw new NatureLFDeletionException(
                    "Impossible de supprimer cette nature : elle est référencée par au moins une ligne fonctionnelle (générale ou spécifique).");
        }

        log.info("Suppression NatureLigneFonctionnelle — id: {}", id);
        repository.deleteById(id);
    }


    @Override
    public Long findFaciliteDeCaisseNatureId() {
        return repository.findByCode(FC_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("Code système FC introuvable."))
                .getId();
    }

    @Override
    @Transactional(readOnly = true)
    public NatureLigneFonctionnelle findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nature de ligne fonctionnelle non trouvée avec l'ID : " + id));
    }


    @Override
    public Optional<NatureLigneFonctionnelle> findByCode(String code) {
        return repository.findByCode(code);
    }

    @Override
    public Optional<NatureLigneFonctionnelle> findByLabel(String label) {
        return repository.findByLabel(label);
    }

    // ── Export PDF ───────────────────────────────────────────────────────────

    /**
     * Exporte en PDF l'intégralité des natures correspondant aux filtres fournis (mêmes critères
     * que {@code GET /natures-lignes-fonctionnelles}) — sans pagination : le fichier contient
     * tous les résultats filtrés, pas seulement la page affichée à l'écran.
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] exportNaturesPdf(NatureLigneFonctionnelleSearchCriteria criteria) {
        log.info("Export PDF des natures de lignes fonctionnelles — critères: {}", criteria);

        Specification<NatureLigneFonctionnelle> spec = NatureLigneFonctionnelleSpecification.searchByCriteria(criteria);
        List<NatureLigneFonctionnelle> natures = repository.findAll(spec, Sort.by(Sort.Direction.ASC, "id"));

        String title = "Natures de Lignes Fonctionnelles";
        Color medBlue = Color.decode("#2563eb");
        Color rowAlt = Color.decode("#f8fafc");
        Color borderGray = Color.decode("#e5e7eb");

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

                    Paragraph sub = new Paragraph(
                            "Exporté le " + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                    + "  •  " + natures.size() + " nature(s)",
                            subFont);
                    sub.setAlignment(Element.ALIGN_CENTER);
                    sub.setSpacingAfter(8f);
                    doc.add(sub);

                    float[] colWidths = {15f, 20f, 35f, 20f};
                    PdfPTable table = new PdfPTable(colWidths);
                    table.setWidthPercentage(100f);
                    table.setSpacingBefore(8f);
                    table.setHeaderRows(1);

                    for (String h : new String[]{"Code", "Libellé", "Description", "Portée"}) {
                        PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                        c.setBackgroundColor(medBlue);
                        c.setPadding(5f);
                        c.setHorizontalAlignment(Element.ALIGN_CENTER);
                        c.setBorderColor(Color.WHITE);
                        c.setBorderWidth(0.5f);
                        table.addCell(c);
                    }

                    for (int i = 0; i < natures.size(); i++) {
                        NatureLigneFonctionnelle n = natures.get(i);
                        Color rowBg = i % 2 == 0 ? Color.WHITE : rowAlt;

                        addPdfCell(table, n.getCode(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                        addPdfCell(table, n.getLabel(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                        addPdfCell(table, n.getDescription(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                        addPdfCell(table, porteeLabel(n.getPortee()), cellFont, rowBg, borderGray, Element.ALIGN_CENTER);
                    }

                    doc.add(table);
                } catch (DocumentException e) {
                    throw new RuntimeException("Erreur lors de la génération du contenu du PDF des natures.", e);
                }
            });
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des natures.", e);
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

    private String porteeLabel(PorteeNature portee) {
        if (portee == null) return "—";
        return switch (portee) {
            case GENERALE -> "Générale uniquement";
            case SPECIFIQUE -> "Spécifique uniquement";
            case GENERALE_ET_SPECIFIQUE -> "Générale et Spécifique";
        };
    }
}
