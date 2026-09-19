package staport.finance.gBanque.restitution.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
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
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.restitution.dtos.RestitutionRequestDto;
import staport.finance.gBanque.restitution.dtos.RestitutionResponseDto;
import staport.finance.gBanque.restitution.dtos.RestitutionSearchDto;
import staport.finance.gBanque.restitution.entities.Restitution;
import staport.finance.gBanque.restitution.mapper.RestitutionMapper;
import staport.finance.gBanque.restitution.repository.RestitutionRepository;
import staport.finance.gBanque.restitution.specs.RestitutionSpec;
import staport.finance.gBanque.reports.service.StaportPdfReportService;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Implémentation du service Restitution.
 *
 * <p>Encapsulation des domaines (architecture.md) : la résolution de la {@link Banque} passe
 * exclusivement par {@link IBanqueService} — jamais par injection directe de
 * {@code BanqueRepository}.
 *
 * <p>Suppression bloquée si déjà PAYE (précisé par HMAYDA le 14/07/2026 : une restitution réglée ne
 * peut plus être supprimée, même règle qu'Impayées/Impôts et Taxes) — {@code deleteRestitution} lève
 * {@link ValidationException} dans ce cas.
 */
@Service
public class RestitutionServiceImpl implements IRestitutionService {

    private static final Logger log = LogManager.getLogger(RestitutionServiceImpl.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RestitutionRepository restitutionRepository;
    private final IBanqueService banqueService;
    private final RestitutionMapper restitutionMapper;
    private final StaportPdfReportService staportPdfReportService;

    public RestitutionServiceImpl(RestitutionRepository restitutionRepository, IBanqueService banqueService,
                                   RestitutionMapper restitutionMapper, StaportPdfReportService staportPdfReportService) {
        this.restitutionRepository = restitutionRepository;
        this.banqueService = banqueService;
        this.restitutionMapper = restitutionMapper;
        this.staportPdfReportService = staportPdfReportService;
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = Restitution.class)
    public RestitutionResponseDto createRestitution(RestitutionRequestDto requestDto) {
        log.info("Début de création d'une restitution avec les données suivantes: {}", requestDto);

        Banque banque = banqueService.findOrThrow(requestDto.banqueId());

        Restitution restitution = restitutionMapper.toEntity(requestDto, banque);
        restitutionRepository.save(restitution);

        log.info("Restitution créée avec succès avec l'id={}", restitution.getId());
        return restitutionMapper.toResponseDto(restitution);
    }

    @Override
    @Transactional(readOnly = true)
    public RestitutionResponseDto getById(Long id) {
        log.info("Récupération de la restitution avec l'id={}", id);

        Restitution restitution = restitutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restitution introuvable avec l'id: " + id));

        log.info("Restitution récupérée avec succès: id={}", id);
        return restitutionMapper.toResponseDto(restitution);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RestitutionResponseDto> searchRestitutions(RestitutionSearchDto searchDto, Pageable pageable) {
        log.info("Recherche de restitutions avec les critères: {}", searchDto);

        Page<RestitutionResponseDto> result = restitutionRepository.findAll(
                        RestitutionSpec.buildSpecification(searchDto),
                        pageable
                )
                .map(restitutionMapper::toResponseDto);

        log.info("Recherche terminée: {} restitutions trouvées", result.getTotalElements());
        return result;
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Restitution.class)
    public RestitutionResponseDto updateRestitution(Long id, RestitutionRequestDto requestDto) {
        log.info("Début de mise à jour de la restitution id={} avec les données: {}", id, requestDto);

        Restitution restitution = restitutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restitution introuvable avec l'id: " + id));

        Banque banque = banqueService.findOrThrow(requestDto.banqueId());

        restitution.setLibelle(requestDto.libelle());
        restitution.setObservation(requestDto.observation());
        restitution.setMontant(requestDto.montant());
        restitution.setBanque(banque);
        restitution.setDateEmission(requestDto.dateEmission());

        restitutionRepository.save(restitution);
        log.info("Restitution mise à jour avec succès: id={}", restitution.getId());
        return restitutionMapper.toResponseDto(restitution);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = Restitution.class)
    public void deleteRestitution(Long id) {
        log.info("Début de suppression de la restitution avec l'id={}", id);

        Restitution restitution = restitutionRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Restitution introuvable avec l'id={}", id);
                    return new ResourceNotFoundException("Restitution introuvable avec l'id: " + id);
                });

        if (restitution.getStatus() != Status.PREVU) {
            log.warn("Tentative de suppression d'une restitution déjà réglée — id={}", id);
            throw new ValidationException("La restitution id: " + id + " est déjà réglée et ne peut plus être supprimée.");
        }

        restitutionRepository.deleteById(id);
        log.info("Restitution supprimée avec succès: id={}", id);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Restitution.class)
    public RestitutionResponseDto toggleStatus(Long id) {
        log.info("Basculement du statut de la restitution ID: {}", id);

        Restitution restitution = restitutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restitution introuvable avec l'id: " + id));

        Status oldStatus = restitution.getStatus();
        Status newStatus = oldStatus == Status.PAYE ? Status.PREVU : Status.PAYE;

        restitution.setStatus(newStatus);
        Restitution updated = restitutionRepository.save(restitution);

        log.info("Statut de la restitution ID: {} basculé de {} vers {}", id, oldStatus, newStatus);
        return restitutionMapper.toResponseDto(updated);
    }

    // ── Export PDF ────────────────────────────────────────────────────────────

    /**
     * Génère un export PDF de l'intégralité des restitutions correspondant aux critères fournis
     * (mêmes filtres que {@link #searchRestitutions}) — sans pagination : le fichier imprime la
     * totalité des résultats filtrés, pas seulement la page affichée à l'écran.
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] exporterPdf(RestitutionSearchDto searchDto) {
        log.info("Export PDF restitutions — critères: {}", searchDto);

        Specification<Restitution> spec = RestitutionSpec.buildSpecification(searchDto);
        List<Restitution> restitutions = restitutionRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "dateEmission"));

        BigDecimal totalMontant = restitutions.stream()
                .map(Restitution::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<String> filterLines = buildFilterSummary(searchDto);

        return genererPdf("État des Restitutions", restitutions, totalMontant, filterLines);
    }

    private byte[] genererPdf(String title, List<Restitution> restitutions, BigDecimal totalMontant, List<String> filterLines) {
        try {
            return staportPdfReportService.generateReport(
                title,
                PageSize.A4.rotate(),
                (doc, writer) -> {
                    try {
                        doc.add(Chunk.NEWLINE);
                        doc.add(Chunk.NEWLINE);

                        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
                        Font subFont   = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.decode("#374151"));
                        Font hdrFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
                        Font cellFont  = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
                        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.decode("#1e3a8a"));

                        Paragraph titlePara = new Paragraph(title, titleFont);
                        titlePara.setAlignment(Element.ALIGN_CENTER);
                        doc.add(titlePara);

                        String exportInfo = "Exporté le " + LocalDate.now().format(DATE_FMT)
                                + "  •  " + restitutions.size() + " restitution(s)";
                        if (!filterLines.isEmpty()) {
                            exportInfo += "  •  Filtres : " + String.join("  |  ", filterLines);
                        }
                        Paragraph sub = new Paragraph(exportInfo, subFont);
                        sub.setAlignment(Element.ALIGN_CENTER);
                        sub.setSpacingAfter(8f);
                        doc.add(sub);

                        Color medBlue    = Color.decode("#2563eb");
                        Color lightBlue  = Color.decode("#dbeafe");
                        Color rowAlt     = Color.decode("#f8fafc");
                        Color green      = Color.decode("#16a34a");
                        Color orange     = Color.decode("#ea580c");
                        Color borderGray = Color.decode("#e5e7eb");

                        float[] colWidths = {24f, 19f, 19f, 19f, 19f};
                        PdfPTable table = new PdfPTable(colWidths);
                        table.setWidthPercentage(100f);
                        table.setSpacingBefore(8f);
                        table.setHeaderRows(1);

                        for (String h : new String[]{
                                "Libellé", "Banque", "Montant (MAD)", "Date émission", "Statut"}) {
                            PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                            c.setBackgroundColor(medBlue);
                            c.setPadding(5f);
                            c.setHorizontalAlignment(Element.ALIGN_CENTER);
                            c.setBorderColor(Color.WHITE);
                            c.setBorderWidth(0.5f);
                            table.addCell(c);
                        }

                        for (int i = 0; i < restitutions.size(); i++) {
                            Restitution r = restitutions.get(i);
                            Color rowBg = i % 2 == 0 ? Color.WHITE : rowAlt;

                            addPdfCell(table, r.getLibelle(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                            addPdfCell(table, r.getBanque() != null ? r.getBanque().getNom() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                            addPdfCell(table, formatMontantPdf(r.getMontant()), cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);
                            addPdfCell(table, r.getDateEmission() != null ? r.getDateEmission().format(DATE_FMT) : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);

                            boolean paye = r.getStatus() == Status.PAYE;
                            PdfPCell statusCell = new PdfPCell(new Phrase(
                                    paye ? "PAYÉE" : "PRÉVUE",
                                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
                            statusCell.setBackgroundColor(paye ? green : orange);
                            statusCell.setPadding(4f);
                            statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                            statusCell.setBorderColor(borderGray);
                            statusCell.setBorderWidth(0.5f);
                            table.addCell(statusCell);
                        }

                        // Ligne total
                        PdfPCell totalLabel = new PdfPCell(
                                new Phrase("TOTAL — " + restitutions.size() + " restitution(s)", totalFont));
                        totalLabel.setColspan(2);
                        totalLabel.setBackgroundColor(lightBlue);
                        totalLabel.setPadding(5f);
                        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        totalLabel.setBorderColor(medBlue);
                        table.addCell(totalLabel);

                        PdfPCell totalVal = new PdfPCell(
                                new Phrase(formatMontantPdf(totalMontant) + " MAD", totalFont));
                        totalVal.setBackgroundColor(lightBlue);
                        totalVal.setPadding(5f);
                        totalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        totalVal.setBorderColor(medBlue);
                        table.addCell(totalVal);

                        for (int j = 0; j < 2; j++) {
                            PdfPCell empty = new PdfPCell(new Phrase(""));
                            empty.setBackgroundColor(lightBlue);
                            empty.setBorderColor(medBlue);
                            table.addCell(empty);
                        }

                        doc.add(table);

                    } catch (DocumentException e) {
                        throw new RuntimeException("Erreur génération contenu PDF restitutions", e);
                    }
                }
            );
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des restitutions", e);
        }
    }

    private void addPdfCell(PdfPTable table, String text, Font font,
                             Color bg, Color border, int align) {
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
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.FRANCE);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(montant);
    }

    private List<String> buildFilterSummary(RestitutionSearchDto dto) {
        List<String> lines = new ArrayList<>();
        if (dto == null) return lines;

        if (dto.banqueId() != null) {
            try {
                lines.add("Banque : " + banqueService.findOrThrow(dto.banqueId()).getNom());
            } catch (ResourceNotFoundException e) {
                log.debug("Banque id={} introuvable pour le résumé de filtre export — ignorée", dto.banqueId());
            }
        }
        if (dto.libelle() != null && !dto.libelle().isBlank())
            lines.add("Libellé : " + dto.libelle());
        if (dto.montant() != null)
            lines.add("Montant : " + formatMontantPdf(dto.montant()) + " MAD");
        if (dto.status() != null)
            lines.add("Statut : " + (dto.status() == Status.PAYE ? "Payée" : "Prévue"));
        if (dto.dateEmissionDebut() != null)
            lines.add("Date émission min : " + dto.dateEmissionDebut().format(DATE_FMT));
        if (dto.dateEmissionFin() != null)
            lines.add("Date émission max : " + dto.dateEmissionFin().format(DATE_FMT));
        if (dto.dateOperationDebut() != null)
            lines.add("Date opération min : " + dto.dateOperationDebut().format(DATE_FMT));
        if (dto.dateOperationFin() != null)
            lines.add("Date opération max : " + dto.dateOperationFin().format(DATE_FMT));
        if (dto.dateValeurDebut() != null)
            lines.add("Date valeur min : " + dto.dateValeurDebut().format(DATE_FMT));
        if (dto.dateValeurFin() != null)
            lines.add("Date valeur max : " + dto.dateValeurFin().format(DATE_FMT));

        return lines;
    }

}
