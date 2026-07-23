package staport.finance.gBanque.cmt.echeances.services;


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
import staport.finance.gBanque.cmt.echeances.dtos.*;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;
import staport.finance.gBanque.cmt.echeances.mappers.EcheanceCMTMapper;
import staport.finance.gBanque.cmt.echeances.repositories.EcheanceCMTRepository;
import staport.finance.gBanque.cmt.echeances.specification.EcheanceCMTSpecification;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.reports.service.StaportPdfReportService;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * Service pour la gestion des échéances d'un crédit à moyen terme (CMT).
 */
@Service
@Transactional(readOnly = true)
public class EcheanceCMTService {

    private static final Logger log = LoggerFactory.getLogger(EcheanceCMTService.class);
    private static final int SCALE = 2;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color MED_BLUE = Color.decode("#2563eb");
    private static final Color LIGHT_BLUE = Color.decode("#dbeafe");
    private static final Color ROW_ALT = Color.decode("#f8fafc");
    private static final Color GREEN = Color.decode("#16a34a");
    private static final Color ORANGE = Color.decode("#ea580c");
    private static final Color BORDER_GRAY = Color.decode("#e5e7eb");

    private final EcheanceCMTRepository echeanceCmtRepository;
    private final IBanqueService banqueService;
    private final EcheanceCMTMapper echeanceCmtMapper;
    private final StaportPdfReportService staportPdfReportService;

    public EcheanceCMTService(EcheanceCMTRepository echeanceCmtRepository,
                              IBanqueService banqueService,
                              EcheanceCMTMapper echeanceCmtMapper,
                              StaportPdfReportService staportPdfReportService) {
        this.echeanceCmtRepository = echeanceCmtRepository;
        this.banqueService = banqueService;
        this.echeanceCmtMapper = echeanceCmtMapper;
        this.staportPdfReportService = staportPdfReportService;
    }

    @Transactional
    public void generateEcheances(CreditMoyenTerme credit) {
        LocalDate dateDebut = credit.getDateDebut();
        List<EcheanceCMT> echeances = new ArrayList<>();

        for (int i = 1; i <= credit.getDuree(); i++) {
            LocalDate dateEcheanceCourante = dateDebut.plusMonths((long) (i - 1) * credit.getPeriodicite());

            EcheanceCMT echeance = new EcheanceCMT();
            echeance.setCreditMoyenTerme(credit);
            echeance.setNumeroEcheance(i);
            echeance.setDateEcheance(dateEcheanceCourante);
            echeance.setMontant(credit.getEcheance());
            echeance.setStatutPaiement(StatutEcheanceCMT.PREVU);
            echeance.setLibelle(credit.getBanque().getNom() + ", crédit CMT \"" + credit.getDesignation() + "\"");
            echeance.setObservation("Prélèvement CMT - échéance n°" + i + "/" + credit.getDuree()
                    + " - crédit \"" + credit.getDesignation() + "\"");
            echeances.add(echeance);
        }

        echeanceCmtRepository.saveAll(echeances);
        log.info("{} échéance(s) générée(s) pour le crédit CMT ID: {}", echeances.size(), credit.getId());
    }

    @Transactional
    public void regenerateEcheances(CreditMoyenTerme credit) {
        if (hasPaidEcheances(credit.getId())) {
            throw new ValidationException("Impossible de régénérer les échéances car certaines ont déjà été payées.");
        }

        echeanceCmtRepository.deleteByCreditMoyenTerme(credit);
        generateEcheances(credit);
        log.info("Échéances régénérées pour le crédit CMT ID: {}", credit.getId());
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = EcheanceCMT.class)
    public EcheanceCMTResponseDto toggleStatus(Long echeanceId) {
        EcheanceCMT echeance = echeanceCmtRepository.findById(echeanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Echeance avec l'ID " + echeanceId + " introuvable."));

        StatutEcheanceCMT newStatus = echeance.getStatutPaiement() == StatutEcheanceCMT.PAYE
                ? StatutEcheanceCMT.PREVU
                : StatutEcheanceCMT.PAYE;

        echeance.setStatutPaiement(newStatus);
        echeance = echeanceCmtRepository.save(echeance);

        log.info("Statut de l'échéance CMT ID: {} modifié — nouveau statut: {}", echeanceId, newStatus);
        return echeanceCmtMapper.toResponseDto(echeance);
    }

    /**
     * Met à jour les champs d'une échéance dont le statut est {@code PREVU}.
     * Toute tentative de modification d'une échéance {@code PAYE} lève une {@link ValidationException}.
     *
     * @param id  L'identifiant de l'échéance à modifier
     * @param dto Les nouvelles valeurs des champs
     * @return Le DTO de l'échéance mise à jour
     */
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = EcheanceCMT.class)
    public EcheanceCMTResponseDto updateEcheance(Long id, EcheanceCMTUpdateRequestDto dto) {
        EcheanceCMT echeance = echeanceCmtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Echeance avec l'ID " + id + " introuvable."));

        if (echeance.getStatutPaiement() == StatutEcheanceCMT.PAYE) {
            log.warn("Tentative de modification d'une échéance CMT déjà payée — ID: {}", id);
            throw new ValidationException("Impossible de modifier une échéance dont le statut est PAYE.");
        }

        echeance.setDateEcheance(dto.dateEcheance());
        echeance.setMontant(dto.montant());
        echeance.setDateOperation(dto.dateOperation());
        echeance.setDateValeur(dto.dateValeur());
        if (dto.observation() != null) echeance.setObservation(dto.observation());
        if (dto.libelle() != null) echeance.setLibelle(dto.libelle());

        EcheanceCMT saved = echeanceCmtRepository.save(echeance);
        log.info("Échéance CMT ID: {} mise à jour avec succès", id);
        return echeanceCmtMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public TableauAmortissementCMTResponseDto getTableauAmortissementForCMT(CreditMoyenTerme credit, EcheanceCMTSearchDto searchDto, Pageable pageable) {
        Long creditId = credit.getId();
        List<EcheanceCMT> allEcheancesForTotals = echeanceCmtRepository.findByCreditMoyenTerme(credit);

        BigDecimal montantTotalAPayer = allEcheancesForTotals.stream()
                .map(EcheanceCMT::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal montantTotalDejaPaye = allEcheancesForTotals.stream()
                .filter(e -> e.getStatutPaiement() == StatutEcheanceCMT.PAYE)
                .map(EcheanceCMT::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal montantRestantAPayer = montantTotalAPayer.subtract(montantTotalDejaPaye);

        EcheanceCMTSearchDto effectiveSearchDto = (searchDto == null
                ? new EcheanceCMTSearchDto(creditId, null, null, null, null)
                : searchDto.withCreditMoyenTermeId(creditId));
        Specification<EcheanceCMT> spec = EcheanceCMTSpecification.searchByCriteria(effectiveSearchDto);

        Page<EcheanceCMT> echeancesPage = echeanceCmtRepository.findAll(spec, defaultSort(pageable));
        Page<EcheanceCMTResponseDto> echeancesDtoPage = echeancesPage.map(echeanceCmtMapper::toResponseDto);

        log.info("Tableau d'amortissement récupéré pour le crédit CMT ID: {} — {} échéance(s)", creditId, echeancesPage.getTotalElements());

        return new TableauAmortissementCMTResponseDto(
                credit.getId(),
                credit.getDesignation(),
                montantTotalAPayer,
                montantTotalDejaPaye,
                montantRestantAPayer,
                echeancesDtoPage
        );
    }

    public boolean hasPaidEcheances(Long creditId) {
        return echeanceCmtRepository.existsByCreditMoyenTermeIdAndStatutPaiement(creditId, StatutEcheanceCMT.PAYE);
    }

    @Transactional(readOnly = true)
    public EncoursCMTResponseDTO getEncoursCMTForBanque(EncoursCMTRequestDTO requestDTO, Pageable pageable) {
        Banque banque = banqueService.findOrThrow(requestDTO.banqueId());

        LocalDate finDeMoisCourant = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        Specification<EcheanceCMT> spec = EcheanceCMTSpecification.byCreditBanqueId(requestDTO.banqueId())
                .and(EcheanceCMTSpecification.outstandingUntil(finDeMoisCourant))
                .and(EcheanceCMTSpecification.byCreditDesignationLike(requestDTO.designation()))
                .and(EcheanceCMTSpecification.byMontant(requestDTO.montant()));

        Page<EcheanceCMT> encoursEcheancesPage = echeanceCmtRepository.findAll(spec, defaultSort(pageable));
        Page<EcheanceCMTResponseDto> echeancesDtoPage = encoursEcheancesPage.map(echeanceCmtMapper::toResponseDto);

        List<EcheanceCMT> allOutstandingEcheances = echeanceCmtRepository.findAll(spec);
        BigDecimal totalEncours = allOutstandingEcheances.stream()
                .map(EcheanceCMT::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RoundingMode.HALF_UP);

        log.info("Encours CMT calculés — banqueId: {}, total: {}", banque.getId(), totalEncours);

        return new EncoursCMTResponseDTO(
                banque.getId(),
                banque.getNom(),
                totalEncours,
                echeancesDtoPage
        );
    }


    /**
     * Supprime toutes les échéances d'un crédit à moyen terme.
     * Utilisé lors de la suppression complète d'un crédit CMT.
     *
     * @param credit Le crédit dont les échéances doivent être supprimées
     * @return Le nombre d'échéances supprimées
     */
    @Transactional
    public int deleteEcheancesByCreditMoyenTerme(CreditMoyenTerme credit) {
        List<EcheanceCMT> echeances = echeanceCmtRepository.findByCreditMoyenTerme(credit);
        int nombreEcheances = echeances.size();

        echeanceCmtRepository.deleteByCreditMoyenTerme(credit);
        log.info("{} échéance(s) supprimée(s) pour le crédit CMT ID : {}", nombreEcheances, credit.getId());

        return nombreEcheances;
    }

    private Pageable defaultSort(Pageable pageable) {
        return pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));
    }

    /**
     * Exporte en PDF l'encours d'échéances CMT d'une banque (mêmes critères que
     * {@link #getEncoursCMTForBanque}, sans pagination).
     */
    @Transactional(readOnly = true)
    public byte[] exportEncoursCMTPdf(EncoursCMTRequestDTO requestDTO) {
        Banque banque = banqueService.findOrThrow(requestDTO.banqueId());

        LocalDate finDeMoisCourant = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        Specification<EcheanceCMT> spec = EcheanceCMTSpecification.byCreditBanqueId(requestDTO.banqueId())
                .and(EcheanceCMTSpecification.outstandingUntil(finDeMoisCourant))
                .and(EcheanceCMTSpecification.byCreditDesignationLike(requestDTO.designation()))
                .and(EcheanceCMTSpecification.byMontant(requestDTO.montant()));

        List<EcheanceCMT> echeances = echeanceCmtRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "dateEcheance"));
        BigDecimal totalEncours = echeances.stream()
                .map(EcheanceCMT::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RoundingMode.HALF_UP);

        List<String> filterLines = new ArrayList<>();
        filterLines.add("Banque : " + banque.getNom());
        if (requestDTO.designation() != null && !requestDTO.designation().isBlank()) {
            filterLines.add("Désignation : " + requestDTO.designation());
        }
        if (requestDTO.montant() != null) {
            filterLines.add("Montant : " + formatMontantPdf(requestDTO.montant()) + " MAD");
        }

        String title = "Encours d'Échéances CMT — " + banque.getNom();

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
                            + "  •  " + echeances.size() + " échéance(s)";
                    if (!filterLines.isEmpty()) {
                        exportInfo += "  •  Filtres : " + String.join("  |  ", filterLines);
                    }
                    Paragraph sub = new Paragraph(exportInfo, subFont);
                    sub.setAlignment(Element.ALIGN_CENTER);
                    sub.setSpacingAfter(8f);
                    doc.add(sub);

                    float[] colWidths = {26f, 12f, 15f, 15f, 12f};
                    PdfPTable table = new PdfPTable(colWidths);
                    table.setWidthPercentage(100f);
                    table.setSpacingBefore(8f);
                    table.setHeaderRows(1);

                    for (String h : new String[]{"Crédit", "N° Échéance", "Date d'échéance", "Montant (MAD)", "Statut"}) {
                        PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                        c.setBackgroundColor(MED_BLUE);
                        c.setPadding(5f);
                        c.setHorizontalAlignment(Element.ALIGN_CENTER);
                        c.setBorderColor(Color.WHITE);
                        c.setBorderWidth(0.5f);
                        table.addCell(c);
                    }

                    for (int i = 0; i < echeances.size(); i++) {
                        EcheanceCMT e = echeances.get(i);
                        Color rowBg = i % 2 == 0 ? Color.WHITE : ROW_ALT;

                        addPdfCell(table, e.getCreditMoyenTerme() != null ? e.getCreditMoyenTerme().getDesignation() : "—", cellFont, rowBg, BORDER_GRAY, Element.ALIGN_LEFT);
                        addPdfCell(table, String.valueOf(e.getNumeroEcheance()), cellFont, rowBg, BORDER_GRAY, Element.ALIGN_CENTER);
                        addPdfCell(table, e.getDateEcheance() != null ? e.getDateEcheance().format(DATE_FMT) : "—", cellFont, rowBg, BORDER_GRAY, Element.ALIGN_CENTER);
                        addPdfCell(table, formatMontantPdf(e.getMontant()), cellFont, rowBg, BORDER_GRAY, Element.ALIGN_RIGHT);

                        boolean paye = e.getStatutPaiement() == StatutEcheanceCMT.PAYE;
                        PdfPCell statusCell = new PdfPCell(new Phrase(
                                paye ? "PAYÉ" : "PRÉVU",
                                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
                        statusCell.setBackgroundColor(paye ? GREEN : ORANGE);
                        statusCell.setPadding(4f);
                        statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        statusCell.setBorderColor(BORDER_GRAY);
                        statusCell.setBorderWidth(0.5f);
                        table.addCell(statusCell);
                    }

                    PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL — " + echeances.size() + " échéance(s)", totalFont));
                    totalLabel.setColspan(3);
                    totalLabel.setBackgroundColor(LIGHT_BLUE);
                    totalLabel.setPadding(5f);
                    totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalLabel.setBorderColor(MED_BLUE);
                    table.addCell(totalLabel);

                    PdfPCell totalVal = new PdfPCell(new Phrase(formatMontantPdf(totalEncours) + " MAD", totalFont));
                    totalVal.setBackgroundColor(LIGHT_BLUE);
                    totalVal.setPadding(5f);
                    totalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalVal.setBorderColor(MED_BLUE);
                    table.addCell(totalVal);

                    PdfPCell empty = new PdfPCell(new Phrase(""));
                    empty.setBackgroundColor(LIGHT_BLUE);
                    empty.setBorderColor(MED_BLUE);
                    table.addCell(empty);

                    doc.add(table);
                } catch (DocumentException e) {
                    throw new RuntimeException("Erreur lors de la génération du contenu du PDF de l'encours CMT.", e);
                }
            });
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF de l'encours CMT.", e);
        }
    }

    // ========================== EXPORT PDF ==========================

    /**
     * Exporte en PDF l'échéancier complet (filtré, sans pagination) d'un crédit à moyen
     * terme, avec les mêmes totaux (Endettement / Payé / Restant) que ceux affichés à
     * l'écran du tableau d'amortissement.
     */
    @Transactional(readOnly = true)
    public byte[] exportTableauAmortissementPdf(CreditMoyenTerme credit, EcheanceCMTSearchDto searchDto) {
        log.info("Export PDF du tableau d'amortissement — crédit CMT ID: {}", credit.getId());

        List<EcheanceCMT> allEcheancesForTotals = echeanceCmtRepository.findByCreditMoyenTerme(credit);

        BigDecimal montantTotalAPayer = allEcheancesForTotals.stream()
                .map(EcheanceCMT::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal montantTotalDejaPaye = allEcheancesForTotals.stream()
                .filter(e -> e.getStatutPaiement() == StatutEcheanceCMT.PAYE)
                .map(EcheanceCMT::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal montantRestantAPayer = montantTotalAPayer.subtract(montantTotalDejaPaye);

        EcheanceCMTSearchDto effectiveSearchDto = (searchDto == null
                ? new EcheanceCMTSearchDto(credit.getId(), null, null, null, null)
                : searchDto.withCreditMoyenTermeId(credit.getId()));
        Specification<EcheanceCMT> spec = EcheanceCMTSpecification.searchByCriteria(effectiveSearchDto);
        List<EcheanceCMT> echeances = echeanceCmtRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "numeroEcheance"));

        List<String> filterLines = buildFilterSummary(searchDto);
        String title = "Tableau d'Amortissement — " + credit.getDesignation();

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
                            + "  •  " + echeances.size() + " échéance(s)";
                    if (!filterLines.isEmpty()) {
                        exportInfo += "  •  Filtres : " + String.join("  |  ", filterLines);
                    }
                    Paragraph sub = new Paragraph(exportInfo, subFont);
                    sub.setAlignment(Element.ALIGN_CENTER);
                    sub.setSpacingAfter(6f);
                    doc.add(sub);

                    // Résumé Endettement / Payé / Restant, identique à l'écran
                    PdfPTable summaryTable = new PdfPTable(3);
                    summaryTable.setWidthPercentage(100f);
                    summaryTable.setSpacingAfter(10f);
                    addSummaryCell(summaryTable, "Endettement", formatMontantPdf(montantTotalAPayer), totalFont, subFont);
                    addSummaryCell(summaryTable, "Montant déjà payé", formatMontantPdf(montantTotalDejaPaye), totalFont, subFont);
                    addSummaryCell(summaryTable, "Montant restant à payer", formatMontantPdf(montantRestantAPayer), totalFont, subFont);
                    doc.add(summaryTable);

                    float[] colWidths = {10f, 15f, 15f, 12f};
                    PdfPTable table = new PdfPTable(colWidths);
                    table.setWidthPercentage(100f);
                    table.setSpacingBefore(4f);
                    table.setHeaderRows(1);

                    for (String h : new String[]{"N° Échéance", "Date d'échéance", "Montant TTC (MAD)", "Statut"}) {
                        PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                        c.setBackgroundColor(MED_BLUE);
                        c.setPadding(5f);
                        c.setHorizontalAlignment(Element.ALIGN_CENTER);
                        c.setBorderColor(Color.WHITE);
                        c.setBorderWidth(0.5f);
                        table.addCell(c);
                    }

                    for (int i = 0; i < echeances.size(); i++) {
                        EcheanceCMT e = echeances.get(i);
                        Color rowBg = i % 2 == 0 ? Color.WHITE : ROW_ALT;

                        addPdfCell(table, String.valueOf(e.getNumeroEcheance()), cellFont, rowBg, BORDER_GRAY, Element.ALIGN_CENTER);
                        addPdfCell(table, e.getDateEcheance() != null ? e.getDateEcheance().format(DATE_FMT) : "—", cellFont, rowBg, BORDER_GRAY, Element.ALIGN_CENTER);
                        addPdfCell(table, formatMontantPdf(e.getMontant()), cellFont, rowBg, BORDER_GRAY, Element.ALIGN_RIGHT);

                        boolean paye = e.getStatutPaiement() == StatutEcheanceCMT.PAYE;
                        PdfPCell statusCell = new PdfPCell(new Phrase(
                                paye ? "PAYÉ" : "PRÉVU",
                                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
                        statusCell.setBackgroundColor(paye ? GREEN : ORANGE);
                        statusCell.setPadding(4f);
                        statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        statusCell.setBorderColor(BORDER_GRAY);
                        statusCell.setBorderWidth(0.5f);
                        table.addCell(statusCell);
                    }

                    doc.add(table);
                } catch (DocumentException e) {
                    throw new RuntimeException("Erreur lors de la génération du contenu du PDF du tableau d'amortissement.", e);
                }
            });
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF du tableau d'amortissement.", e);
        }
    }

    private void addSummaryCell(PdfPTable table, String label, String value, Font valueFont, Font labelFont) {
        Paragraph content = new Paragraph();
        content.add(new Phrase(label + "\n", labelFont));
        content.add(new Phrase(value + " MAD", valueFont));
        PdfPCell cell = new PdfPCell(content);
        cell.setBackgroundColor(LIGHT_BLUE);
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorderColor(MED_BLUE);
        table.addCell(cell);
    }

    private List<String> buildFilterSummary(EcheanceCMTSearchDto dto) {
        List<String> lines = new ArrayList<>();
        if (dto == null) return lines;

        if (dto.numeroEcheance() != null) lines.add("N° Échéance : " + dto.numeroEcheance());
        if (dto.month() != null) lines.add("Mois : " + dto.month());
        if (dto.year() != null) lines.add("Année : " + dto.year());
        if (dto.statutPaiement() != null) lines.add("Statut : " + dto.statutPaiement());
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

    private String formatMontantPdf(BigDecimal montant) {
        if (montant == null) return "—";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        return formatter.format(montant);
    }
}
