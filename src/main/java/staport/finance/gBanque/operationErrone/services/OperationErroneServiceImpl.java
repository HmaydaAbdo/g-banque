package staport.finance.gBanque.operationErrone.services;

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
import staport.finance.gBanque.operationErrone.dtos.*;
import staport.finance.gBanque.operationErrone.entities.OperationErrone;
import staport.finance.gBanque.operationErrone.mappers.OperationErroneMapper;
import staport.finance.gBanque.operationErrone.repositories.OperationErroneRepository;
import staport.finance.gBanque.operationErrone.specs.OperationErroneSpec;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.reports.service.StaportPdfReportService;
import staport.finance.gBanque.transactions.enums.TransactionSource;

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
 * Implémentation du service Opérations Erronées.
 *
 * <p>Encapsulation des domaines (architecture.md) : la résolution de la {@link Banque} passe
 * exclusivement par {@link IBanqueService} — jamais par injection directe de
 * {@code BanqueRepository}.
 *
 * <p>Suppression bloquée si déjà PAYE (règle transversale, {@code conventions/architecture.md},
 * "Règle 5") — {@code deleteOperationErrone} lève {@link ValidationException} dans ce cas.
 *
 * <p>Bug latent corrigé (14/07/2026) : {@code endOfMonth} était auparavant un champ calculé une
 * seule fois à la construction du bean singleton — figé à la date de démarrage du serveur au lieu
 * d'être recalculé à chaque appel. Recalculé maintenant localement dans
 * {@link #getOutstandingOperationErrone}, même correctif que celui appliqué à
 * {@code ImpayeeServiceImpl}/{@code RestitutionServiceImpl} lors des sweeps précédents.
 */
@Service
public class OperationErroneServiceImpl implements IOperationErroneService {

    private static final Logger log = LoggerFactory.getLogger(OperationErroneServiceImpl.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final OperationErroneRepository operationErroneRepository;
    private final IBanqueService banqueService;
    private final OperationErroneMapper operationErroneMapper;
    private final StaportPdfReportService staportPdfReportService;

    public OperationErroneServiceImpl(OperationErroneRepository operationErroneRepository,
                                       IBanqueService banqueService,
                                       OperationErroneMapper operationErroneMapper,
                                       StaportPdfReportService staportPdfReportService) {
        this.operationErroneRepository = operationErroneRepository;
        this.banqueService = banqueService;
        this.operationErroneMapper = operationErroneMapper;
        this.staportPdfReportService = staportPdfReportService;
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = OperationErrone.class)
    public OperationErroneResponseDto createOperationErrone(OperationErroneRequestDto requestDto) {
        log.info("Création d'une opération erronée - Banque ID: {}, Libellé: '{}', Montant: {}, Motif: {}",
                requestDto.banqueId(), requestDto.libelle(), requestDto.montant(), requestDto.motif());

        Banque banque = banqueService.findOrThrow(requestDto.banqueId());

        OperationErrone operationErrone = operationErroneMapper.toEntity(requestDto, banque);
        OperationErrone saved = operationErroneRepository.save(operationErrone);

        log.info("Opération erronée créée avec succès - ID: {}, Statut: {}, Motif: {}",
                saved.getId(), saved.getStatus(), saved.getMotif());
        return operationErroneMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OperationErroneResponseDto getById(Long id) {
        log.info("Récupération de l'opération erronée avec l'ID: {}", id);

        OperationErrone operationErrone = operationErroneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opération erronée non trouvée avec l'ID: " + id));

        return operationErroneMapper.toResponseDto(operationErrone);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OperationErroneResponseDto> searchOperationErrone(OperationErroneSearchDto searchDto, Pageable pageable) {
        log.info("Recherche d'opérations erronées - Critères: {}, Page: {}, Taille: {}",
                searchDto, pageable.getPageNumber(), pageable.getPageSize());

        Page<OperationErroneResponseDto> result = operationErroneRepository.findAll(
                        OperationErroneSpec.buildSpecification(searchDto), pageable)
                .map(operationErroneMapper::toResponseDto);

        log.info("Recherche terminée - {} opération(s) erronée(s) trouvée(s) sur {} au total",
                result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = OperationErrone.class)
    public OperationErroneResponseDto updateOperationErrone(Long id, OperationErroneRequestDto requestDto) {
        log.info("Mise à jour de l'opération erronée ID: {} - Nouveau libellé: '{}', Motif: {}",
                id, requestDto.libelle(), requestDto.motif());

        OperationErrone operationErrone = operationErroneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opération erronée non trouvée avec l'ID: " + id));

        Banque banque = banqueService.findOrThrow(requestDto.banqueId());

        operationErrone.setLibelle(requestDto.libelle());
        operationErrone.setMontant(requestDto.montant());
        operationErrone.setBanque(banque);
        operationErrone.setDateEmission(requestDto.dateEmission());
        operationErrone.setMotif(requestDto.motif());

        OperationErrone updated = operationErroneRepository.save(operationErrone);
        log.info("Opération erronée ID: {} mise à jour avec succès", id);
        return operationErroneMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = OperationErrone.class)
    public void deleteOperationErrone(Long id) {
        log.info("Suppression de l'opération erronée avec l'ID: {}", id);

        OperationErrone operationErrone = operationErroneRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Opération erronée non trouvée avec l'ID: {}", id);
                    return new ResourceNotFoundException("Opération erronée non trouvée avec l'ID: " + id);
                });

        if (operationErrone.getStatus() != Status.PREVU) {
            log.warn("Tentative de suppression d'une opération erronée déjà réglée — id: {}", id);
            throw new ValidationException("L'opération erronée id: " + id + " est déjà réglée et ne peut plus être supprimée.");
        }

        operationErroneRepository.deleteById(id);
        log.info("Opération erronée ID: {} supprimée avec succès", id);
    }

    @Override
    @Transactional(readOnly = true)
    public EncoursOperationErroneDto getOutstandingOperationErrone(EncoursOperationErroneSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Récupération des opérations erronées en cours - banqueId={}, Critères: {}", banqueId, searchDto);

        LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        Specification<OperationErrone> spec = Specification.where(OperationErroneSpec.outstandingUntil(endOfMonth))
                .and(OperationErroneSpec.hasBanque(banqueId))
                .and(OperationErroneSpec.hasLibelle(searchDto.libelle()))
                .and(OperationErroneSpec.hasMontant(searchDto.montant()))
                .and(OperationErroneSpec.hasMotif(searchDto.motif()));

        Page<OperationErrone> page = operationErroneRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));
        BigDecimal total = operationErroneRepository.findAll(spec).stream()
                .map(OperationErrone::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Encours calculé: {}, Nombre d'opérations erronées: {}", total, page.getTotalElements());

        return new EncoursOperationErroneDto(total, page.map(operationErroneMapper::toResponseDto));
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = OperationErrone.class)
    public OperationErroneResponseDto toggleStatus(Long id) {
        log.info("Basculement du statut de l'opération erronée ID: {}", id);

        OperationErrone operationErrone = operationErroneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opération erronée non trouvée avec l'ID: " + id));

        Status oldStatus = operationErrone.getStatus();
        Status newStatus = oldStatus == Status.PAYE ? Status.PREVU : Status.PAYE;

        operationErrone.setStatus(newStatus);
        OperationErrone updated = operationErroneRepository.save(operationErrone);

        log.info("Statut de l'opération erronée ID: {} basculé de {} vers {}", id, oldStatus, newStatus);
        return operationErroneMapper.toResponseDto(updated);
    }

    // ── Export PDF ────────────────────────────────────────────────────────────

    /**
     * Génère un export PDF de l'intégralité des opérations erronées correspondant aux critères
     * fournis (mêmes filtres que {@link #searchOperationErrone}) — sans pagination : le fichier
     * imprime la totalité des résultats filtrés, pas seulement la page affichée à l'écran.
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] exporterPdf(OperationErroneSearchDto searchDto) {
        log.info("Export PDF opérations erronées — critères: {}", searchDto);

        Specification<OperationErrone> spec = OperationErroneSpec.buildSpecification(searchDto);
        List<OperationErrone> operations = operationErroneRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "dateEmission"));

        BigDecimal totalMontant = operations.stream()
                .map(OperationErrone::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<String> filterLines = buildFilterSummary(searchDto);

        return genererPdf("État des Opérations Erronées", operations, totalMontant, filterLines);
    }

    /**
     * Génère un export PDF de l'encours d'opérations erronées d'une banque (mêmes critères que
     * {@link #getOutstandingOperationErrone}) — sans pagination : le fichier imprime la totalité
     * des résultats filtrés, pas seulement la page affichée à l'écran.
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] exporterEncoursPdf(EncoursOperationErroneSearchDto searchDto, Long banqueId) {
        log.info("Export PDF encours opérations erronées — banqueId={}", banqueId);

        LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        Specification<OperationErrone> spec = Specification.where(OperationErroneSpec.outstandingUntil(endOfMonth))
                .and(OperationErroneSpec.hasBanque(banqueId))
                .and(OperationErroneSpec.hasLibelle(searchDto.libelle()))
                .and(OperationErroneSpec.hasMontant(searchDto.montant()))
                .and(OperationErroneSpec.hasMotif(searchDto.motif()));

        List<OperationErrone> operations = operationErroneRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "dateEmission"));

        BigDecimal totalMontant = operations.stream()
                .map(OperationErrone::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<String> filterLines = buildEncoursFilterSummary(searchDto, banqueId);

        return genererPdf("Encours des Opérations Erronées", operations, totalMontant, filterLines);
    }

    /**
     * Construit le document PDF partagé par {@link #exporterPdf} et {@link #exporterEncoursPdf} —
     * même mise en page, seuls le titre, les lignes et le résumé de filtres varient.
     */
    private byte[] genererPdf(String title, List<OperationErrone> operations, BigDecimal totalMontant, List<String> filterLines) {
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
                                + "  •  " + operations.size() + " opération(s)";
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

                        float[] colWidths = {20f, 16f, 16f, 16f, 16f, 16f};
                        PdfPTable table = new PdfPTable(colWidths);
                        table.setWidthPercentage(100f);
                        table.setSpacingBefore(8f);
                        table.setHeaderRows(1);

                        for (String h : new String[]{
                                "Libellé", "Banque", "Motif", "Montant (MAD)", "Date émission", "Statut"}) {
                            PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                            c.setBackgroundColor(medBlue);
                            c.setPadding(5f);
                            c.setHorizontalAlignment(Element.ALIGN_CENTER);
                            c.setBorderColor(Color.WHITE);
                            c.setBorderWidth(0.5f);
                            table.addCell(c);
                        }

                        for (int i = 0; i < operations.size(); i++) {
                            OperationErrone o = operations.get(i);
                            Color rowBg = i % 2 == 0 ? Color.WHITE : rowAlt;

                            addPdfCell(table, o.getLibelle(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                            addPdfCell(table, o.getBanque() != null ? o.getBanque().getNom() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                            addPdfCell(table, libelleMotif(o.getMotif()), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                            addPdfCell(table, formatMontantPdf(o.getMontant()), cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);
                            addPdfCell(table, o.getDateEmission() != null ? o.getDateEmission().format(DATE_FMT) : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);

                            boolean paye = o.getStatus() == Status.PAYE;
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
                                new Phrase("TOTAL — " + operations.size() + " opération(s)", totalFont));
                        totalLabel.setColspan(3);
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
                        throw new RuntimeException("Erreur génération contenu PDF opérations erronées", e);
                    }
                }
            );
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des opérations erronées", e);
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

    /** Libellé lisible d'un motif — même règle de formatage que le frontend (`formatMotifLabel`). */
    private String libelleMotif(TransactionSource motif) {
        if (motif == null) return "—";
        String[] mots = motif.name().replace('_', ' ').toLowerCase(Locale.FRANCE).split(" ");
        StringBuilder sb = new StringBuilder();
        for (String mot : mots) {
            if (mot.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(mot.charAt(0))).append(mot.substring(1));
        }
        return sb.toString();
    }

    private List<String> buildFilterSummary(OperationErroneSearchDto dto) {
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
        if (dto.motif() != null)
            lines.add("Motif : " + libelleMotif(dto.motif()));

        return lines;
    }

    /**
     * Résumé des filtres pour l'export PDF de l'écran encours — {@code banqueId} vient du path
     * variable (pas du DTO, contrairement à {@link #buildFilterSummary}).
     */
    private List<String> buildEncoursFilterSummary(EncoursOperationErroneSearchDto dto, Long banqueId) {
        List<String> lines = new ArrayList<>();

        try {
            lines.add("Banque : " + banqueService.findOrThrow(banqueId).getNom());
        } catch (ResourceNotFoundException e) {
            log.debug("Banque id={} introuvable pour le résumé de filtre export encours — ignorée", banqueId);
        }

        if (dto == null) return lines;

        if (dto.libelle() != null && !dto.libelle().isBlank())
            lines.add("Libellé : " + dto.libelle());
        if (dto.montant() != null)
            lines.add("Montant : " + formatMontantPdf(dto.montant()) + " MAD");
        if (dto.motif() != null)
            lines.add("Motif : " + libelleMotif(dto.motif()));

        return lines;
    }
}
