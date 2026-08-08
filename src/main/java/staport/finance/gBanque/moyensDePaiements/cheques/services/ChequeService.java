package staport.finance.gBanque.moyensDePaiements.cheques.services;

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
import staport.finance.gBanque.moyensDePaiements.cheques.dtos.*;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.moyensDePaiements.cheques.exceptions.ChequeBusinessException;
import staport.finance.gBanque.moyensDePaiements.cheques.mappers.ChequeMapper;
import staport.finance.gBanque.moyensDePaiements.cheques.metrics.ChequeMetrics;
import staport.finance.gBanque.moyensDePaiements.cheques.repositories.ChequeRepository;
import staport.finance.gBanque.moyensDePaiements.cheques.specs.ChequeSpec;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
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
 * Service métier du module Chèque.
 *
 * <p>Façade publique du domaine {@code cheques} (architecture.md — Encapsulation des domaines) :
 * les autres modules doivent passer par ce service, jamais par {@code ChequeRepository} directement.
 */
@Service
public class ChequeService {

    private static final Logger log = LoggerFactory.getLogger(ChequeService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ChequeRepository chequeRepository;
    private final IBanqueService banqueService;
    private final ChequeMapper chequeMapper;
    private final ChequeMetrics chequeMetrics;
    private final StaportPdfReportService staportPdfReportService;

    public ChequeService(ChequeRepository chequeRepository, IBanqueService banqueService, ChequeMapper chequeMapper,
                          ChequeMetrics chequeMetrics, StaportPdfReportService staportPdfReportService) {
        this.chequeRepository = chequeRepository;
        this.banqueService = banqueService;
        this.chequeMapper = chequeMapper;
        this.chequeMetrics = chequeMetrics;
        this.staportPdfReportService = staportPdfReportService;
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = Cheque.class)
    public ChequeResponseDto createEncaissementCheque(ChequeRequestDto dto) {
        log.info("Création d'un chèque d'encaissement — numero: {}", dto.getNumero());

        Banque banque = banqueService.findOrThrow(dto.getBanqueId());
        Cheque cheque = chequeMapper.toEntity(dto, banque);
        cheque = chequeRepository.save(cheque);
        chequeMetrics.incrementerCreation(cheque.getType());

        log.info("Chèque créé — id: {}", cheque.getId());
        return toDto(cheque);
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Cheque.class)
    public ChequeResponseDto editEncaissementCheque(Long id, ChequeRequestDto dto) {
        log.info("Modification chèque encaissement — id: {}", id);

        Cheque existingCheque = trouverOuLeverException(id);

        if (existingCheque.getType() != Type.ENCAISSEMENT) {
            log.warn("Tentative de modification d'un chèque non ENCAISSEMENT — id: {}", id);
            throw new ChequeBusinessException("Modification interdite : le chèque id " + id + " n'est pas de type ENCAISSEMENT.");
        }

        if (existingCheque.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de modification d'un chèque déjà réglé — id: {}", id);
            throw new ChequeBusinessException("Modification interdite : le chèque id " + id + " est déjà réglé.");
        }

        if (dto.getNumero() != null) {
            existingCheque.setNumero(dto.getNumero());
        }
        if (dto.getDateEmission() != null) {
            existingCheque.setDateEmission(dto.getDateEmission());
        }
        if (dto.getDateEcheance() != null) {
            existingCheque.setDateEcheance(dto.getDateEcheance());
        }
        if (dto.getBanqueId() != null) {
            existingCheque.setBanque(banqueService.findOrThrow(dto.getBanqueId()));
        }
        if (dto.getMontant() != null) {
            existingCheque.setMontant(dto.getMontant());
        }
        if (dto.getEmetteur() != null) {
            existingCheque.setEmetteur(dto.getEmetteur());
        }

        existingCheque = chequeRepository.save(existingCheque);
        log.info("Chèque modifié — id: {}", id);
        return toDto(existingCheque);
    }

    @Transactional(readOnly = true)
    public ChequeResponseDto getChequeById(Long id) {
        log.debug("Récupération chèque — id: {}", id);
        return toDto(trouverOuLeverException(id));
    }

    /**
     * Supprime un chèque saisi manuellement.
     *
     * <p>Un chèque issu de la synchronisation FunPaiement ({@code idPaiement} renseigné) ne peut pas
     * être supprimé : le lecteur de synchro relit tout l'historique à chaque exécution et la seule
     * clé d'idempotence est {@code findByIdPaiement}. Supprimer la ligne efface cette clé, et la
     * synchro suivante la recrée à l'identique. Pour retirer une telle ligne des encours, l'annuler
     * plutôt que la supprimer : une ligne existante n'est jamais recréée ni écrasée par la synchro.
     * Même règle que {@code VirementService.deleteVirement} (boucle constatée en production sur les
     * virements en juillet 2026).
     */
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = Cheque.class)
    public void deleteCheque(Long id) {
        log.info("Suppression chèque — id: {}", id);

        Cheque cheque = trouverOuLeverException(id);

        if (cheque.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de suppression d'un chèque déjà réglé — id: {}", id);
            throw new ChequeBusinessException("Suppression interdite : le chèque id " + id + " est déjà réglé.");
        }

        if (cheque.getIdPaiement() != null) {
            log.warn("Tentative de suppression d'un chèque synchronisé — id: {}, idPaiement: {}",
                    id, cheque.getIdPaiement());
            throw new ChequeBusinessException(
                    "Suppression interdite : le chèque " + id + " provient de la synchronisation FunPaiement "
                            + "et serait recréé à la prochaine synchronisation. Annulez-le plutôt que de le supprimer.");
        }

        chequeRepository.deleteById(id);
        log.info("Chèque supprimé — id: {}", id);
    }

    @Transactional(readOnly = true)
    public Page<ChequeResponseDto> getAllDecaissement(ChequeSearchDto searchDto, Pageable pageable) {
        log.debug("Recherche chèques décaissement — critères: {}", searchDto);

        Specification<Cheque> spec = Specification.where(ChequeSpec.hasNumero(searchDto.numero()))
                .and(ChequeSpec.hasBeneficiaire(searchDto.beneficiaire()))
                .and(ChequeSpec.hasNomBanque(searchDto.nomBanque()))
                .and(ChequeSpec.hasStatut(searchDto.statut()))
                .and(ChequeSpec.byDateEmissionRange(searchDto.dateEmissionFrom(), searchDto.dateEmissionTo()))
                .and(ChequeSpec.byDateEcheanceRange(searchDto.dateEcheanceFrom(), searchDto.dateEcheanceTo()))
                .and(ChequeSpec.byDateOperationRange(searchDto.dateOperationFrom(), searchDto.dateOperationTo()))
                .and(ChequeSpec.byDateValeurRange(searchDto.dateValeurFrom(), searchDto.dateValeurTo()))
                .and(ChequeSpec.hasType(Type.DECAISSEMENT));

        Page<Cheque> chequePage = chequeRepository.findAll(spec, stablePageable(pageable));
        log.debug("{} chèque(s) de décaissement trouvé(s)", chequePage.getTotalElements());

        return chequePage.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ChequeResponseDto> getAllEncaissement(ChequeSearchDto searchDto, Pageable pageable) {
        log.debug("Recherche chèques encaissement — critères: {}", searchDto);

        Specification<Cheque> spec = Specification.where(ChequeSpec.hasType(Type.ENCAISSEMENT))
                .and(ChequeSpec.hasNomBanque(searchDto.nomBanque()))
                .and(ChequeSpec.hasStatut(searchDto.statut()))
                .and(ChequeSpec.hasMontant(searchDto.montant()))
                .and(ChequeSpec.byDateEmissionRange(searchDto.dateEmissionFrom(), searchDto.dateEmissionTo()))
                .and(ChequeSpec.byDateEcheanceRange(searchDto.dateEcheanceFrom(), searchDto.dateEcheanceTo()))
                .and(ChequeSpec.byDateOperationRange(searchDto.dateOperationFrom(), searchDto.dateOperationTo()))
                .and(ChequeSpec.byDateValeurRange(searchDto.dateValeurFrom(), searchDto.dateValeurTo()))
                .and(ChequeSpec.hasEmetteur(searchDto.emetteur()))
                .and(ChequeSpec.hasNumero(searchDto.numero()));

        Page<Cheque> chequePage = chequeRepository.findAll(spec, stablePageable(pageable));
        log.debug("{} chèque(s) d'encaissement trouvé(s)", chequePage.getTotalElements());

        return chequePage.map(this::toDto);
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Cheque.class)
    public ChequeResponseDto toggleChequeStatus(Long id) {
        log.info("Basculement statut chèque — id: {}", id);

        Cheque cheque = trouverOuLeverException(id);

        if (cheque.getStatut() == StatutMoyenPaiement.ANNULE) {
            log.warn("Tentative de basculement du statut d'un chèque annulé — id: {}", id);
            throw new ChequeBusinessException("Impossible de basculer le statut d'un chèque annulé (id: " + id + ").");
        }

        cheque.setStatut(cheque.getStatut() == StatutMoyenPaiement.ENCOURS ? StatutMoyenPaiement.REGLE : StatutMoyenPaiement.ENCOURS);
        cheque = chequeRepository.save(cheque);
        if (cheque.getStatut() == StatutMoyenPaiement.REGLE) {
            chequeMetrics.incrementerPassageRegle();
        }

        log.info("Statut basculé — id: {}, nouveau statut: {}", id, cheque.getStatut());
        return toDto(cheque);
    }

    @Transactional(readOnly = true)
    public EncoursChequeResponseDto getEncoursDecaissementForCheques(EncoursChequeSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.debug("Encours décaissement chèques — banqueId: {}, critères: {}", banqueId, searchDto);

        Specification<Cheque> spec = ChequeSpec.encoursDecaissements(banqueId, finDuMoisCourant())
                .and(ChequeSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(ChequeSpec.hasNumero(searchDto.getNumero()))
                .and(ChequeSpec.hasMontant(searchDto.getMontant()));

        Page<Cheque> page = chequeRepository.findAll(spec, stablePageable(pageable));
        Page<ChequeResponseDto> chequePage = page.map(this::toDto);
        BigDecimal total = chequeRepository.findAll(spec).stream().map(Cheque::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug("Total encours décaissement — banqueId: {}, total: {}", banqueId, total);
        return new EncoursChequeResponseDto(total, chequePage);
    }

    @Transactional(readOnly = true)
    public EncoursChequeResponseDto getEncoursEncaissementForCheques(EncoursChequeSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.debug("Encours encaissement chèques — banqueId: {}, critères: {}", banqueId, searchDto);

        Specification<Cheque> spec = ChequeSpec.encoursEncaissements(banqueId, finDuMoisCourant())
                .and(ChequeSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(ChequeSpec.hasEmetteur(searchDto.getEmetteur()))
                .and(ChequeSpec.hasMontant(searchDto.getMontant()))
                .and(ChequeSpec.hasNumero(searchDto.getNumero()))
                .and(ChequeSpec.hasDateEmission(searchDto.getDateEmission()));

        Page<Cheque> page = chequeRepository.findAll(spec, stablePageable(pageable));
        Page<ChequeResponseDto> chequePage = page.map(this::toDto);
        BigDecimal total = chequeRepository.findAll(spec).stream().map(Cheque::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug("Total encours encaissement — banqueId: {}, total: {}", banqueId, total);
        return new EncoursChequeResponseDto(total, chequePage);
    }

    // ── Export PDF ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] exportEncaissementsPdf(ChequeSearchDto searchDto) {
        Specification<Cheque> spec = Specification.where(ChequeSpec.hasType(Type.ENCAISSEMENT))
                .and(ChequeSpec.hasNomBanque(searchDto.nomBanque()))
                .and(ChequeSpec.hasStatut(searchDto.statut()))
                .and(ChequeSpec.hasMontant(searchDto.montant()))
                .and(ChequeSpec.byDateEmissionRange(searchDto.dateEmissionFrom(), searchDto.dateEmissionTo()))
                .and(ChequeSpec.byDateEcheanceRange(searchDto.dateEcheanceFrom(), searchDto.dateEcheanceTo()))
                .and(ChequeSpec.byDateOperationRange(searchDto.dateOperationFrom(), searchDto.dateOperationTo()))
                .and(ChequeSpec.byDateValeurRange(searchDto.dateValeurFrom(), searchDto.dateValeurTo()))
                .and(ChequeSpec.hasEmetteur(searchDto.emetteur()))
                .and(ChequeSpec.hasNumero(searchDto.numero()));

        List<Cheque> cheques = chequeRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "dateEmission"));
        BigDecimal total = sumMontant(spec);
        List<String> filterLines = buildFilterSummary(searchDto);

        return generateChequePdf("État des Chèques d'Encaissement", cheques, total, filterLines);
    }

    @Transactional(readOnly = true)
    public byte[] exportDecaissementsPdf(ChequeSearchDto searchDto) {
        Specification<Cheque> spec = Specification.where(ChequeSpec.hasNumero(searchDto.numero()))
                .and(ChequeSpec.hasBeneficiaire(searchDto.beneficiaire()))
                .and(ChequeSpec.hasNomBanque(searchDto.nomBanque()))
                .and(ChequeSpec.hasStatut(searchDto.statut()))
                .and(ChequeSpec.byDateEmissionRange(searchDto.dateEmissionFrom(), searchDto.dateEmissionTo()))
                .and(ChequeSpec.byDateEcheanceRange(searchDto.dateEcheanceFrom(), searchDto.dateEcheanceTo()))
                .and(ChequeSpec.byDateOperationRange(searchDto.dateOperationFrom(), searchDto.dateOperationTo()))
                .and(ChequeSpec.byDateValeurRange(searchDto.dateValeurFrom(), searchDto.dateValeurTo()))
                .and(ChequeSpec.hasType(Type.DECAISSEMENT));

        List<Cheque> cheques = chequeRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "dateEmission"));
        BigDecimal total = sumMontant(spec);
        List<String> filterLines = buildFilterSummary(searchDto);

        return generateChequePdf("État des Chèques de Décaissement", cheques, total, filterLines);
    }

    @Transactional(readOnly = true)
    public byte[] exportEncoursEncaissementPdf(EncoursChequeSearchDto searchDto, Long banqueId) {
        Specification<Cheque> spec = ChequeSpec.encoursEncaissements(banqueId, finDuMoisCourant())
                .and(ChequeSpec.hasEmetteur(searchDto.getEmetteur()))
                .and(ChequeSpec.hasMontant(searchDto.getMontant()))
                .and(ChequeSpec.hasNumero(searchDto.getNumero()))
                .and(ChequeSpec.hasDateEmission(searchDto.getDateEmission()));

        List<Cheque> cheques = chequeRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "dateEmission"));
        BigDecimal total = sumMontant(spec);
        List<String> filterLines = buildEncoursFilterSummary(searchDto, banqueId);

        return generateChequePdf("Encours d'Encaissement — Chèques", cheques, total, filterLines);
    }

    @Transactional(readOnly = true)
    public byte[] exportEncoursDecaissementPdf(EncoursChequeSearchDto searchDto, Long banqueId) {
        Specification<Cheque> spec = ChequeSpec.encoursDecaissements(banqueId, finDuMoisCourant())
                .and(ChequeSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(ChequeSpec.hasNumero(searchDto.getNumero()))
                .and(ChequeSpec.hasMontant(searchDto.getMontant()));

        List<Cheque> cheques = chequeRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "dateEmission"));
        BigDecimal total = sumMontant(spec);
        List<String> filterLines = buildEncoursFilterSummary(searchDto, banqueId);

        return generateChequePdf("Encours de Décaissement — Chèques", cheques, total, filterLines);
    }

    private BigDecimal sumMontant(Specification<Cheque> spec) {
        return chequeRepository.findAll(spec).stream()
                .map(Cheque::getMontant)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private byte[] generateChequePdf(String title, List<Cheque> cheques, BigDecimal totalMontant, List<String> filterLines) {
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
                                    + "  •  " + cheques.size() + " chèque(s)";
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

                            float[] colWidths = {10f, 14f, 13f, 13f, 11f, 11f, 11f, 8f};
                            PdfPTable table = new PdfPTable(colWidths);
                            table.setWidthPercentage(100f);
                            table.setSpacingBefore(8f);
                            table.setHeaderRows(1);

                            for (String h : new String[]{
                                    "N° Chèque", "Banque", "Émetteur", "Bénéficiaire",
                                    "Montant (MAD)", "Date émission", "Date échéance", "Statut"}) {
                                PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                                c.setBackgroundColor(medBlue);
                                c.setPadding(5f);
                                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                                c.setBorderColor(Color.WHITE);
                                c.setBorderWidth(0.5f);
                                table.addCell(c);
                            }

                            for (int i = 0; i < cheques.size(); i++) {
                                Cheque c = cheques.get(i);
                                Color rowBg = i % 2 == 0 ? Color.WHITE : rowAlt;

                                addPdfCell(table, c.getNumero(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, c.getBanque() != null ? c.getBanque().getNom() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, c.getEmetteur(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, c.getBeneficiaire(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, formatMontantPdf(c.getMontant()), cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);
                                addPdfCell(table, c.getDateEmission() != null ? c.getDateEmission().format(DATE_FMT) : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);
                                addPdfCell(table, c.getDateEcheance() != null ? c.getDateEcheance().format(DATE_FMT) : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);

                                boolean regle = c.getStatut() == StatutMoyenPaiement.REGLE;
                                boolean annule = c.getStatut() == StatutMoyenPaiement.ANNULE;
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

                            PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL — " + cheques.size() + " chèque(s)", totalFont));
                            totalLabel.setColspan(4);
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

                            for (int j = 0; j < 3; j++) {
                                PdfPCell empty = new PdfPCell(new Phrase(""));
                                empty.setBackgroundColor(lightBlue);
                                empty.setBorderColor(medBlue);
                                table.addCell(empty);
                            }

                            doc.add(table);

                        } catch (DocumentException e) {
                            throw new RuntimeException("Erreur génération contenu PDF chèques", e);
                        }
                    }
            );
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des chèques", e);
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

    private List<String> buildFilterSummary(ChequeSearchDto dto) {
        List<String> lines = new ArrayList<>();
        if (dto == null) return lines;
        if (dto.numero() != null && !dto.numero().isBlank())
            lines.add("N° Chèque : " + dto.numero());
        if (dto.nomBanque() != null && !dto.nomBanque().isBlank())
            lines.add("Banque : " + dto.nomBanque());
        if (dto.beneficiaire() != null && !dto.beneficiaire().isBlank())
            lines.add("Bénéficiaire : " + dto.beneficiaire());
        if (dto.emetteur() != null && !dto.emetteur().isBlank())
            lines.add("Émetteur : " + dto.emetteur());
        if (dto.statut() != null)
            lines.add("Statut : " + dto.statut());
        if (dto.montant() != null)
            lines.add("Montant : " + formatMontantPdf(dto.montant()) + " MAD");
        if (dto.dateEmissionFrom() != null || dto.dateEmissionTo() != null) {
            String from = dto.dateEmissionFrom() != null ? dto.dateEmissionFrom().format(DATE_FMT) : "…";
            String to   = dto.dateEmissionTo()   != null ? dto.dateEmissionTo().format(DATE_FMT)   : "…";
            lines.add("Date émission : " + from + " → " + to);
        }
        if (dto.dateEcheanceFrom() != null || dto.dateEcheanceTo() != null) {
            String from = dto.dateEcheanceFrom() != null ? dto.dateEcheanceFrom().format(DATE_FMT) : "…";
            String to   = dto.dateEcheanceTo()   != null ? dto.dateEcheanceTo().format(DATE_FMT)   : "…";
            lines.add("Date échéance : " + from + " → " + to);
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

    private List<String> buildEncoursFilterSummary(EncoursChequeSearchDto dto, Long banqueId) {
        List<String> lines = new ArrayList<>();
        try {
            lines.add("Banque : " + banqueService.findOrThrow(banqueId).getNom());
        } catch (ResourceNotFoundException ignored) {
            // Filtre obsolète (banque supprimée depuis) — ignoré silencieusement dans le résumé PDF.
        }
        if (dto == null) return lines;
        if (dto.getNumero() != null && !dto.getNumero().isBlank())
            lines.add("N° Chèque : " + dto.getNumero());
        if (dto.getBeneficiaire() != null && !dto.getBeneficiaire().isBlank())
            lines.add("Bénéficiaire : " + dto.getBeneficiaire());
        if (dto.getEmetteur() != null && !dto.getEmetteur().isBlank())
            lines.add("Émetteur : " + dto.getEmetteur());
        if (dto.getMontant() != null)
            lines.add("Montant : " + formatMontantPdf(dto.getMontant()) + " MAD");
        if (dto.getDateEmission() != null)
            lines.add("Date émission : " + dto.getDateEmission().format(DATE_FMT));
        return lines;
    }

    // ── Helpers internes ──────────────────────────────────────────────────────

    private Cheque trouverOuLeverException(Long id) {
        return chequeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cheque with id " + id + " not found."));
    }

    /**
     * Fin du mois courant — recalculée à chaque appel (et non mise en cache) pour que
     * les calculs d'encours restent corrects après le premier jour suivant le déploiement.
     */
    private LocalDate finDuMoisCourant() {
        return LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
    }

    private Pageable stablePageable(Pageable pageable) {
        return pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));
    }

    /**
     * Mappe l'entité vers le DTO puis applique les libellés d'affichage calculés
     * (règle métier propre au service — jamais dans le Mapper, cf. architecture.md).
     */
    private ChequeResponseDto toDto(Cheque cheque) {
        ChequeResponseDto dto = chequeMapper.toDto(cheque);
        dto.setObservation("CHQ N°" + cheque.getNumero());
        dto.setLibelle(cheque.getType() == Type.DECAISSEMENT ? cheque.getBeneficiaire() : cheque.getEmetteur());
        return dto;
    }
}
