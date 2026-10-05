package staport.finance.gBanque.moyensDePaiements.miseADisposition.services;

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
import staport.finance.gBanque.moyensDePaiements.miseADisposition.dtos.*;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.exceptions.MiseADispositionBusinessException;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.mapper.MiseADispositionMapper;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.metrics.MiseADispositionMetrics;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.repositories.MiseADispositionRepository;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.specs.MiseADispositionSpec;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.reports.service.StaportPdfReportService;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.reports.utils.MontantFormatter;

/**
 * Service métier du module Mise à Disposition.
 *
 * <p>Façade publique du domaine {@code miseADisposition} (architecture.md — Encapsulation des
 * domaines) : les autres modules doivent passer par ce service, jamais par
 * {@code MiseADispositionRepository} directement.
 */
@Service
public class MiseADispositionService {

    private static final Logger log = LoggerFactory.getLogger(MiseADispositionService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final MiseADispositionRepository miseADispositionRepository;
    private final IBanqueService banqueService;
    private final MiseADispositionMapper miseADispositionMapper;
    private final MiseADispositionMetrics miseADispositionMetrics;
    private final StaportPdfReportService staportPdfReportService;

    public MiseADispositionService(MiseADispositionRepository miseADispositionRepository, IBanqueService banqueService,
                                    MiseADispositionMapper miseADispositionMapper, MiseADispositionMetrics miseADispositionMetrics,
                                    StaportPdfReportService staportPdfReportService) {
        this.miseADispositionRepository = miseADispositionRepository;
        this.banqueService = banqueService;
        this.miseADispositionMapper = miseADispositionMapper;
        this.miseADispositionMetrics = miseADispositionMetrics;
        this.staportPdfReportService = staportPdfReportService;
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = MiseADisposition.class)
    public MiseADispositionResponseDto createEncaissementMiseAdisposition(MiseAdispositionRequestDto dto) {
        log.info("Création d'une nouvelle mise à disposition d'encaissement: {}", dto);

        Banque banque = banqueService.findOrThrow(dto.getBanqueId());
        MiseADisposition miseADisposition = miseADispositionMapper.toEntity(dto, banque);

        log.debug("Enregistrement de l'entité mise à disposition: {}", miseADisposition);
        miseADisposition = miseADispositionRepository.save(miseADisposition);
        miseADispositionMetrics.incrementerCreation();

        log.info("Mise à disposition créée avec succès avec l'ID: {}", miseADisposition.getId());
        return toDto(miseADisposition);
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = MiseADisposition.class)
    public MiseADispositionResponseDto editEncaissementMiseAdisposition(Long id, MiseAdispositionRequestDto dto) {
        log.info("Modification de la mise à disposition d'encaissement ID={} avec les données={}", id, dto);

        MiseADisposition existingMiseAdisposition = trouverOuLeverException(id);

        if (existingMiseAdisposition.getType() != Type.ENCAISSEMENT) {
            log.warn("Tentative de modification d'une mise à disposition qui n'est pas de type ENCAISSEMENT, ID={}", id);
            throw new MiseADispositionBusinessException("Modification interdite: la MiseADisposition with id " + id + " n'est pas de type ENCAISSEMENT.");
        }

        if (existingMiseAdisposition.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de modification d'une mise à disposition déjà réglée, ID={}", id);
            throw new MiseADispositionBusinessException("Modification interdite: la MiseADisposition with id " + id + " est déjà réglée.");
        }

        if (dto.getDateEmission() != null) {
            existingMiseAdisposition.setDateEmission(dto.getDateEmission());
        }
        if (dto.getBanqueId() != null) {
            existingMiseAdisposition.setBanque(banqueService.findOrThrow(dto.getBanqueId()));
        }
        if (dto.getMontant() != null) {
            existingMiseAdisposition.setMontant(dto.getMontant());
        }
        if (dto.getEmetteur() != null) {
            existingMiseAdisposition.setEmetteur(dto.getEmetteur());
        }

        existingMiseAdisposition = miseADispositionRepository.save(existingMiseAdisposition);
        log.info("Mise à disposition ID={} mise à jour avec succès", id);
        return toDto(existingMiseAdisposition);
    }

    @Transactional(readOnly = true)
    public MiseADispositionResponseDto getMiseAdispositionById(Long id) {
        log.debug("Récupération de la mise à disposition avec l'ID={}", id);
        return toDto(trouverOuLeverException(id));
    }

    /**
     * Supprime une mise à disposition saisie manuellement.
     *
     * <p>Une mise à disposition issue de la synchronisation FunPaiement ({@code idPaiement}
     * renseigné) ne peut pas être supprimée : le lecteur de synchro relit tout l'historique à chaque
     * exécution et la seule clé d'idempotence est {@code findByIdPaiement}. Supprimer la ligne efface
     * cette clé, et la synchro suivante la recrée à l'identique. Pour retirer une telle ligne des
     * encours, l'annuler plutôt que la supprimer : une ligne existante n'est jamais recréée ni
     * écrasée par la synchro. Même règle que {@code VirementService.deleteVirement} (boucle
     * constatée en production sur les virements en juillet 2026).
     */
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = MiseADisposition.class)
    public void deleteMiseAdispositionById(Long id) {
        log.info("Suppression de la mise à disposition avec l'ID={}", id);

        MiseADisposition miseADisposition = trouverOuLeverException(id);

        if (miseADisposition.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de suppression d'une mise à disposition déjà réglée, ID={}", id);
            throw new MiseADispositionBusinessException("Suppression interdite: la mise à dispo with id " + id + " est déjà réglée.");
        }

        if (miseADisposition.getIdPaiement() != null) {
            log.warn("Tentative de suppression d'une mise à disposition synchronisée, ID={}, idPaiement={}",
                    id, miseADisposition.getIdPaiement());
            throw new MiseADispositionBusinessException(
                    "Suppression interdite: la mise à disposition " + id + " provient de la synchronisation FunPaiement "
                            + "et serait recréée à la prochaine synchronisation. Annulez-la plutôt que de la supprimer.");
        }

        miseADispositionRepository.deleteById(id);
        log.info("Mise à disposition ID={} supprimée avec succès", id);
    }

    @Transactional(readOnly = true)
    public Page<MiseADispositionResponseDto> getDecaissementForMiseAdisposition(MiseADispositionSearchDto searchDto, Pageable pageable) {
        log.debug("Recherche des mises à disposition de décaissement avec les filtres={} et pagination={}", searchDto, pageable);

        Specification<MiseADisposition> spec = MiseADispositionSpec.bySearchDto(searchDto)
                .and(MiseADispositionSpec.hasType(Type.DECAISSEMENT));
        Page<MiseADisposition> miseADispositionPage = miseADispositionRepository.findAll(spec, stablePageable(pageable));

        log.debug("{} mise(s) à disposition de décaissement trouvée(s)", miseADispositionPage.getTotalElements());
        return miseADispositionPage.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<MiseADispositionResponseDto> getEncaissementForMiseAdisposition(MiseADispositionSearchDto searchDto, Pageable pageable) {
        log.debug("Recherche des mises à disposition d'encaissement avec les filtres={} et pagination={}", searchDto, pageable);

        Specification<MiseADisposition> spec = MiseADispositionSpec.bySearchDto(searchDto)
                .and(MiseADispositionSpec.hasType(Type.ENCAISSEMENT));
        Page<MiseADisposition> miseADispositionPage = miseADispositionRepository.findAll(spec, stablePageable(pageable));

        log.debug("{} mise(s) à disposition d'encaissement trouvée(s)", miseADispositionPage.getTotalElements());
        return miseADispositionPage.map(this::toDto);
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = MiseADisposition.class)
    public MiseADispositionResponseDto toggleMiseADispositionStatus(Long id) {
        log.info("Basculement du statut pour la mise à disposition ID={}", id);

        MiseADisposition miseADisposition = trouverOuLeverException(id);

        if (miseADisposition.getStatut() == StatutMoyenPaiement.ANNULE) {
            log.warn("Tentative de basculement du statut d'une mise à disposition annulée, ID={}", id);
            throw new MiseADispositionBusinessException("Impossible de basculer le statut d'une mise à disposition annulée (ID=" + id + ").");
        }
        miseADisposition.setStatut(miseADisposition.getStatut() == StatutMoyenPaiement.ENCOURS ? StatutMoyenPaiement.REGLE : StatutMoyenPaiement.ENCOURS);
        miseADisposition = miseADispositionRepository.save(miseADisposition);
        if (miseADisposition.getStatut() == StatutMoyenPaiement.REGLE) {
            miseADispositionMetrics.incrementerPassageRegle();
        }

        log.info("Statut basculé avec succès pour la mise à disposition ID={}", id);
        return toDto(miseADisposition);
    }

    // ── Export PDF ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] exportDecaissementsPdf(MiseADispositionSearchDto searchDto) {
        Specification<MiseADisposition> spec = MiseADispositionSpec.bySearchDto(searchDto)
                .and(MiseADispositionSpec.hasType(Type.DECAISSEMENT));

        List<MiseADisposition> mads = miseADispositionRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "dateEmission"));
        BigDecimal total = sumMontant(spec);
        List<String> filterLines = buildFilterSummary(searchDto);

        return generateMisesADispositionPdf("État des Mises à Disposition de Décaissement", mads, total, filterLines);
    }

    private BigDecimal sumMontant(Specification<MiseADisposition> spec) {
        return miseADispositionRepository.findAll(spec).stream()
                .map(MiseADisposition::getMontant)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private byte[] generateMisesADispositionPdf(String title, List<MiseADisposition> mads, BigDecimal totalMontant, List<String> filterLines) {
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
                                    + "  •  " + mads.size() + " mise(s) à disposition";
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

                            float[] colWidths = {12f, 14f, 14f, 13f, 12f, 11f, 9f};
                            PdfPTable table = new PdfPTable(colWidths);
                            table.setWidthPercentage(100f);
                            table.setSpacingBefore(8f);
                            table.setHeaderRows(1);

                            for (String h : new String[]{
                                    "N° MAD", "Banque", "Bénéficiaire",
                                    "Montant (MAD)", "Date émission", "CIN bénéficiaire", "Statut"}) {
                                PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                                c.setBackgroundColor(medBlue);
                                c.setPadding(5f);
                                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                                c.setBorderColor(Color.WHITE);
                                c.setBorderWidth(0.5f);
                                table.addCell(c);
                            }

                            for (int i = 0; i < mads.size(); i++) {
                                MiseADisposition m = mads.get(i);
                                Color rowBg = i % 2 == 0 ? Color.WHITE : rowAlt;

                                addPdfCell(table, m.getNumero(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, m.getBanque() != null ? m.getBanque().getNom() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, m.getBeneficiaire(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, MontantFormatter.format(m.getMontant()), cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);
                                addPdfCell(table, m.getDateEmission() != null ? m.getDateEmission().format(DATE_FMT) : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);
                                addPdfCell(table, m.getCinBeneficiaire(), cellFont, rowBg, borderGray, Element.ALIGN_CENTER);

                                boolean regle = m.getStatut() == StatutMoyenPaiement.REGLE;
                                boolean annule = m.getStatut() == StatutMoyenPaiement.ANNULE;
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

                            PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL — " + mads.size() + " mise(s) à disposition", totalFont));
                            totalLabel.setColspan(3);
                            totalLabel.setBackgroundColor(lightBlue);
                            totalLabel.setPadding(5f);
                            totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                            totalLabel.setBorderColor(medBlue);
                            table.addCell(totalLabel);

                            PdfPCell totalVal = new PdfPCell(new Phrase(MontantFormatter.format(totalMontant) + " MAD", totalFont));
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
                            throw new RuntimeException("Erreur génération contenu PDF mises à disposition", e);
                        }
                    }
            );
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des mises à disposition", e);
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

    private List<String> buildFilterSummary(MiseADispositionSearchDto dto) {
        List<String> lines = new ArrayList<>();
        if (dto == null) return lines;
        if (dto.numero() != null && !dto.numero().isBlank())
            lines.add("N° MAD : " + dto.numero());
        if (dto.banqueName() != null && !dto.banqueName().isBlank())
            lines.add("Banque : " + dto.banqueName());
        if (dto.beneficiaire() != null && !dto.beneficiaire().isBlank())
            lines.add("Bénéficiaire : " + dto.beneficiaire());
        if (dto.cinBeneficiaire() != null && !dto.cinBeneficiaire().isBlank())
            lines.add("CIN bénéficiaire : " + dto.cinBeneficiaire());
        if (dto.emetteur() != null && !dto.emetteur().isBlank())
            lines.add("Émetteur : " + dto.emetteur());
        if (dto.statut() != null)
            lines.add("Statut : " + dto.statut());
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

    private MiseADisposition trouverOuLeverException(Long id) {
        return miseADispositionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mise à disposition with id " + id + " not found."));
    }

    private Pageable stablePageable(Pageable pageable) {
        return pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));
    }

    /**
     * Mappe l'entité vers le DTO puis applique les champs d'affichage calculés (règle métier
     * propre au service — jamais dans le Mapper, cf. architecture.md) : observation (" MAD " +
     * numéro) et libellé (bénéficiaire) — comportement identique à l'ancien mapper statique.
     */
    private MiseADispositionResponseDto toDto(MiseADisposition miseADisposition) {
        MiseADispositionResponseDto dto = miseADispositionMapper.toDto(miseADisposition);
        dto.setObservation(" MAD " + miseADisposition.getNumero());
        dto.setLibelle(miseADisposition.getBeneficiaire());
        return dto;
    }
}
