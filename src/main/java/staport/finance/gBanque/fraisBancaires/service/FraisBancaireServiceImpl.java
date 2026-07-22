package staport.finance.gBanque.fraisBancaires.service;

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
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.fraisBancaires.dtos.EncoursFraisBancaireDto;
import staport.finance.gBanque.fraisBancaires.dtos.EncoursFraisBancaireSearchDto;
import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireRequestDto;
import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireResponseDto;
import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireSearchDto;
import staport.finance.gBanque.fraisBancaires.entities.FraisBancaire;
import staport.finance.gBanque.fraisBancaires.mapper.FraisBancaireMapper;
import staport.finance.gBanque.fraisBancaires.repositories.FraisBancaireRepository;
import staport.finance.gBanque.fraisBancaires.spec.FraisBancaireSpecification;
import staport.finance.gBanque.prefinancement.enums.Status;
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
import java.util.Objects;

/**
 * Implémentation du service Frais bancaires.
 *
 * <p>Encapsulation des domaines (architecture.md) : la résolution de la {@link Banque} passe
 * exclusivement par {@link IBanqueService} — jamais par injection directe de
 * {@code BanqueRepository}.
 *
 * <p>Renommé depuis {@code FraisBancaireService} (sweep de conformité) pour respecter la
 * convention {@code IXxxService}/{@code XxxServiceImpl} appliquée à tous les modules déjà
 * sweepés — le contrôleur injectait auparavant la classe concrète directement.
 */
@Service
public class FraisBancaireServiceImpl implements IFraisBancaireService {

    private static final Logger log = LoggerFactory.getLogger(FraisBancaireServiceImpl.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color MED_BLUE = Color.decode("#2563eb");
    private static final Color LIGHT_BLUE = Color.decode("#dbeafe");
    private static final Color ROW_ALT = Color.decode("#f8fafc");
    private static final Color BORDER_GRAY = Color.decode("#e5e7eb");
    private final FraisBancaireRepository fraisBancaireRepository;
    private final IBanqueService banqueService;
    private final FraisBancaireMapper fraisBancaireMapper;
    private final StaportPdfReportService staportPdfReportService;

    public FraisBancaireServiceImpl(FraisBancaireRepository fraisBancaireRepository,
                                     IBanqueService banqueService,
                                     FraisBancaireMapper fraisBancaireMapper,
                                     StaportPdfReportService staportPdfReportService) {
        this.fraisBancaireRepository = fraisBancaireRepository;
        this.banqueService = banqueService;
        this.fraisBancaireMapper = fraisBancaireMapper;
        this.staportPdfReportService = staportPdfReportService;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsChargeFixeGeneree(Long banqueId, String libelle, LocalDate debut, LocalDate fin) {
        return fraisBancaireRepository.existsByBanqueIdAndLibelleAndDateEmissionBetween(banqueId, libelle, debut, fin);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = FraisBancaire.class)
    public FraisBancaireResponseDto createFraisBancaire(FraisBancaireRequestDto requestDto) {
        log.info("Création d'un frais bancaire - Banque ID: {}, Libellé: '{}'", requestDto.banqueId(), requestDto.libelle());

        Banque banque = banqueService.findOrThrow(requestDto.banqueId());
        FraisBancaire fraisBancaire = fraisBancaireMapper.toEntity(requestDto, banque);
        FraisBancaire saved = fraisBancaireRepository.save(fraisBancaire);

        log.info("Frais bancaire créé avec succès - ID: {}", saved.getId());
        return fraisBancaireMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public FraisBancaireResponseDto getById(Long id) {
        log.info("Récupération du frais bancaire avec l'ID: {}", id);

        FraisBancaire fraisBancaire = fraisBancaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Frais bancaire non trouvé avec l'ID: " + id));

        return fraisBancaireMapper.toResponseDto(fraisBancaire);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FraisBancaireResponseDto> searchFraisBancaire(FraisBancaireSearchDto searchDto, Pageable pageable) {
        log.info("Recherche de frais bancaires - Critères: {}", searchDto);

        Page<FraisBancaireResponseDto> result = fraisBancaireRepository.findAll(
                        FraisBancaireSpecification.searchByCriteria(searchDto), pageable)
                .map(fraisBancaireMapper::toResponseDto);

        log.info("Recherche terminée - {} frais bancaire(s) trouvé(s) sur {} au total",
                result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportFraisBancairePdf(FraisBancaireSearchDto searchDto) {
        log.info("Export PDF des frais bancaires - Critères: {}", searchDto);

        List<FraisBancaire> fraisBancaires = fraisBancaireRepository.findAll(
                FraisBancaireSpecification.searchByCriteria(searchDto),
                Sort.by(Sort.Direction.DESC, "dateEmission"));

        List<FraisBancaireResponseDto> dtos = fraisBancaires.stream()
                .map(fraisBancaireMapper::toResponseDto)
                .toList();

        return generateFraisBancairePdf(dtos, searchDto);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = FraisBancaire.class)
    public FraisBancaireResponseDto updateFraisBancaire(Long id, FraisBancaireRequestDto requestDto) {
        log.info("Mise à jour du frais bancaire ID: {}", id);

        FraisBancaire fraisBancaire = fraisBancaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Frais bancaire non trouvé avec l'ID: " + id));

        Banque banque = banqueService.findOrThrow(requestDto.banqueId());

        fraisBancaire.setLibelle(requestDto.libelle());
        fraisBancaire.setObservation(requestDto.observation());
        fraisBancaire.setMontant(requestDto.montant());
        fraisBancaire.setBanque(banque);
        fraisBancaire.setDateEmission(requestDto.dateEmission());

        FraisBancaire updated = fraisBancaireRepository.save(fraisBancaire);
        log.info("Frais bancaire ID: {} mis à jour avec succès", id);
        return fraisBancaireMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = FraisBancaire.class)
    public void deleteFraisBancaire(Long id) {
        log.info("Suppression du frais bancaire avec l'ID: {}", id);

        FraisBancaire fraisBancaire = fraisBancaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Frais bancaire non trouvé avec l'ID: " + id));

        if (fraisBancaire.getStatus() != Status.PREVU) {
            log.warn("Tentative de suppression d'un frais bancaire déjà réglé — id: {}", id);
            throw new ValidationException("Le frais bancaire id: " + id + " est déjà réglé et ne peut plus être supprimé.");
        }

        fraisBancaireRepository.deleteById(id);
        log.info("Frais bancaire ID: {} supprimé avec succès", id);
    }

    @Override
    @Transactional(readOnly = true)
    public EncoursFraisBancaireDto getOutstandingFraisBancaire(EncoursFraisBancaireSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Récupération des frais bancaires en cours - banqueId={}, Critères: {}", banqueId, searchDto);

        LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        Specification<FraisBancaire> spec = Specification.where(FraisBancaireSpecification.outstandingUntil(endOfMonth))
                .and(FraisBancaireSpecification.hasBanque(banqueId))
                .and(FraisBancaireSpecification.hasLibelle(searchDto.libelle()))
                .and(FraisBancaireSpecification.hasMontant(searchDto.montant()));

        Page<FraisBancaire> page = fraisBancaireRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));
        BigDecimal total = fraisBancaireRepository.findAll(spec).stream()
                .map(FraisBancaire::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Encours calculé: {}, Nombre de frais: {}", total, page.getTotalElements());

        Page<FraisBancaireResponseDto> pageDto = page.map(fraisBancaireMapper::toResponseDto);
        return new EncoursFraisBancaireDto(total, pageDto);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = FraisBancaire.class)
    public FraisBancaireResponseDto toggleStatus(Long id) {
        log.info("Basculement du statut du frais bancaire ID: {}", id);

        FraisBancaire fraisBancaire = fraisBancaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Frais bancaire non trouvé avec l'ID: " + id));

        Status oldStatus = fraisBancaire.getStatus();
        Status newStatus = oldStatus == Status.PAYE ? Status.PREVU : Status.PAYE;
        fraisBancaire.setStatus(newStatus);
        FraisBancaire updated = fraisBancaireRepository.save(fraisBancaire);

        log.info("Statut du frais bancaire ID: {} basculé de {} vers {}", id, oldStatus, newStatus);
        return fraisBancaireMapper.toResponseDto(updated);
    }

    // ── Export PDF ───────────────────────────────────────────────────────────────────────────

    /**
     * Génère le PDF des frais bancaires filtrés — mêmes colonnes que l'écran "Frais Bancaires"
     * (Libellé, Banque, Montant, Date d'émission, Statut), plus un résumé des filtres appliqués
     * et le total des montants.
     */
    private byte[] generateFraisBancairePdf(List<FraisBancaireResponseDto> fraisBancaires, FraisBancaireSearchDto searchDto) {
        String title = "Frais Bancaires";

        BigDecimal totalMontant = fraisBancaires.stream()
                .map(FraisBancaireResponseDto::montant)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> filterLines = buildFilterSummary(searchDto);

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
                            + "  •  " + fraisBancaires.size() + " frais bancaire(s)";
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

                    for (int i = 0; i < fraisBancaires.size(); i++) {
                        FraisBancaireResponseDto f = fraisBancaires.get(i);
                        Color rowBg = i % 2 == 0 ? Color.WHITE : ROW_ALT;

                        addFbPdfCell(table, f.libelle(), cellFont, rowBg, Element.ALIGN_LEFT);
                        addFbPdfCell(table, f.banqueNom(), cellFont, rowBg, Element.ALIGN_LEFT);
                        addFbPdfCell(table, formatMontantFb(f.montant()), cellFont, rowBg, Element.ALIGN_RIGHT);
                        addFbPdfCell(table, f.dateEmission() != null ? f.dateEmission().format(DATE_FMT) : "—", cellFont, rowBg, Element.ALIGN_CENTER);

                        PdfPCell statusCell = new PdfPCell(new Phrase(
                                f.status() != null ? f.status().name() : "—",
                                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
                        statusCell.setBackgroundColor(f.status() == Status.PAYE ? Color.decode("#16a34a") : Color.decode("#ea580c"));
                        statusCell.setPadding(4f);
                        statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        statusCell.setBorderColor(BORDER_GRAY);
                        statusCell.setBorderWidth(0.5f);
                        table.addCell(statusCell);
                    }

                    PdfPCell totalLabel = new PdfPCell(
                            new Phrase("TOTAL — " + fraisBancaires.size() + " frais bancaire(s)", totalFont));
                    totalLabel.setColspan(2);
                    totalLabel.setBackgroundColor(LIGHT_BLUE);
                    totalLabel.setPadding(5f);
                    totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalLabel.setBorderColor(MED_BLUE);
                    table.addCell(totalLabel);

                    PdfPCell totalVal = new PdfPCell(new Phrase(formatMontantFb(totalMontant) + " MAD", totalFont));
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
                    throw new RuntimeException("Erreur lors de la génération du contenu du PDF des frais bancaires.", e);
                }
            });
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des frais bancaires.", e);
        }
    }

    private List<String> buildFilterSummary(FraisBancaireSearchDto dto) {
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
            lines.add("Montant : " + formatMontantFb(dto.montant()));
        }
        return lines;
    }

    private void addFbPdfCell(PdfPTable table, String text, Font font, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", font));
        cell.setBackgroundColor(bg);
        cell.setPadding(4f);
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(BORDER_GRAY);
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
    }

    private String formatMontantFb(BigDecimal montant) {
        if (montant == null) return "—";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        return formatter.format(montant);
    }
}
