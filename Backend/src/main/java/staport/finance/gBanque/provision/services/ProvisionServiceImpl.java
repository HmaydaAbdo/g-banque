package staport.finance.gBanque.provision.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import staport.finance.gBanque.provision.dtos.*;
import staport.finance.gBanque.provision.entities.Provision;
import staport.finance.gBanque.provision.mappers.ProvisionMapper;
import staport.finance.gBanque.provision.repositories.ProvisionRepository;
import staport.finance.gBanque.provision.spec.ProvisionSpecification;
import staport.finance.gBanque.reports.service.StaportPdfReportService;
import staport.finance.gBanque.transactions.enums.SensOperation;
import staport.finance.gBanque.reports.utils.MontantFormatter;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Implémentation du service Provision.
 *
 * <p>Encapsulation des domaines (architecture.md) : la résolution de la {@link Banque} passe
 * exclusivement par {@link IBanqueService} — jamais par injection directe de
 * {@code BanqueRepository}.
 *
 * <p>Suppression bloquée si déjà PAYE (règle transversale, {@code conventions/architecture.md},
 * "Règle 5") — {@code deleteProvision} lève {@link ValidationException} dans ce cas.
 */
@Service
public class ProvisionServiceImpl implements IProvisionService {

    private static final Logger log = LoggerFactory.getLogger(ProvisionServiceImpl.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ProvisionRepository provisionRepository;
    private final IBanqueService banqueService;
    private final ProvisionMapper provisionMapper;
    private final StaportPdfReportService staportPdfReportService;

    public ProvisionServiceImpl(ProvisionRepository provisionRepository,
                                 IBanqueService banqueService,
                                 ProvisionMapper provisionMapper,
                                 StaportPdfReportService staportPdfReportService) {
        this.provisionRepository = provisionRepository;
        this.banqueService = banqueService;
        this.provisionMapper = provisionMapper;
        this.staportPdfReportService = staportPdfReportService;
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = Provision.class)
    public ProvisionResponseDto createProvision(ProvisionRequestDto requestDto) {
        log.info("Création d'une provision - Banque ID: {}, Libellé: '{}', Montant: {}, Motif: {}, Sens: {}",
                requestDto.banqueId(), requestDto.libelle(), requestDto.montant(), requestDto.motif(), requestDto.sens());

        Banque banque = banqueService.findOrThrow(requestDto.banqueId());

        Provision provision = provisionMapper.toEntity(requestDto, banque);
        Provision saved = provisionRepository.save(provision);

        log.info("Provision créée avec succès - ID: {}, Statut: {}, Motif: {}, Sens: {}",
                saved.getId(), saved.getStatus(), saved.getMotif(), saved.getSens());
        return provisionMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProvisionResponseDto getById(Long id) {
        log.info("Récupération de la provision avec l'ID: {}", id);

        Provision provision = provisionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provision non trouvée avec l'ID: " + id));

        return provisionMapper.toResponseDto(provision);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProvisionResponseDto> searchProvision(ProvisionSearchDto searchDto, Pageable pageable) {
        log.info("Recherche de provisions - Critères: {}, Page: {}, Taille: {}",
                searchDto, pageable.getPageNumber(), pageable.getPageSize());

        Page<ProvisionResponseDto> result = provisionRepository.findAll(
                        ProvisionSpecification.searchByCriteria(searchDto), pageable)
                .map(provisionMapper::toResponseDto);

        log.info("Recherche terminée - {} provision(s) trouvée(s) sur {} au total",
                result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Provision.class)
    public ProvisionResponseDto updateProvision(Long id, ProvisionRequestDto requestDto) {
        log.info("Mise à jour de la provision ID: {} - Nouveau libellé: '{}', Motif: {}, Sens: {}",
                id, requestDto.libelle(), requestDto.motif(), requestDto.sens());

        Provision provision = provisionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provision non trouvée avec l'ID: " + id));

        Banque banque = banqueService.findOrThrow(requestDto.banqueId());

        provision.setLibelle(requestDto.libelle());
        provision.setObservation(requestDto.observation());
        provision.setMontant(requestDto.montant());
        provision.setBanque(banque);
        provision.setDateEmission(requestDto.dateEmission());
        provision.setMotif(requestDto.motif());
        provision.setSens(requestDto.sens());

        Provision updated = provisionRepository.save(provision);
        log.info("Provision ID: {} mise à jour avec succès", id);
        return provisionMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = Provision.class)
    public void deleteProvision(Long id) {
        log.info("Suppression de la provision avec l'ID: {}", id);

        Provision provision = provisionRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Provision non trouvée avec l'ID: {}", id);
                    return new ResourceNotFoundException("Provision non trouvée avec l'ID: " + id);
                });

        if (provision.getStatus() != Status.PREVU) {
            log.warn("Tentative de suppression d'une provision déjà réglée — id: {}", id);
            throw new ValidationException("La provision id: " + id + " est déjà réglée et ne peut plus être supprimée.");
        }

        provisionRepository.deleteById(id);
        log.info("Provision ID: {} supprimée avec succès", id);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Provision.class)
    public ProvisionResponseDto toggleStatus(Long id) {
        log.info("Basculement du statut de la provision ID: {}", id);

        Provision provision = provisionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provision non trouvée avec l'ID: " + id));

        Status oldStatus = provision.getStatus();
        Status newStatus = oldStatus == Status.PAYE ? Status.PREVU : Status.PAYE;

        provision.setStatus(newStatus);
        Provision updated = provisionRepository.save(provision);

        log.info("Statut de la provision ID: {} basculé de {} vers {}", id, oldStatus, newStatus);
        return provisionMapper.toResponseDto(updated);
    }

    // ── Export PDF ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] exporterPdf(ProvisionSearchDto searchDto) {
        log.info("Export PDF provisions — critères: {}", searchDto);

        Specification<Provision> spec = ProvisionSpecification.searchByCriteria(searchDto);
        List<Provision> provisions = provisionRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "dateEmission"));

        BigDecimal totalMontant = totalSigne(provisions);
        List<String> filterLines = buildFilterSummary(searchDto);

        return genererPdf("État des Provisions", provisions, totalMontant, filterLines);
    }

    /** Somme des montants — un décaissement compte négativement dans le total affiché. */
    private BigDecimal totalSigne(List<Provision> provisions) {
        return provisions.stream()
                .map(p -> p.getSens() == SensOperation.DECAISSEMENT ? p.getMontant().negate() : p.getMontant())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private byte[] genererPdf(String title, List<Provision> provisions, BigDecimal totalMontant, List<String> filterLines) {
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
                                + "  •  " + provisions.size() + " provision(s)";
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

                        float[] colWidths = {18f, 14f, 14f, 12f, 14f, 14f, 14f};
                        PdfPTable table = new PdfPTable(colWidths);
                        table.setWidthPercentage(100f);
                        table.setSpacingBefore(8f);
                        table.setHeaderRows(1);

                        for (String h : new String[]{
                                "Libellé", "Banque", "Motif", "Sens", "Montant (MAD)", "Date émission", "Statut"}) {
                            PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                            c.setBackgroundColor(medBlue);
                            c.setPadding(5f);
                            c.setHorizontalAlignment(Element.ALIGN_CENTER);
                            c.setBorderColor(Color.WHITE);
                            c.setBorderWidth(0.5f);
                            table.addCell(c);
                        }

                        for (int i = 0; i < provisions.size(); i++) {
                            Provision p = provisions.get(i);
                            Color rowBg = i % 2 == 0 ? Color.WHITE : rowAlt;

                            addPdfCell(table, p.getLibelle(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                            addPdfCell(table, p.getBanque() != null ? p.getBanque().getNom() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                            addPdfCell(table, libelleEnum(p.getMotif()), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                            addPdfCell(table, p.getSens() == SensOperation.ENCAISSEMENT ? "Encaissement" : "Décaissement", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                            addPdfCell(table, MontantFormatter.format(p.getMontant()), cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);
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

                        PdfPCell totalLabel = new PdfPCell(
                                new Phrase("TOTAL (encaissements − décaissements) — " + provisions.size() + " provision(s)", totalFont));
                        totalLabel.setColspan(4);
                        totalLabel.setBackgroundColor(lightBlue);
                        totalLabel.setPadding(5f);
                        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        totalLabel.setBorderColor(medBlue);
                        table.addCell(totalLabel);

                        PdfPCell totalVal = new PdfPCell(
                                new Phrase(MontantFormatter.format(totalMontant) + " MAD", totalFont));
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
                        throw new RuntimeException("Erreur génération contenu PDF provisions", e);
                    }
                }
            );
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des provisions", e);
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

    /** Libellé lisible d'une valeur d'enum — même règle de formatage que le frontend (`formatMotifLabel`). */
    private String libelleEnum(Enum<?> value) {
        if (value == null) return "—";
        String[] mots = value.name().replace('_', ' ').toLowerCase(Locale.FRANCE).split(" ");
        StringBuilder sb = new StringBuilder();
        for (String mot : mots) {
            if (mot.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(mot.charAt(0))).append(mot.substring(1));
        }
        return sb.toString();
    }

    private List<String> buildFilterSummary(ProvisionSearchDto dto) {
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
            lines.add("Montant : " + MontantFormatter.format(dto.montant()) + " MAD");
        if (dto.status() != null)
            lines.add("Statut : " + (dto.status() == Status.PAYE ? "Payée" : "Prévue"));
        if (dto.motif() != null)
            lines.add("Motif : " + libelleEnum(dto.motif()));
        if (dto.sens() != null)
            lines.add("Sens : " + libelleEnum(dto.sens()));
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
