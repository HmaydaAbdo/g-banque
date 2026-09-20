package staport.finance.gBanque.moyensDePaiements.virementInternational.service;

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
import staport.finance.gBanque.moyensDePaiements.virementInternational.dtos.*;
import staport.finance.gBanque.moyensDePaiements.virementInternational.entities.VirementInternational;
import staport.finance.gBanque.moyensDePaiements.virementInternational.exceptions.VirementInternationalBusinessException;
import staport.finance.gBanque.moyensDePaiements.virementInternational.mapper.VirementInternationalMapper;
import staport.finance.gBanque.moyensDePaiements.virementInternational.metrics.VirementInternationalMetrics;
import staport.finance.gBanque.moyensDePaiements.virementInternational.repositories.VirementInternationalRepository;
import staport.finance.gBanque.moyensDePaiements.virementInternational.specs.VirementInternationalSpec;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
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
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

/**
 * Service métier du module Virement International.
 *
 * <p>Façade publique du domaine {@code virementInternational} (architecture.md — Encapsulation
 * des domaines) : les autres modules doivent passer par ce service, jamais par
 * {@code VirementInternationalRepository} directement.
 *
 * <p>Particularité métier : contrairement à Chèque/LCN/Virement, ce module n'a aucune
 * synchronisation FunPaiement — encaissements et décaissements sont tous deux saisis
 * manuellement (aucun {@code VirementInternationalPaymentProcessor} n'existe dans le module
 * {@code synchronisation}).
 */
@Service
public class VirementInternationalService {

    private static final Logger log = LoggerFactory.getLogger(VirementInternationalService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final VirementInternationalRepository virementInternationalRepository;
    private final IBanqueService banqueService;
    private final VirementInternationalMapper virementInternationalMapper;
    private final VirementInternationalMetrics virementInternationalMetrics;
    private final StaportPdfReportService staportPdfReportService;

    public VirementInternationalService(
            VirementInternationalRepository virementInternationalRepository,
            IBanqueService banqueService,
            VirementInternationalMapper virementInternationalMapper,
            VirementInternationalMetrics virementInternationalMetrics,
            StaportPdfReportService staportPdfReportService) {
        this.virementInternationalRepository = virementInternationalRepository;
        this.banqueService = banqueService;
        this.virementInternationalMapper = virementInternationalMapper;
        this.virementInternationalMetrics = virementInternationalMetrics;
        this.staportPdfReportService = staportPdfReportService;
    }

    /**
     * Crée un nouveau virement international (ENCAISSEMENT ou DECAISSEMENT).
     */
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = VirementInternational.class)
    public VirementInternationalResponseDto createVirementInternational(VirementInternationalRequestDto dto) {
        log.info("Création d'un nouveau virement international de type {}: {}", dto.getType(), dto);

        Banque banque = banqueService.findOrThrow(dto.getBanqueId());
        VirementInternational virement = virementInternationalMapper.toEntity(dto, banque);
        virement = virementInternationalRepository.save(virement);
        virementInternationalMetrics.incrementerCreation(virement.getType());

        log.info("Virement international créé avec succès avec l'ID: {} (Type: {})", virement.getId(), virement.getType());
        return virementInternationalMapper.toDto(virement);
    }

    /**
     * Modifie un virement international existant. La modification n'est autorisée que si le
     * virement n'est pas déjà réglé.
     */
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = VirementInternational.class)
    public VirementInternationalResponseDto editVirementInternational(Long id, VirementInternationalRequestDto dto) {
        log.info("Modification du virement international ID={} avec les données={}", id, dto);

        VirementInternational existingVirement = trouverOuLeverException(id);

        if (existingVirement.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de modification d'un virement international déjà réglé, ID={}", id);
            throw new VirementInternationalBusinessException("Modification interdite: le virement international with id " + id + " est déjà réglé.");
        }

        Banque banque = dto.getBanqueId() != null && !dto.getBanqueId().equals(existingVirement.getBanque().getId())
                ? banqueService.findOrThrow(dto.getBanqueId())
                : existingVirement.getBanque();

        VirementInternational updatedVirement = virementInternationalMapper.updateEntity(existingVirement, dto, banque);

        updatedVirement = virementInternationalRepository.save(updatedVirement);
        log.info("Virement international ID={} mis à jour avec succès", id);
        return virementInternationalMapper.toDto(updatedVirement);
    }

    @Transactional(readOnly = true)
    public VirementInternationalResponseDto getVirementInternationalById(Long id) {
        log.debug("Récupération du virement international avec l'ID={}", id);
        return virementInternationalMapper.toDto(trouverOuLeverException(id));
    }

    @Transactional(readOnly = true)
    public Page<VirementInternationalResponseDto> getVirementsInternationaux(
            VirementInternationalSearchDto searchDto, Pageable pageable) {
        log.debug("Recherche des virements internationaux avec les filtres={} et pagination={}", searchDto, pageable);

        Specification<VirementInternational> spec = VirementInternationalSpec.bySearchDto(searchDto);
        Page<VirementInternational> virementPage = virementInternationalRepository.findAll(spec, stablePageable(pageable));

        log.debug("{} virement(s) international(aux) trouvé(s)", virementPage.getTotalElements());
        return virementPage.map(virementInternationalMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<VirementInternationalResponseDto> getEncaissementVirementsInternationaux(
            VirementInternationalSearchDto searchDto, Pageable pageable) {
        log.debug("Recherche des virements internationaux d'encaissement avec les filtres={} et pagination={}",
                searchDto, pageable);

        Specification<VirementInternational> spec = VirementInternationalSpec.bySearchDto(searchDto)
                .and(VirementInternationalSpec.hasType(Type.ENCAISSEMENT));
        Page<VirementInternational> virementPage = virementInternationalRepository.findAll(spec, stablePageable(pageable));

        log.debug("{} virement(s) international(aux) d'encaissement trouvé(s)", virementPage.getTotalElements());
        return virementPage.map(virementInternationalMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<VirementInternationalResponseDto> getDecaissementVirementsInternationaux(
            VirementInternationalSearchDto searchDto, Pageable pageable) {
        log.debug("Recherche des virements internationaux de décaissement avec les filtres={} et pagination={}",
                searchDto, pageable);

        Specification<VirementInternational> spec = VirementInternationalSpec.bySearchDto(searchDto)
                .and(VirementInternationalSpec.hasType(Type.DECAISSEMENT));
        Page<VirementInternational> virementPage = virementInternationalRepository.findAll(spec, stablePageable(pageable));

        log.debug("{} virement(s) international(aux) de décaissement trouvé(s)", virementPage.getTotalElements());
        return virementPage.map(virementInternationalMapper::toDto);
    }

    /**
     * Bascule le statut ENCOURS/REGLE d'un virement international.
     */
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = VirementInternational.class)
    public VirementInternationalResponseDto toggleVirementStatus(Long id) {
        log.info("Basculement du statut pour le virement international ID={}", id);

        VirementInternational virement = trouverOuLeverException(id);

        if (virement.getStatut() == StatutMoyenPaiement.ANNULE) {
            log.warn("Tentative de basculement du statut d'un virement international annulé, ID={}", id);
            throw new VirementInternationalBusinessException("Impossible de basculer le statut d'un virement international annulé (ID=" + id + ").");
        }
        virement.setStatut(virement.getStatut() == StatutMoyenPaiement.ENCOURS ? StatutMoyenPaiement.REGLE : StatutMoyenPaiement.ENCOURS);
        virement = virementInternationalRepository.save(virement);
        if (virement.getStatut() == StatutMoyenPaiement.REGLE) {
            virementInternationalMetrics.incrementerPassageRegle();
        }

        log.info("Statut basculé avec succès pour le virement international ID={} — nouveau statut={}", id, virement.getStatut());
        return virementInternationalMapper.toDto(virement);
    }

    /**
     * Supprime un virement international. La suppression n'est autorisée que si le virement n'est
     * pas déjà réglé.
     */
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = VirementInternational.class)
    public void deleteVirementInternational(Long id) {
        log.info("Suppression du virement international ID={}", id);

        VirementInternational virement = trouverOuLeverException(id);

        if (virement.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de suppression d'un virement international déjà réglé, ID={}", id);
            throw new VirementInternationalBusinessException("Suppression interdite: le virement international with id " + id + " est déjà réglé.");
        }

        virementInternationalRepository.delete(virement);
        log.info("Virement international ID={} supprimé avec succès", id);
    }

    // ── Export PDF ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] exportPdf(VirementInternationalSearchDto searchDto) {
        Specification<VirementInternational> spec = VirementInternationalSpec.bySearchDto(searchDto);

        List<VirementInternational> virements = virementInternationalRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "dateEmission"));
        BigDecimal total = sumMontant(spec);
        List<String> filterLines = buildFilterSummary(searchDto);

        return generateVirementInternationalPdf("État des Virements Internationaux", virements, total, filterLines);
    }

    private BigDecimal sumMontant(Specification<VirementInternational> spec) {
        return virementInternationalRepository.findAll(spec).stream()
                .map(VirementInternational::getMontant)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private byte[] generateVirementInternationalPdf(String title, List<VirementInternational> virements, BigDecimal totalMontant, List<String> filterLines) {
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
                                    + "  •  " + virements.size() + " virement(s)";
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

                            float[] colWidths = {14f, 14f, 14f, 13f, 12f, 11f, 11f};
                            PdfPTable table = new PdfPTable(colWidths);
                            table.setWidthPercentage(100f);
                            table.setSpacingBefore(8f);
                            table.setHeaderRows(1);

                            for (String h : new String[]{
                                    "Banque", "Émetteur", "Bénéficiaire",
                                    "Montant (MAD)", "Date émission", "Type", "Statut"}) {
                                PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                                c.setBackgroundColor(medBlue);
                                c.setPadding(5f);
                                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                                c.setBorderColor(Color.WHITE);
                                c.setBorderWidth(0.5f);
                                table.addCell(c);
                            }

                            for (int i = 0; i < virements.size(); i++) {
                                VirementInternational v = virements.get(i);
                                Color rowBg = i % 2 == 0 ? Color.WHITE : rowAlt;

                                addPdfCell(table, v.getBanque() != null ? v.getBanque().getNom() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, v.getEmetteur(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, v.getBeneficiaire(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, formatMontantPdf(v.getMontant()), cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);
                                addPdfCell(table, v.getDateEmission() != null ? v.getDateEmission().format(DATE_FMT) : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);
                                addPdfCell(table, v.getType() != null ? v.getType().toString() : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);

                                boolean regle = v.getStatut() == StatutMoyenPaiement.REGLE;
                                boolean annule = v.getStatut() == StatutMoyenPaiement.ANNULE;
                                PdfPCell statusCell = new PdfPCell(new Phrase(
                                        regle ? "RÉGLÉ" : (annule ? "ANNULÉ" : "ENCOURS"),
                                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
                                statusCell.setBackgroundColor(regle ? green : (annule ? Color.GRAY : orange));
                                statusCell.setPadding(4f);
                                statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                                statusCell.setBorderColor(borderGray);
                                statusCell.setBorderWidth(0.5f);
                                table.addCell(statusCell);
                            }

                            PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL — " + virements.size() + " virement(s)", totalFont));
                            totalLabel.setColspan(3);
                            totalLabel.setBackgroundColor(lightBlue);
                            totalLabel.setPadding(5f);
                            totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                            totalLabel.setBorderColor(medBlue);
                            table.addCell(totalLabel);

                            PdfPCell totalVal = new PdfPCell(new Phrase(formatMontantPdf(totalMontant) + " MAD", totalFont));
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
                            throw new RuntimeException("Erreur génération contenu PDF virements internationaux", e);
                        }
                    }
            );
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des virements internationaux", e);
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

    private String formatMontantPdf(BigDecimal montant) {
        if (montant == null) return "—";
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.FRANCE);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(montant);
    }

    private List<String> buildFilterSummary(VirementInternationalSearchDto dto) {
        List<String> lines = new ArrayList<>();
        if (dto == null) return lines;
        if (dto.banqueId() != null) {
            try {
                lines.add("Banque : " + banqueService.findOrThrow(dto.banqueId()).getNom());
            } catch (ResourceNotFoundException ignored) {
                // Filtre obsolète (banque supprimée depuis) — ignoré silencieusement dans le résumé PDF.
            }
        }
        if (dto.beneficiaire() != null && !dto.beneficiaire().isBlank())
            lines.add("Bénéficiaire : " + dto.beneficiaire());
        if (dto.emetteur() != null && !dto.emetteur().isBlank())
            lines.add("Émetteur : " + dto.emetteur());
        if (dto.type() != null)
            lines.add("Type : " + dto.type());
        if (dto.statut() != null)
            lines.add("Statut : " + dto.statut());
        if (dto.montant() != null)
            lines.add("Montant : " + formatMontantPdf(dto.montant()) + " MAD");
        if (dto.dateEmissionFrom() != null || dto.dateEmissionTo() != null) {
            String from = dto.dateEmissionFrom() != null ? dto.dateEmissionFrom().format(DATE_FMT) : "…";
            String to   = dto.dateEmissionTo()   != null ? dto.dateEmissionTo().format(DATE_FMT)   : "…";
            lines.add("Date émission : " + from + " → " + to);
        }
        if (dto.dateOperationFrom() != null || dto.dateOperationTo() != null) {
            String from = dto.dateOperationFrom() != null ? dto.dateOperationFrom().format(DATE_FMT) : "…";
            String to   = dto.dateOperationTo()   != null ? dto.dateOperationTo().format(DATE_FMT)   : "…";
            lines.add("Date opération : " + from + " → " + to);
        }
        if (dto.dateValeurFrom() != null || dto.dateValeurTo() != null) {
            String from = dto.dateValeurFrom() != null ? dto.dateValeurFrom().format(DATE_FMT) : "…";
            String to   = dto.dateValeurTo()   != null ? dto.dateValeurTo().format(DATE_FMT)   : "…";
            lines.add("Date valeur : " + from + " → " + to);
        }
        return lines;
    }

    // ── Helpers internes ──────────────────────────────────────────────────────

    private VirementInternational trouverOuLeverException(Long id) {
        return virementInternationalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VirementInternational with id " + id + " not found."));
    }

    private Pageable stablePageable(Pageable pageable) {
        return pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));
    }
}
