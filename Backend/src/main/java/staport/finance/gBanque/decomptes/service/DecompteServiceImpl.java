package staport.finance.gBanque.decomptes.service;

import java.util.Set;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.ReverseTransactionService;
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
import staport.finance.gBanque.decomptes.dtos.request.DecompteRequestDto;
import staport.finance.gBanque.decomptes.dtos.request.DecompteSearchDto;
import staport.finance.gBanque.decomptes.dtos.response.DecompteResponseDto;
import staport.finance.gBanque.decomptes.entities.Decompte;
import staport.finance.gBanque.decomptes.mapper.DecompteMapper;
import staport.finance.gBanque.decomptes.repositories.DecompteRepository;
import staport.finance.gBanque.decomptes.spec.DecompteSpec;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.service.IMarcheService;
import staport.finance.gBanque.prefinancement.enums.Status;
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

/**
 * Implémentation du service Décompte.
 *
 * <p>Encapsulation des domaines (architecture.md) : la résolution du {@link Marche} et de la
 * {@link Banque} d'encaissement passe exclusivement par la façade publique de chaque module
 * ({@link IMarcheService}, {@link IBanqueService}) — jamais par injection directe d'un
 * {@code Repository} étranger.
 *
 * <p>Anti-N+1 : {@code DecompteRepository.findAll} porte l'{@code @EntityGraph}
 * {@code "Decompte.avecAssociations"} (marché + banque du marché + banque d'encaissement).
 *
 * <p>19/09/2026 : l'encours de décomptes par banque (écran dédié {@code decomptes}) a été retiré —
 * le récapitulatif unifié couvre désormais la validation de cette source.
 * {@code getDecomptesEncoursForBanque}, son export PDF et le calcul de somme par
 * {@code EntityManager} qu'ils utilisaient seuls ont été retirés avec lui.
 */
@Service
public class DecompteServiceImpl implements IDecompteService {

    private static final Logger log = LoggerFactory.getLogger(DecompteServiceImpl.class);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Tri par défaut : dateValeur DESC, les NULL en dernier */
    private static final Sort DEFAULT_SORT =
            Sort.by(Sort.Order.desc("dateValeur").nullsLast());

    private final DecompteRepository decompteRepository;
    private final DecompteMapper decompteMapper;
    private final IMarcheService marcheService;
    private final IBanqueService banqueService;
    private final StaportPdfReportService staportPdfReportService;
    private final ReverseTransactionService reverseTransactionService;

    public DecompteServiceImpl(DecompteRepository decompteRepository, DecompteMapper decompteMapper,
                                IMarcheService marcheService, IBanqueService banqueService,
                                StaportPdfReportService staportPdfReportService,
                                ReverseTransactionService reverseTransactionService) {
        this.reverseTransactionService = reverseTransactionService;
        this.decompteRepository = decompteRepository;
        this.decompteMapper = decompteMapper;
        this.marcheService = marcheService;
        this.banqueService = banqueService;
        this.staportPdfReportService = staportPdfReportService;
    }

    /**
     * Applique le tri demandé en garantissant NULLS LAST sur dateValeur.
     * Si aucun tri n'est fourni, utilise DEFAULT_SORT.
     */
    private Sort resolveSort(Sort incoming) {
        if (!incoming.isSorted()) return DEFAULT_SORT;
        List<Sort.Order> orders = incoming.stream()
                .map(order -> order.getProperty().equals("dateValeur")
                        ? (order.isAscending()
                                ? Sort.Order.asc("dateValeur").nullsLast()
                                : Sort.Order.desc("dateValeur").nullsLast())
                        : order)
                .toList();
        return Sort.by(orders);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = Decompte.class)
    public DecompteResponseDto createDecompte(DecompteRequestDto requestDTO) {
        log.info("Début de création d'un décompte pour le marché ID: {}", requestDTO.marcheId());

        Marche marche = marcheService.findOrThrow(requestDTO.marcheId());
        Banque banqueEncaissement = banqueService.findOrThrow(requestDTO.banqueEncaissementId());

        Decompte decompte = decompteMapper.toEntity(requestDTO, marche, banqueEncaissement);
        decompte.setObservation("Marche: " + marche.getNumero());
        decompte = decompteRepository.save(decompte);

        log.info("Décompte créé avec succès - ID: {}, banque encaissement ID: {}",
                decompte.getId(), banqueEncaissement.getId());

        return decompteMapper.toDto(decompte);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Decompte.class)
    public DecompteResponseDto updateDecompte(Long id, DecompteRequestDto requestDTO) {
        log.info("Début de la modification du décompte ID: {}", id);

        Decompte existingDecompte = decompteRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Décompte non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Décompte non trouvé avec l'ID: " + id);
                });

        Marche marche = marcheService.findOrThrow(requestDTO.marcheId());
        Banque banqueEncaissement = banqueService.findOrThrow(requestDTO.banqueEncaissementId());

        existingDecompte.setMarche(marche);
        existingDecompte.setBanqueEncaissement(banqueEncaissement);
        existingDecompte.setDateEmission(requestDTO.dateEmission());
        existingDecompte.setMontant(requestDTO.montant());
        existingDecompte.setLibelle(requestDTO.libelle());
        existingDecompte.setObservation("Marche: " + marche.getNumero());

        Decompte updatedDecompte = decompteRepository.save(existingDecompte);

        log.info("Décompte modifié avec succès - ID: {}", id);

        return decompteMapper.toDto(updatedDecompte);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = Decompte.class)
    public void deleteDecompte(Long id) {
        log.info("Tentative de suppression du décompte ID: {}", id);

        Decompte decompte = decompteRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Impossible de supprimer - Décompte non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Décompte non trouvé avec l'ID: " + id);
                });

        if (decompte.getStatus() != Status.PREVU) {
            log.warn("Tentative de suppression d'un décompte déjà réglé — id: {}", id);
            throw new ValidationException("Le décompte id: " + id + " est déjà réglé et ne peut plus être supprimé.");
        }

        decompteRepository.deleteById(id);
        log.info("Décompte ID: {} supprimé avec succès", id);
    }

    @Override
    @Transactional(readOnly = true)
    public DecompteResponseDto findById(Long id) {
        log.debug("Recherche du décompte avec l'ID: {}", id);

        Decompte decompte = decompteRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Décompte non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Décompte non trouvé avec l'ID: " + id);
                });

        return decompteMapper.toDto(decompte);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DecompteResponseDto> findAll(DecompteSearchDto searchDTO, Pageable pageable) {
        log.info("Recherche de décomptes avec critères - marché: {}, banque: {}, statut: {}",
                searchDTO.marcheId(), searchDTO.banqueId(), searchDTO.status());

        Specification<Decompte> spec = DecompteSpec.searchByCriteria(searchDTO);
        Sort sort = resolveSort(pageable.getSort());
        Page<Decompte> decomptes = decompteRepository.findAll(spec,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort));

        log.info("Recherche terminée - {} résultat(s) trouvé(s) sur {} élément(s) au total",
                decomptes.getNumberOfElements(), decomptes.getTotalElements());

        return decomptes.map(decompteMapper::toDto);
    }

    @Override
    @Transactional
    public DecompteResponseDto toggleStatus(Long id) {
        log.info("Changement du statut du décompte ID: {}", id);

        Decompte decompte = decompteRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Décompte non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Décompte non trouvé avec l'ID: " + id);
                });

        // Retour « en cours » d'une opération déjà rapprochée : sa transaction est annulée (même effet que l'annulation
        // de la validation) ; sans transaction liée, la bascule reste libre (correction manuelle).
        if (decompte.getStatus() == Status.PAYE
                && reverseTransactionService.annulerSiRapproche(Set.of(TransactionSource.DECOMPTES), id).effectuee()) {
            return decompteMapper.toDto(decompte);
        }
        Status existingStatus = decompte.getStatus();
        Status newStatus = existingStatus == Status.PAYE ? Status.PREVU : Status.PAYE;

        log.debug("Changement de statut - id: {}, ancien: {}, nouveau: {}", id, existingStatus, newStatus);
        decompte.setStatus(newStatus);
        Decompte updatedDecompte = decompteRepository.save(decompte);

        log.info("Statut du décompte ID: {} modifié avec succès", id);

        return decompteMapper.toDto(updatedDecompte);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PDF EXPORT
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToPdf(DecompteSearchDto searchDTO) {
        List<Decompte> decomptes = decompteRepository.findAll(
                DecompteSpec.searchByCriteria(searchDTO), DEFAULT_SORT);

        BigDecimal totalMontant = decomptes.stream()
                .filter(d -> d.getMontant() != null)
                .map(Decompte::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> filterLines = buildFilterSummary(searchDTO);

        return generateDecomptePdf("État des Décomptes", decomptes, totalMontant, filterLines);
    }

    /**
     * Génère le contenu PDF utilisé par {@link #exportToPdf}.
     */
    private byte[] generateDecomptePdf(String title, List<Decompte> decomptes,
                                        BigDecimal totalMontant, List<String> filterLines) {
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
                                    + "  •  " + decomptes.size() + " décompte(s)";
                            if (!filterLines.isEmpty()) {
                                exportInfo += "  •  Filtres : " + String.join("  |  ", filterLines);
                            }
                            Paragraph sub = new Paragraph(exportInfo, subFont);
                            sub.setAlignment(Element.ALIGN_CENTER);
                            sub.setSpacingAfter(8f);
                            doc.add(sub);

                            Color medBlue   = Color.decode("#2563eb");
                            Color lightBlue = Color.decode("#dbeafe");
                            Color rowAlt    = Color.decode("#f8fafc");
                            Color green     = Color.decode("#16a34a");
                            Color orange    = Color.decode("#ea580c");
                            Color borderGray = Color.decode("#e5e7eb");

                            float[] colWidths = {4f, 13f, 16f, 12f, 9f, 9f, 9f, 9f};
                            PdfPTable table = new PdfPTable(colWidths);
                            table.setWidthPercentage(100f);
                            table.setSpacingBefore(8f);
                            table.setHeaderRows(1);

                            for (String h : new String[]{
                                "ID", "Marché", "Banque d'encaissement", "Montant (MAD)",
                                "Date d'émission", "Date d'opération", "Date valeur", "Statut"}) {
                                PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                                c.setBackgroundColor(medBlue);
                                c.setPadding(5f);
                                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                                c.setBorderColor(Color.WHITE);
                                c.setBorderWidth(0.5f);
                                table.addCell(c);
                            }

                            for (int i = 0; i < decomptes.size(); i++) {
                                Decompte d = decomptes.get(i);
                                Color rowBg = i % 2 == 0 ? Color.WHITE : rowAlt;

                                addPdfCell(table, String.valueOf(d.getId()), cellFont, rowBg, borderGray, Element.ALIGN_CENTER);
                                addPdfCell(table, d.getMarche() != null ? d.getMarche().getNumero() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, d.getBanqueEncaissement() != null ? d.getBanqueEncaissement().getNom() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, MontantFormatter.format(d.getMontant()), cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);
                                addPdfCell(table, d.getDateEmission()  != null ? d.getDateEmission().format(DATE_FMT)  : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);
                                addPdfCell(table, d.getDateOperation() != null ? d.getDateOperation().format(DATE_FMT) : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);
                                addPdfCell(table, d.getDateValeur()    != null ? d.getDateValeur().format(DATE_FMT)    : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);

                                boolean paye = d.getStatus() == Status.PAYE;
                                PdfPCell statusCell = new PdfPCell(new Phrase(
                                        paye ? "PAYÉ" : "PRÉVU",
                                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
                                statusCell.setBackgroundColor(paye ? green : orange);
                                statusCell.setPadding(4f);
                                statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                                statusCell.setBorderColor(borderGray);
                                statusCell.setBorderWidth(0.5f);
                                table.addCell(statusCell);
                            }

                            PdfPCell totalLabel = new PdfPCell(
                                    new Phrase("TOTAL — " + decomptes.size() + " décompte(s)", totalFont));
                            totalLabel.setColspan(3);
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

                            for (int j = 0; j < 4; j++) {
                                PdfPCell empty = new PdfPCell(new Phrase(""));
                                empty.setBackgroundColor(lightBlue);
                                empty.setBorderColor(medBlue);
                                table.addCell(empty);
                            }

                            doc.add(table);

                        } catch (DocumentException e) {
                            throw new RuntimeException("Erreur génération contenu PDF décomptes", e);
                        }
                    }
            );
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des décomptes", e);
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

    private List<String> buildFilterSummary(DecompteSearchDto dto) {
        List<String> lines = new ArrayList<>();
        if (dto == null) return lines;
        if (dto.marcheId() != null) {
            try {
                Marche m = marcheService.findOrThrow(dto.marcheId());
                lines.add("Marché : " + m.getNumero());
            } catch (ResourceNotFoundException ignored) {
                // Filtre obsolète (marché supprimé depuis) — ignoré silencieusement dans le résumé PDF.
            }
        }
        if (dto.banqueId() != null) {
            try {
                Banque b = banqueService.findOrThrow(dto.banqueId());
                lines.add("Banque : " + b.getNom());
            } catch (ResourceNotFoundException ignored) {
                // idem
            }
        }
        if (dto.status() != null)
            lines.add("Statut : " + (dto.status() == Status.PAYE ? "Payé" : "Prévu"));
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
        if (dto.montant() != null) {
            lines.add("Montant : " + MontantFormatter.format(dto.montant()) + " MAD");
        }
        return lines;
    }

}
