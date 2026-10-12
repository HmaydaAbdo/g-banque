package staport.finance.gBanque.prelevements.service;

import java.util.Set;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.ReverseTransactionService;
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
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prelevements.dtos.PrelevementRequestDto;
import staport.finance.gBanque.prelevements.dtos.PrelevementResponseDto;
import staport.finance.gBanque.prelevements.dtos.PrelevementSearchDto;
import staport.finance.gBanque.prelevements.entitie.Prelevement;
import staport.finance.gBanque.prelevements.exceptions.PrelevementBusinessException;
import staport.finance.gBanque.prelevements.mapper.PrelevementMapper;
import staport.finance.gBanque.prelevements.metrics.PrelevementMetrics;
import staport.finance.gBanque.prelevements.repository.PrelevementRepository;
import staport.finance.gBanque.prelevements.spec.PrelevementSpec;
import staport.finance.gBanque.reports.service.StaportPdfReportService;
import staport.finance.gBanque.reports.utils.MontantFormatter;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class PrelevementService implements IPrelevementService {

    private static final Logger log = LoggerFactory.getLogger(PrelevementService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final PrelevementRepository prelevementRepository;
    private final IBanqueService banqueService;
    private final PrelevementMapper prelevementMapper;
    private final PrelevementMetrics prelevementMetrics;
    private final StaportPdfReportService staportPdfReportService;
    private final ReverseTransactionService reverseTransactionService;

    public PrelevementService(PrelevementRepository prelevementRepository, IBanqueService banqueService, PrelevementMapper prelevementMapper,
                               PrelevementMetrics prelevementMetrics, StaportPdfReportService staportPdfReportService,
                               ReverseTransactionService reverseTransactionService) {
        this.reverseTransactionService = reverseTransactionService;
        this.prelevementRepository = prelevementRepository;
        this.banqueService = banqueService;
        this.prelevementMapper = prelevementMapper;
        this.prelevementMetrics = prelevementMetrics;
        this.staportPdfReportService = staportPdfReportService;
        log.info("Initialisation du service Prelevement");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsChargeFixeGeneree(Long banqueId, String libelle, LocalDate debut, LocalDate fin) {
        return prelevementRepository.existsByBanqueIdAndLibelleAndDateEmissionBetween(banqueId, libelle, debut, fin);
    }

    /**
     * Crée un nouveau prélèvement
     *
     * @param requestDto Données du prélèvement à créer
     * @return Le prélèvement créé
     */
    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = Prelevement.class, idAccessor = "getId")
    public PrelevementResponseDto createPrelevement(PrelevementRequestDto requestDto) {
        log.info("Création d'un prélèvement - Banque ID: {}, Libellé: '{}', Montant: {}",
                requestDto.getBanqueId(), requestDto.getLibelle(), requestDto.getMontant());

        Banque banque = banqueService.findOrThrow(requestDto.getBanqueId());

        log.debug("Banque trouvée: {} (ID: {})", banque.getNom(), banque.getId());

        Prelevement prelevement = prelevementMapper.toEntity(requestDto, banque);
        Prelevement savedPrelevement = prelevementRepository.save(prelevement);
        prelevementMetrics.incrementerCreation();

        log.info("Prélèvement créé avec succès - ID: {}, Statut: {}", savedPrelevement.getId(), savedPrelevement.getStatus());
        return prelevementMapper.toResponseDto(savedPrelevement);
    }

    /**
     * Récupère un prélèvement par son ID
     *
     * @param id Identifiant du prélèvement
     * @return Le prélèvement demandé
     */
    @Transactional(readOnly = true)
    public PrelevementResponseDto getById(Long id) {
        log.info("Récupération du prélèvement avec l'ID: {}", id);

        Prelevement prelevement = prelevementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Prélèvement non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Prélèvement non trouvé avec l'ID: " + id);
                });

        log.debug("Prélèvement trouvé - Libellé: '{}', Montant: {}, Statut: {}",
                prelevement.getLibelle(), prelevement.getMontant(), prelevement.getStatus());
        return prelevementMapper.toResponseDto(prelevement);
    }

    /**
     * Recherche des prélèvements selon des critères avec pagination
     *
     * @param searchDto Critères de recherche
     * @param pageable Informations de pagination
     * @return Page de prélèvements correspondant aux critères
     */
    @Transactional(readOnly = true)
    public Page<PrelevementResponseDto> searchPrelevements(PrelevementSearchDto searchDto, Pageable pageable) {
        log.info("Recherche de prélèvements avec critères: {}, Page: {}, Taille: {}",
                searchDto, pageable.getPageNumber(), pageable.getPageSize());

        Page<PrelevementResponseDto> result = prelevementRepository.findAll(
                        PrelevementSpec.buildSpecification(searchDto),
                        pageable
                )
                .map(prelevementMapper::toResponseDto);

        log.info("Recherche terminée - {} prélèvement(s) trouvé(s) sur {} au total",
                result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    /**
     * Met à jour un prélèvement existant
     *
     * @param id Identifiant du prélèvement à modifier
     * @param requestDto Nouvelles données du prélèvement
     * @return Le prélèvement mis à jour
     */
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Prelevement.class, idAccessor = "getId")
    public PrelevementResponseDto updatePrelevement(Long id, PrelevementRequestDto requestDto) {
        log.info("Mise à jour du prélèvement ID: {} - Nouveau libellé: '{}', Nouveau montant: {}",
                id, requestDto.getLibelle(), requestDto.getMontant());

        Prelevement prelevement = prelevementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Prélèvement non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Prélèvement non trouvé avec l'ID: " + id);
                });

        log.debug("Prélèvement actuel avant modification - Libellé: '{}', Montant: {}, Statut: {}",
                prelevement.getLibelle(), prelevement.getMontant(), prelevement.getStatus());

        if (prelevement.getStatus() == Status.PAYE) {
            log.warn("Tentative de modification d'un prélèvement déjà payé — id: {}", id);
            throw new PrelevementBusinessException("Modification interdite : le prélèvement id " + id + " est déjà payé.");
        }

        Banque banque = banqueService.findOrThrow(requestDto.getBanqueId());

        prelevement.setLibelle(requestDto.getLibelle());
        prelevement.setMontant(requestDto.getMontant());
        prelevement.setBanque(banque);
        prelevement.setDateEmission(requestDto.getDateEmission());

        Prelevement updatedPrelevement = prelevementRepository.save(prelevement);
        log.info("Prélèvement ID: {} mis à jour avec succès", id);
        return prelevementMapper.toResponseDto(updatedPrelevement);
    }

    /**
     * Supprime un prélèvement
     *
     * @param id Identifiant du prélèvement à supprimer
     */
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = Prelevement.class)
    public void deletePrelevement(Long id) {
        log.info("Suppression du prélèvement avec l'ID: {}", id);

        Prelevement prelevement = prelevementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Prélèvement non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Prélèvement non trouvé avec l'ID: " + id);
                });

        if (prelevement.getStatus() == Status.PAYE) {
            log.warn("Tentative de suppression d'un prélèvement déjà payé — id: {}", id);
            throw new PrelevementBusinessException("Suppression interdite : le prélèvement id " + id + " est déjà payé.");
        }

        prelevementRepository.deleteById(id);
        log.info("Prélèvement ID: {} supprimé avec succès", id);
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Prelevement.class, idAccessor = "getId")
    public PrelevementResponseDto toggleStatus(Long id) {
        log.info("Basculement du statut du prélèvement ID: {}", id);

        Prelevement prelevement = prelevementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Prélèvement non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Prélèvement non trouvé avec l'ID: " + id);
                });

        // Retour « en cours » d'une opération déjà rapprochée : sa transaction est annulée (même effet que l'annulation
        // de la validation) ; sans transaction liée, la bascule reste libre (correction manuelle).
        if (prelevement.getStatus() == Status.PAYE
                && reverseTransactionService.annulerSiRapproche(Set.of(TransactionSource.PRELEVEMENT), id).effectuee()) {
            return prelevementMapper.toResponseDto(prelevement);
        }
        Status oldStatus = prelevement.getStatus();
        Status newStatus=oldStatus==Status.PAYE ? Status.PREVU : Status.PAYE;

        prelevement.setStatus(newStatus);
        Prelevement updatedPrelevement = prelevementRepository.save(prelevement);
        if (newStatus == Status.PAYE) {
            prelevementMetrics.incrementerPassagePaye();
        }

        log.info("Statut du prélèvement ID: {} basculé avec succès de {} vers {}",
                id, oldStatus, newStatus);

        return prelevementMapper.toResponseDto(updatedPrelevement);
    }

    // ── Export PDF ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] exportPdf(PrelevementSearchDto searchDto) {
        Specification<Prelevement> spec = PrelevementSpec.buildSpecification(searchDto);

        List<Prelevement> prelevements = prelevementRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "dateEmission"));
        BigDecimal total = sumMontant(spec);
        List<String> filterLines = buildFilterSummary(searchDto);

        return generatePrelevementPdf("État des Prélèvements", prelevements, total, filterLines);
    }

    private BigDecimal sumMontant(Specification<Prelevement> spec) {
        return prelevementRepository.findAll(spec).stream()
                .map(Prelevement::getMontant)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private byte[] generatePrelevementPdf(String title, List<Prelevement> prelevements, BigDecimal totalMontant, List<String> filterLines) {
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
                                    + "  •  " + prelevements.size() + " prélèvement(s)";
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
                            Color borderGray = Color.decode("#e5e7eb");

                            float[] colWidths = {24f, 16f, 16f, 14f, 12f};
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

                            for (int i = 0; i < prelevements.size(); i++) {
                                Prelevement p = prelevements.get(i);
                                Color rowBg = i % 2 == 0 ? Color.WHITE : rowAlt;

                                addPdfCell(table, p.getLibelle(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, p.getBanque() != null ? p.getBanque().getNom() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, MontantFormatter.format(p.getMontant()), cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);
                                addPdfCell(table, p.getDateEmission() != null ? p.getDateEmission().format(DATE_FMT) : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);

                                boolean paye = p.getStatus() == Status.PAYE;
                                PdfPCell statusCell = new PdfPCell(new Phrase(
                                        paye ? "PAYÉ" : "PRÉVU",
                                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
                                statusCell.setBackgroundColor(paye ? green : Color.decode("#ea580c"));
                                statusCell.setPadding(4f);
                                statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                                statusCell.setBorderColor(borderGray);
                                statusCell.setBorderWidth(0.5f);
                                table.addCell(statusCell);
                            }

                            PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL — " + prelevements.size() + " prélèvement(s)", totalFont));
                            totalLabel.setColspan(2);
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
                            throw new RuntimeException("Erreur génération contenu PDF prélèvements", e);
                        }
                    }
            );
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des prélèvements", e);
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

    private List<String> buildFilterSummary(PrelevementSearchDto dto) {
        List<String> lines = new ArrayList<>();
        if (dto == null) return lines;
        if (dto.getBanqueId() != null) {
            try {
                lines.add("Banque : " + banqueService.findOrThrow(dto.getBanqueId()).getNom());
            } catch (ResourceNotFoundException ignored) {
                // Filtre obsolète (banque supprimée depuis) — ignoré silencieusement dans le résumé PDF.
            }
        }
        if (dto.getLibelle() != null && !dto.getLibelle().isBlank())
            lines.add("Libellé : " + dto.getLibelle());
        if (dto.getStatus() != null)
            lines.add("Statut : " + dto.getStatus());
        if (dto.getMontant() != null)
            lines.add("Montant : " + MontantFormatter.format(dto.getMontant()) + " MAD");
        return lines;
    }
}