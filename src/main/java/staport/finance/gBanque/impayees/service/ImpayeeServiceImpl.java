package staport.finance.gBanque.impayees.service;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import staport.finance.gBanque.impayees.dtos.*;
import staport.finance.gBanque.impayees.entities.Impayee;
import staport.finance.gBanque.impayees.mapper.ImpayeeMapper;
import staport.finance.gBanque.impayees.repository.ImpayeeRepository;
import staport.finance.gBanque.impayees.specs.ImpayeeSpecifications;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.reports.service.StaportPdfReportService;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Implémentation du service Impayées.
 *
 * <p>Encapsulation des domaines (architecture.md) : la résolution de la {@link Banque} passe
 * exclusivement par {@link IBanqueService} — jamais par injection directe de
 * {@code BanqueRepository}.
 *
 * <p>Bug latent corrigé (14/07/2026) : {@code endOfMonth} était auparavant un champ calculé une
 * seule fois à la construction du bean singleton — figé à la date de démarrage du serveur au lieu
 * d'être recalculé à chaque appel. Recalculé maintenant localement dans
 * {@link #getOutstandingImpayees}, même correctif que celui appliqué à
 * {@code EcheanceCMTService} lors du sweep CMT.
 */
@Service
public class ImpayeeServiceImpl implements IImpayeeService {

    private static final Logger log = LogManager.getLogger(ImpayeeServiceImpl.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ImpayeeRepository impayeeRepository;
    private final IBanqueService banqueService;
    private final ImpayeeMapper impayeeMapper;
    private final StaportPdfReportService staportPdfReportService;

    public ImpayeeServiceImpl(ImpayeeRepository impayeeRepository, IBanqueService banqueService, ImpayeeMapper impayeeMapper,
                               StaportPdfReportService staportPdfReportService) {
        this.impayeeRepository = impayeeRepository;
        this.banqueService = banqueService;
        this.impayeeMapper = impayeeMapper;
        this.staportPdfReportService = staportPdfReportService;
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = Impayee.class)
    public ImpayeeResponseDto createImpayee(ImpayeeRequestDto requestDto) {
        log.info("Début de création d'une impayée avec les données: {}", requestDto);

        Banque banque = banqueService.findOrThrow(requestDto.banqueId());

        Impayee impayee = impayeeMapper.toEntity(requestDto, banque);
        impayeeRepository.save(impayee);

        log.info("Impayée créée avec succès: id={}", impayee.getId());
        return impayeeMapper.toResponseDto(impayee);
    }

    @Override
    @Transactional(readOnly = true)
    public ImpayeeResponseDto getById(Long id) {
        log.info("Récupération de l'impayée id={}", id);
        Impayee impayee = impayeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Impayée introuvable avec l'ID: " + id));
        return impayeeMapper.toResponseDto(impayee);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ImpayeeResponseDto> searchImpayees(ImpayeeSearchDto searchDto, Pageable pageable) {
        log.info("Recherche d'impayées - Critères: {}", searchDto);
        Page<ImpayeeResponseDto> result = impayeeRepository.findAll(ImpayeeSpecifications.buildSpecification(searchDto), pageable)
                .map(impayeeMapper::toResponseDto);
        log.info("Recherche terminée: {} éléments trouvés", result.getTotalElements());
        return result;
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Impayee.class)
    public ImpayeeResponseDto updateImpayee(Long id, ImpayeeRequestDto requestDto) {
        log.info("Mise à jour de l'impayée id={}", id);

        Impayee impayee = impayeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Impayée introuvable avec l'ID: " + id));

        Banque banque = banqueService.findOrThrow(requestDto.banqueId());

        impayee.setLibelle(requestDto.libelle());
        impayee.setMontant(requestDto.montant());
        impayee.setBanque(banque);
        impayee.setMotif(requestDto.motif());
        impayee.setDateEmission(requestDto.dateEmission());
        impayee.setObservation(requestDto.observation());

        impayeeRepository.save(impayee);
        log.info("Impayée mise à jour avec succès: id={}", id);
        return impayeeMapper.toResponseDto(impayee);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = Impayee.class)
    public void deleteImpayee(Long id) {
        log.info("Suppression de l'impayée id={}", id);

        Impayee impayee = impayeeRepository.findById(id).orElseThrow(() -> {
            log.warn("Impayée introuvable — id={}", id);
            return new ResourceNotFoundException("Impayée introuvable avec l'ID: " + id);
        });

        if (impayee.getStatus() != Status.PREVU) {
            log.warn("Tentative de suppression d'une impayée déjà réglée — id={}", id);
            throw new ValidationException("L'impayée id: " + id + " est déjà réglée et ne peut plus être supprimée.");
        }

        impayeeRepository.deleteById(id);
        log.info("Impayée supprimée avec succès: id={}", id);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Impayee.class)
    public ImpayeeResponseDto toggleStatus(Long id) {
        log.info("Basculement du statut de l'impayée ID: {}", id);

        Impayee impayee = impayeeRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Impayée introuvable — id={}", id);
                    return new ResourceNotFoundException("Impayée introuvable avec l'ID: " + id);
                });

        Status oldStatus = impayee.getStatus();
        Status newStatus = (oldStatus == Status.PAYE) ? Status.PREVU : Status.PAYE;

        impayee.setStatus(newStatus);
        Impayee updated = impayeeRepository.save(impayee);

        log.info("Statut basculé pour ID {}: {} -> {}", id, oldStatus, newStatus);
        return impayeeMapper.toResponseDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public EncoursImpayeeDto getOutstandingImpayees(EncoursImpayeeSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Récupération des encours impayées - banqueId={}, Critères: {}", banqueId, searchDto);

        LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        Specification<Impayee> spec = Specification.where(ImpayeeSpecifications.outstandingUntil(endOfMonth, searchDto.type()))
                .and(ImpayeeSpecifications.hasBanque(banqueId))
                .and(ImpayeeSpecifications.hasLibelle(searchDto.libelle()))
                .and(ImpayeeSpecifications.hasMotif(searchDto.motif()))
                .and(ImpayeeSpecifications.hasMontant(searchDto.montant()));

        Page<Impayee> impayeePage = impayeeRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        BigDecimal totalDesEncours = impayeeRepository.findAll(spec).stream()
                .map(Impayee::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Page<ImpayeeResponseDto> pageDto = impayeePage.map(impayeeMapper::toResponseDto);

        log.info("Encours récupérés: {} éléments, Total: {}", pageDto.getTotalElements(), totalDesEncours);
        return new EncoursImpayeeDto(totalDesEncours, pageDto);
    }

    // ── Export PDF ────────────────────────────────────────────────────────────

    /**
     * Génère un export PDF de l'intégralité des impayées correspondant aux critères fournis
     * (mêmes filtres que {@link #searchImpayees}) — sans pagination : le fichier imprime la
     * totalité des résultats filtrés, pas seulement la page affichée à l'écran.
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] exporterPdf(ImpayeeSearchDto searchDto) {
        log.info("Export PDF impayées — critères: {}", searchDto);

        Specification<Impayee> spec = ImpayeeSpecifications.buildSpecification(searchDto);
        List<Impayee> impayees = impayeeRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "dateEmission"));

        BigDecimal totalMontant = sumMontant(impayees);
        List<String> filterLines = buildFilterSummary(searchDto);

        return genererPdf("État des Impayées", impayees, totalMontant, filterLines);
    }

    /**
     * Génère un export PDF de l'encours d'impayées d'une banque (mêmes critères que
     * {@link #getOutstandingImpayees}, {@code type} inclus) — sans pagination : le fichier imprime
     * la totalité des résultats filtrés, pas seulement la page affichée à l'écran.
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] exporterEncoursPdf(EncoursImpayeeSearchDto searchDto, Long banqueId) {
        log.info("Export PDF encours impayées — banqueId={}, type={}", banqueId, searchDto.type());

        LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        Specification<Impayee> spec = Specification.where(ImpayeeSpecifications.outstandingUntil(endOfMonth, searchDto.type()))
                .and(ImpayeeSpecifications.hasBanque(banqueId))
                .and(ImpayeeSpecifications.hasLibelle(searchDto.libelle()))
                .and(ImpayeeSpecifications.hasMotif(searchDto.motif()))
                .and(ImpayeeSpecifications.hasMontant(searchDto.montant()));

        List<Impayee> impayees = impayeeRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "dateEmission"));

        BigDecimal totalMontant = sumMontant(impayees);
        List<String> filterLines = buildEncoursFilterSummary(searchDto, banqueId);

        String titre = "Encours des Impayées" + (searchDto.type() != null ? " — " + libelleType(searchDto.type()) : "");
        return genererPdf(titre, impayees, totalMontant, filterLines);
    }

    private String libelleType(Type type) {
        return type == Type.ENCAISSEMENT ? "Encaissements" : "Décaissements";
    }

    private BigDecimal sumMontant(List<Impayee> impayees) {
        return impayees.stream()
                .map(Impayee::getMontant)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Construit le document PDF partagé par {@link #exporterPdf} et {@link #exporterEncoursPdf} —
     * même mise en page, seuls le titre, les lignes et le résumé de filtres varient.
     */
    private byte[] genererPdf(String title, List<Impayee> impayees, BigDecimal totalMontant, List<String> filterLines) {
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
                                + "  •  " + impayees.size() + " ligne(s)";
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

                        float[] colWidths = {19f, 14f, 13f, 11f, 13f, 12f, 10f};
                        PdfPTable table = new PdfPTable(colWidths);
                        table.setWidthPercentage(100f);
                        table.setSpacingBefore(8f);
                        table.setHeaderRows(1);

                        for (String h : new String[]{
                                "Libellé", "Banque", "Motif", "Type", "Montant (MAD)", "Date émission", "Statut"}) {
                            PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                            c.setBackgroundColor(medBlue);
                            c.setPadding(5f);
                            c.setHorizontalAlignment(Element.ALIGN_CENTER);
                            c.setBorderColor(Color.WHITE);
                            c.setBorderWidth(0.5f);
                            table.addCell(c);
                        }

                        for (int i = 0; i < impayees.size(); i++) {
                            Impayee p = impayees.get(i);
                            Color rowBg = i % 2 == 0 ? Color.WHITE : rowAlt;

                            addPdfCell(table, p.getLibelle(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                            addPdfCell(table, p.getBanque() != null ? p.getBanque().getNom() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                            addPdfCell(table, p.getMotif() != null ? p.getMotif().name() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                            addPdfCell(table, p.getType() != null ? p.getType().name() : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);
                            addPdfCell(table, formatMontantPdf(p.getMontant()), cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);
                            addPdfCell(table, p.getDateEmission() != null ? p.getDateEmission().format(DATE_FMT) : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);

                            boolean paye = p.getStatus() == Status.PAYE;
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
                                new Phrase("TOTAL — " + impayees.size() + " ligne(s)", totalFont));
                        totalLabel.setColspan(4);
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
                        throw new RuntimeException("Erreur génération contenu PDF impayées", e);
                    }
                }
            );
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des impayées", e);
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

    private List<String> buildFilterSummary(ImpayeeSearchDto dto) {
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
        if (dto.motif() != null)
            lines.add("Motif : " + dto.motif());
        if (dto.type() != null)
            lines.add("Type : " + libelleType(dto.type()));
        if (dto.montant() != null)
            lines.add("Montant : " + formatMontantPdf(dto.montant()) + " MAD");
        if (dto.status() != null)
            lines.add("Statut : " + (dto.status() == Status.PAYE ? "Payée" : "Prévue"));

        return lines;
    }

    /**
     * Résumé des filtres pour l'export PDF de l'écran encours — {@code banqueId} vient du path
     * variable (pas du DTO, contrairement à {@link #buildFilterSummary}).
     */
    private List<String> buildEncoursFilterSummary(EncoursImpayeeSearchDto dto, Long banqueId) {
        List<String> lines = new ArrayList<>();

        try {
            lines.add("Banque : " + banqueService.findOrThrow(banqueId).getNom());
        } catch (ResourceNotFoundException e) {
            log.debug("Banque id={} introuvable pour le résumé de filtre export encours — ignorée", banqueId);
        }

        if (dto == null) return lines;

        if (dto.libelle() != null && !dto.libelle().isBlank())
            lines.add("Libellé : " + dto.libelle());
        if (dto.motif() != null)
            lines.add("Motif : " + dto.motif());
        if (dto.montant() != null)
            lines.add("Montant : " + formatMontantPdf(dto.montant()) + " MAD");

        return lines;
    }
}
