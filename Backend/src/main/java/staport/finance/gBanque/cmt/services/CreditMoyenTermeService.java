package staport.finance.gBanque.cmt.services;

import com.lowagie.text.Chunk;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
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
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.cmt.dtos.*;
import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;
import staport.finance.gBanque.cmt.echeances.services.interfaces.IEcheanceCMTService;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;
import staport.finance.gBanque.cmt.mappers.CreditMoyenTermeMapper;
import staport.finance.gBanque.cmt.repositories.CreditMoyenTermeRepository;
import staport.finance.gBanque.cmt.specifications.CreditMoyenTermeSpecification;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.reports.service.StaportPdfReportService;
import staport.finance.gBanque.reports.utils.MontantFormatter;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour la gestion des crédits à moyen terme (CMT).
 */
@Service
@Transactional(readOnly = true)
public class CreditMoyenTermeService {

    private static final Logger log = LoggerFactory.getLogger(CreditMoyenTermeService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color MED_BLUE = Color.decode("#2563eb");
    private static final Color LIGHT_BLUE = Color.decode("#dbeafe");
    private static final Color ROW_ALT = Color.decode("#f8fafc");
    private static final Color GREEN = Color.decode("#16a34a");
    private static final Color ORANGE = Color.decode("#ea580c");
    private static final Color BORDER_GRAY = Color.decode("#e5e7eb");

    private final CreditMoyenTermeRepository creditMoyenTermeRepository;
    private final IBanqueService banqueService;
    private final CreditMoyenTermeMapper creditMoyenTermeMapper;
    private final IEcheanceCMTService echeanceCmtService;
    private final StaportPdfReportService staportPdfReportService;

    public CreditMoyenTermeService(CreditMoyenTermeRepository creditMoyenTermeRepository,
                                   IBanqueService banqueService,
                                   CreditMoyenTermeMapper creditMoyenTermeMapper,
                                   IEcheanceCMTService echeanceCmtService,
                                   StaportPdfReportService staportPdfReportService) {
        this.creditMoyenTermeRepository = creditMoyenTermeRepository;
        this.banqueService = banqueService;
        this.creditMoyenTermeMapper = creditMoyenTermeMapper;
        this.echeanceCmtService = echeanceCmtService;
        this.staportPdfReportService = staportPdfReportService;
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = CreditMoyenTerme.class)
    public CreditMoyenTermeResponseDto create(CreditMoyenTermeRequestDto requestDto) {
        log.info("Création d'un crédit CMT — banqueId: {}", requestDto.banqueId());

        Banque banque = banqueService.findOrThrow(requestDto.banqueId());

        CreditMoyenTerme credit = creditMoyenTermeMapper.toEntity(requestDto, banque);
        CreditMoyenTerme savedCredit = creditMoyenTermeRepository.save(credit);

        echeanceCmtService.generateEcheances(savedCredit);
        log.info("Crédit CMT créé — ID: {}", savedCredit.getId());

        return creditMoyenTermeMapper.toResponseDto(savedCredit);
    }

    @Transactional(readOnly = true)
    public CreditMoyenTermeResponseDto findById(Long id) {
        return creditMoyenTermeMapper.toResponseDto(findOrThrow(id));
    }

    /**
     * Résout l'entité par son ID ou lève une exception.
     * Point d'entrée public du domaine {@code cmt} pour les autres modules
     * (ex. {@code ValidationController}, cf. Bounded Context — architecture.md).
     */
    @Transactional(readOnly = true)
    public CreditMoyenTerme findOrThrow(Long id) {
        return creditMoyenTermeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Credit moyen terme introuvable avec l'ID : " + id));
    }

    @Transactional(readOnly = true)
    public Page<CreditMoyenTermeResponseDto> searchActiveCredits(CreditMoyenTermeSearchDto searchDto, Pageable pageable) {
        Specification<CreditMoyenTerme> statusSpec = CreditMoyenTermeSpecification.getActiveCredits();
        Specification<CreditMoyenTerme> searchSpec = CreditMoyenTermeSpecification.searchByCriteria(searchDto);

        return creditMoyenTermeRepository
                .findAll(Specification.where(statusSpec).and(searchSpec), defaultSort(pageable))
                .map(creditMoyenTermeMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<CreditMoyenTermeResponseDto> searchPendingClosureCredits(CreditMoyenTermeSearchDto searchDto, Pageable pageable) {
        Specification<CreditMoyenTerme> statusSpec = CreditMoyenTermeSpecification.getPendingClosureCredits();
        Specification<CreditMoyenTerme> searchSpec = CreditMoyenTermeSpecification.searchByCriteria(searchDto);

        return creditMoyenTermeRepository
                .findAll(Specification.where(statusSpec).and(searchSpec), defaultSort(pageable))
                .map(creditMoyenTermeMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<CreditMoyenTermeResponseDto> searchArchivedCredits(CreditMoyenTermeSearchDto searchDto, Pageable pageable) {
        Specification<CreditMoyenTerme> statusSpec = CreditMoyenTermeSpecification.getArchivedCredits();
        Specification<CreditMoyenTerme> searchSpec = CreditMoyenTermeSpecification.searchByCriteria(searchDto);

        return creditMoyenTermeRepository
                .findAll(Specification.where(statusSpec).and(searchSpec), defaultSort(pageable))
                .map(creditMoyenTermeMapper::toResponseDto);
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = CreditMoyenTerme.class)
    public CreditMoyenTermeResponseDto update(Long id, CreditMoyenTermeRequestDto requestDto) {
        log.info("Mise à jour du crédit CMT — ID: {}", id);

        CreditMoyenTerme existingCredit = findOrThrow(id);
        Banque banque = banqueService.findOrThrow(requestDto.banqueId());

        creditMoyenTermeMapper.updateEntity(requestDto, existingCredit, banque);
        CreditMoyenTerme updatedCredit = creditMoyenTermeRepository.save(existingCredit);

        echeanceCmtService.regenerateEcheances(updatedCredit);
        log.info("Crédit CMT mis à jour — ID: {}", updatedCredit.getId());

        return creditMoyenTermeMapper.toResponseDto(updatedCredit);
    }

    /**
     * Supprime un crédit à moyen terme ainsi que toutes ses échéances associées.
     * La suppression est bloquée si le crédit contient des échéances déjà payées.
     *
     * @param id L'ID du crédit à supprimer
     * @return Un DTO de confirmation de suppression
     * @throws ResourceNotFoundException Si le crédit n'existe pas
     * @throws ValidationException Si le crédit contient des échéances payées
     */
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = CreditMoyenTerme.class)
    public DeleteResponseDto delete(Long id) {
        log.warn("Suppression du crédit CMT — ID: {}", id);

        CreditMoyenTerme credit = findOrThrow(id);

        if (echeanceCmtService.hasPaidEcheances(id)) {
            throw new ValidationException(
                    "Impossible de supprimer le crédit avec l'ID " + id +
                            " car certaines de ses échéances ont déjà été payées. " +
                            "Veuillez l'archiver au lieu de le supprimer.");
        }

        int echeancesDeleted = echeanceCmtService.deleteEcheancesByCreditMoyenTerme(credit);
        creditMoyenTermeRepository.delete(credit);

        log.info("Crédit CMT supprimé — ID: {}, {} échéance(s) supprimée(s)", id, echeancesDeleted);

        return new DeleteResponseDto("Credit moyen terme deleted successfully");
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = CreditMoyenTerme.class)
    public CreditMoyenTermeResponseDto markAsArchived(Long id) {
        log.info("Archivage du crédit CMT — ID: {}", id);

        CreditMoyenTerme credit = findOrThrow(id);

        if (credit.isCompleted()) {
            throw new ValidationException("Le crédit avec l'ID " + id + " est déjà archivé.");
        }

        boolean hasUnpaidInstallments = credit.getEcheanceCMTS().stream()
                .anyMatch(echeance -> echeance.getStatutPaiement() != StatutEcheanceCMT.PAYE);

        if (hasUnpaidInstallments) {
            throw new ValidationException("Le crédit ne peut être archivé que si toutes les échéances ont été payées.");
        }

        credit.setCompleted(true);
        CreditMoyenTerme updatedCredit = creditMoyenTermeRepository.save(credit);

        log.info("Crédit CMT archivé — ID: {}", id);
        return creditMoyenTermeMapper.toResponseDto(updatedCredit);
    }

    private Pageable defaultSort(Pageable pageable) {
        return pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));
    }

    // ========================== EXPORT PDF ==========================

    /**
     * Exporte en PDF l'intégralité des crédits actifs correspondant aux filtres fournis
     * (mêmes critères que {@code GET /credits-cmt/actifs}) — sans pagination : le fichier
     * contient tous les résultats filtrés, pas seulement la page affichée à l'écran.
     */
    @Transactional(readOnly = true)
    public byte[] exportActiveCreditsPdf(CreditMoyenTermeSearchDto searchDto) {
        log.info("Export PDF des crédits CMT actifs — critères: {}", searchDto);
        return generateCreditsPdf("Crédits Moyen Terme Actifs",
                CreditMoyenTermeSpecification.getActiveCredits(), searchDto);
    }

    /**
     * Exporte en PDF l'intégralité des crédits en attente de main levée correspondant
     * aux filtres fournis (mêmes critères que {@code GET /credits-cmt/en-attente-de-main-levees}).
     */
    @Transactional(readOnly = true)
    public byte[] exportPendingClosureCreditsPdf(CreditMoyenTermeSearchDto searchDto) {
        log.info("Export PDF des crédits CMT en attente de main levée — critères: {}", searchDto);
        return generateCreditsPdf("Crédits Moyen Terme en Attente de Main Levée",
                CreditMoyenTermeSpecification.getPendingClosureCredits(), searchDto);
    }

    /**
     * Exporte en PDF l'intégralité des crédits archivés correspondant aux filtres fournis
     * (mêmes critères que {@code GET /credits-cmt/archives}).
     */
    @Transactional(readOnly = true)
    public byte[] exportArchivedCreditsPdf(CreditMoyenTermeSearchDto searchDto) {
        log.info("Export PDF des crédits CMT archivés — critères: {}", searchDto);
        return generateCreditsPdf("Crédits Moyen Terme Archivés",
                CreditMoyenTermeSpecification.getArchivedCredits(), searchDto);
    }

    /**
     * Génère le PDF partagé par les 3 écrans de liste (actifs / en attente de main levée /
     * archivés) : même mise en forme, seuls le titre et la spécification de statut varient.
     */
    private byte[] generateCreditsPdf(String title, Specification<CreditMoyenTerme> statusSpec, CreditMoyenTermeSearchDto searchDto) {
        Specification<CreditMoyenTerme> searchSpec = CreditMoyenTermeSpecification.searchByCriteria(searchDto);
        List<CreditMoyenTerme> credits = creditMoyenTermeRepository
                .findAll(Specification.where(statusSpec).and(searchSpec), Sort.by(Sort.Direction.ASC, "id"));

        BigDecimal totalNominal = credits.stream()
                .map(CreditMoyenTerme::getNominal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        List<String> filterLines = buildFilterSummary(searchDto);

        try {
            return staportPdfReportService.generateReport(title, PageSize.A4.rotate(), (doc, writer) -> {
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
                            + "  •  " + credits.size() + " crédit(s)";
                    if (!filterLines.isEmpty()) {
                        exportInfo += "  •  Filtres : " + String.join("  |  ", filterLines);
                    }
                    Paragraph sub = new Paragraph(exportInfo, subFont);
                    sub.setAlignment(Element.ALIGN_CENTER);
                    sub.setSpacingAfter(8f);
                    doc.add(sub);

                    float[] colWidths = {18f, 12f, 12f, 8f, 8f, 12f, 10f, 10f};
                    PdfPTable table = new PdfPTable(colWidths);
                    table.setWidthPercentage(100f);
                    table.setSpacingBefore(8f);
                    table.setHeaderRows(1);

                    for (String h : new String[]{
                            "Désignation", "Banque", "Nominal (MAD)", "Taux (%)",
                            "Durée (mois)", "Échéance (MAD)", "Date début", "Statut"}) {
                        PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                        c.setBackgroundColor(MED_BLUE);
                        c.setPadding(5f);
                        c.setHorizontalAlignment(Element.ALIGN_CENTER);
                        c.setBorderColor(Color.WHITE);
                        c.setBorderWidth(0.5f);
                        table.addCell(c);
                    }

                    for (int i = 0; i < credits.size(); i++) {
                        CreditMoyenTerme c = credits.get(i);
                        Color rowBg = i % 2 == 0 ? Color.WHITE : ROW_ALT;

                        addPdfCell(table, c.getDesignation(), cellFont, rowBg, BORDER_GRAY, Element.ALIGN_LEFT);
                        addPdfCell(table, c.getBanque() != null ? c.getBanque().getNom() : "—", cellFont, rowBg, BORDER_GRAY, Element.ALIGN_LEFT);
                        addPdfCell(table, MontantFormatter.format(c.getNominal()), cellFont, rowBg, BORDER_GRAY, Element.ALIGN_RIGHT);
                        addPdfCell(table, MontantFormatter.format(c.getTaux()), cellFont, rowBg, BORDER_GRAY, Element.ALIGN_RIGHT);
                        addPdfCell(table, String.valueOf(c.getDuree()), cellFont, rowBg, BORDER_GRAY, Element.ALIGN_CENTER);
                        addPdfCell(table, MontantFormatter.format(c.getEcheance()), cellFont, rowBg, BORDER_GRAY, Element.ALIGN_RIGHT);
                        addPdfCell(table, c.getDateDebut() != null ? c.getDateDebut().format(DATE_FMT) : "—", cellFont, rowBg, BORDER_GRAY, Element.ALIGN_CENTER);

                        boolean termine = c.isCompleted();
                        PdfPCell statusCell = new PdfPCell(new Phrase(
                                termine ? "TERMINÉ" : "EN COURS",
                                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
                        statusCell.setBackgroundColor(termine ? GREEN : ORANGE);
                        statusCell.setPadding(4f);
                        statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        statusCell.setBorderColor(BORDER_GRAY);
                        statusCell.setBorderWidth(0.5f);
                        table.addCell(statusCell);
                    }

                    PdfPCell totalLabel = new PdfPCell(
                            new Phrase("TOTAL — " + credits.size() + " crédit(s)", totalFont));
                    totalLabel.setColspan(2);
                    totalLabel.setBackgroundColor(LIGHT_BLUE);
                    totalLabel.setPadding(5f);
                    totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalLabel.setBorderColor(MED_BLUE);
                    table.addCell(totalLabel);

                    PdfPCell totalVal = new PdfPCell(
                            new Phrase(MontantFormatter.format(totalNominal) + " MAD", totalFont));
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
                    throw new RuntimeException("Erreur lors de la génération du contenu du PDF des crédits CMT.", e);
                }
            });
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des crédits CMT.", e);
        }
    }

    private List<String> buildFilterSummary(CreditMoyenTermeSearchDto dto) {
        List<String> lines = new ArrayList<>();
        if (dto == null) return lines;

        if (dto.designation() != null && !dto.designation().isBlank()) {
            lines.add("Désignation : " + dto.designation());
        }
        if (dto.banqueId() != null) {
            try {
                lines.add("Banque : " + banqueService.findOrThrow(dto.banqueId()).getNom());
            } catch (ResourceNotFoundException e) {
                log.debug("Banque id={} introuvable pour le résumé de filtre export — ignorée", dto.banqueId());
            }
        }
        if (dto.dateDebutMin() != null) {
            lines.add("Date début min : " + dto.dateDebutMin().format(DATE_FMT));
        }
        if (dto.dateDebutMax() != null) {
            lines.add("Date début max : " + dto.dateDebutMax().format(DATE_FMT));
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
}
