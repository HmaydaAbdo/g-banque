package staport.finance.gBanque.decomptes.service;

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
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.decomptes.dto.*;
import staport.finance.gBanque.decomptes.entities.Decompte;
import staport.finance.gBanque.decomptes.mapper.DecompteMapper;
import staport.finance.gBanque.decomptes.repositories.DecompteRepository;
import staport.finance.gBanque.decomptes.spec.DecompteSpec;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.repositories.MarcheRepository;
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

@Service
public class DecompteService {

    private static final Logger log = LoggerFactory.getLogger(DecompteService.class);

    /** Tri par défaut : dateValeur DESC, les NULL en dernier */
    private static final Sort DEFAULT_SORT =
            Sort.by(Sort.Order.desc("dateValeur").nullsLast());

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
    private final DecompteRepository decompteRepository;
    private final DecompteMapper decompteMapper;
    private final MarcheRepository marcheRepository;
    private final BanqueRepository banqueRepository;
    private final StaportPdfReportService staportPdfReportService;
    LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());


    public DecompteService(DecompteRepository decompteRepository, DecompteMapper decompteMapper,
                           MarcheRepository marcheRepository, BanqueRepository banqueRepository,
                           StaportPdfReportService staportPdfReportService) {
        this.decompteRepository = decompteRepository;
        this.decompteMapper = decompteMapper;
        this.marcheRepository = marcheRepository;
        this.banqueRepository = banqueRepository;
        this.staportPdfReportService = staportPdfReportService;
    }

    @Transactional
    public DecompteResponseDto createDecompte(DecompteRequestDto requestDTO) {
        log.info("Début de la création d'un décompte pour le marché ID: {}", requestDTO.getMarcheId());
        log.debug("Données reçues: {}", requestDTO);

        Marche marche = marcheRepository.findById(requestDTO.getMarcheId())
                .orElseThrow(() -> {
                    log.error("Marché non trouvé avec l'ID: {}", requestDTO.getMarcheId());
                    return new ResourceNotFoundException("Marche not found with ID: " + requestDTO.getMarcheId());
                });

        log.debug("Marché récupéré: {} (Numéro: {}, Nanti: {})", marche.getNom(), marche.getNumero(), marche.isNanti());


        Banque banqueDEncaissement = banqueRepository.findById(requestDTO.getBanqueEncaissementId())
                .orElseThrow(() -> {
                    log.error("Banque d'encaissement non trouvée avec l'ID: {}", requestDTO.getBanqueEncaissementId());
                    return new ResourceNotFoundException(
                            "Banque non trouvée avec l'ID " + requestDTO.getBanqueEncaissementId()
                    );
                });

        Decompte decompte = decompteMapper.toEntity(requestDTO, marche, banqueDEncaissement);

        decompte = decompteRepository.save(decompte);

        log.info("Décompte créé avec succès - ID: {}, Montant: {}, Libellé: {}, Banque encaissement: {}, Marché nanti: {}",
                decompte.getId(), decompte.getMontant(), decompte.getLibelle(),
                banqueDEncaissement.getNom(), marche.isNanti());

        return decompteMapper.toDto(decompte);
    }

    @Transactional
    public DecompteResponseDto updateDecompte(Long id, DecompteRequestDto requestDTO) {
        log.info("Début de la modification du décompte ID: {}", id);
        log.debug("Nouvelles données reçues: {}", requestDTO);

        Decompte existingDecompte = decompteRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Décompte non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("décompte not found with ID: " + id);
                });

        log.debug("Décompte existant récupéré - Ancien montant: {}, Ancien libellé: {}",
                existingDecompte.getMontant(), existingDecompte.getLibelle());

        Marche marche = marcheRepository.findById(requestDTO.getMarcheId())
                .orElseThrow(() -> {
                    log.error("Marché non trouvé avec l'ID: {}", requestDTO.getMarcheId());
                    return new ResourceNotFoundException("Marche not found with ID: " + requestDTO.getMarcheId());
                });

        log.debug("Marché récupéré pour la mise à jour: {} (Nanti: {})", marche.getNom(), marche.isNanti());

        // Déterminer la banque d'encaissement selon le statut du marché
        Banque banqueDEncaissement = banqueRepository.findById(requestDTO.getBanqueEncaissementId())
                .orElseThrow(() -> {
                    log.error("Banque d'encaissement non trouvée avec l'ID: {}", requestDTO.getBanqueEncaissementId());
                    return new ResourceNotFoundException(
                            "Banque non trouvée avec l'ID " + requestDTO.getBanqueEncaissementId()
                    );
                });
        // Mettre à jour tous les champs
        existingDecompte.setMarche(marche);
        existingDecompte.setBanqueEncaissement(banqueDEncaissement);
        existingDecompte.setDateEmission(requestDTO.getDateEmission());
        existingDecompte.setMontant(requestDTO.getMontant());
        existingDecompte.setLibelle(requestDTO.getLibelle());
        existingDecompte.setObservation("Marche: " + marche.getNumero());

        Decompte updatedDecompte = decompteRepository.save(existingDecompte);

        log.info("Décompte modifié avec succès - ID: {}, Nouveau montant: {}, Banque encaissement: {}, Marché nanti: {}",
                id, updatedDecompte.getMontant(), banqueDEncaissement.getNom(), marche.isNanti());

        return decompteMapper.toDto(updatedDecompte);
    }
    @Transactional
    public void deleteDecompte(Long id) {
        log.info("Tentative de suppression du décompte ID: {}", id);

        if (!decompteRepository.existsById(id)) {
            log.error("Impossible de supprimer - Décompte non trouvé avec l'ID: {}", id);
            throw new ResourceNotFoundException("décompte not found with ID: " + id);
        }

        decompteRepository.deleteById(id);
        log.info("Décompte ID: {} supprimé avec succès", id);
    }

    @Transactional(readOnly = true)
    public DecompteResponseDto findById(Long id) {
        log.debug("Recherche du décompte avec l'ID: {}", id);

        Decompte decompte = decompteRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Décompte non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Décompte not found with ID: " + id);
                });

        log.debug("Décompte trouvé - Marché: {}, Montant: {}",
                decompte.getMarche().getNom(), decompte.getMontant());

        return decompteMapper.toDto(decompte);
    }

    @Transactional(readOnly = true)
    public Page<DecompteResponseDto> findAll(DecompteSearchDto searchDTO, Pageable pageable) {
        log.info("Recherche de décomptes avec critères: {}", searchDTO);
        log.debug("Pagination demandée - Page: {}, Taille: {}", pageable.getPageNumber(), pageable.getPageSize());

        Specification<Decompte> spec = DecompteSpec.bySearchCriteria(searchDTO);
        Sort sort = resolveSort(pageable.getSort());
        Page<Decompte> decomptes = decompteRepository.findAll(spec,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort));

        log.info("Recherche terminée - {} résultat(s) trouvé(s) sur {} élément(s) au total", decomptes.getNumberOfElements(), decomptes.getTotalElements());

        return decomptes.map(decompteMapper::toDto);
    }

    @Transactional
    public DecompteResponseDto toglleStatus(Long id ) {
        log.info("Changement du statut du décompte ID: {}", id);

        Decompte decompte = decompteRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Décompte non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("décompte not found with ID: " + id);
                });

        Status existingStatus = decompte.getStatus();
        Status newStatus = existingStatus == Status.PAYE ? Status.PREVU : Status.PAYE;

        log.debug("Changement de statut - Ancien: {}, Nouveau: {}", existingStatus, newStatus);

        decompte.setStatus(newStatus);
        Decompte updatedDecompte = decompteRepository.save(decompte);

        log.info("Statut du décompte ID: {} modifié avec succès de {} vers {}",
                id, existingStatus, newStatus);

        return decompteMapper.toDto(updatedDecompte);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PDF EXPORT
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] exportToPdf(DecompteSearchDto searchDTO) {
        List<Decompte> decomptes = decompteRepository.findAll(
                DecompteSpec.bySearchCriteria(searchDTO), DEFAULT_SORT);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Précalculer le total (variable effectively-final pour la lambda)
        BigDecimal totalMontant = decomptes.stream()
                .filter(d -> d.getMontant() != null)
                .map(Decompte::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> filterLines = buildFilterSummary(searchDTO, fmt);

        try {
            return staportPdfReportService.generateReport(
                    "État des Décomptes",
                    PageSize.A4.rotate(),
                    (doc, writer) -> {
                        try {
                            // ── En-tête du document (logo/footer gérés par StaportPdfPageEventHelper) ──
                            doc.add(Chunk.NEWLINE);
                            doc.add(Chunk.NEWLINE);

                            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
                            Font subFont   = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.decode("#374151"));
                            Font hdrFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
                            Font cellFont  = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
                            Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.decode("#1e3a8a"));

                            Paragraph title = new Paragraph("État des Décomptes", titleFont);
                            title.setAlignment(Element.ALIGN_CENTER);
                            doc.add(title);

                            String exportInfo = "Exporté le " + LocalDate.now().format(fmt)
                                    + "  •  " + decomptes.size() + " décompte(s)";
                            if (!filterLines.isEmpty()) {
                                exportInfo += "  •  Filtres : " + String.join("  |  ", filterLines);
                            }
                            Paragraph sub = new Paragraph(exportInfo, subFont);
                            sub.setAlignment(Element.ALIGN_CENTER);
                            sub.setSpacingAfter(8f);
                            doc.add(sub);

                            // ── Tableau ───────────────────────────────────────────
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
                                addPdfCell(table, formatMontantPdf(d.getMontant()), cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);
                                addPdfCell(table, d.getDateEmission()  != null ? d.getDateEmission().format(fmt)  : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);
                                addPdfCell(table, d.getDateOperation() != null ? d.getDateOperation().format(fmt) : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);
                                addPdfCell(table, d.getDateValeur()    != null ? d.getDateValeur().format(fmt)    : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);

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

                            // Ligne total
                            PdfPCell totalLabel = new PdfPCell(
                                    new Phrase("TOTAL — " + decomptes.size() + " décompte(s)", totalFont));
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

    private String formatMontantPdf(BigDecimal montant) {
        if (montant == null) return "—";
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.FRANCE);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(montant);
    }

    private List<String> buildFilterSummary(DecompteSearchDto dto, DateTimeFormatter fmt) {
        List<String> lines = new ArrayList<>();
        if (dto == null) return lines;
        if (dto.getMarcheId() != null)
            marcheRepository.findById(dto.getMarcheId())
                    .ifPresent(m -> lines.add("Marché : " + m.getNumero()));
        if (dto.getBanqueId() != null)
            banqueRepository.findById(dto.getBanqueId())
                    .ifPresent(b -> lines.add("Banque : " + b.getNom()));
        if (dto.getStatus() != null)
            lines.add("Statut : " + (dto.getStatus() == Status.PAYE ? "Payé" : "Prévu"));
        if (dto.getDateValeurFrom() != null || dto.getDateValeurTo() != null) {
            String from = dto.getDateValeurFrom() != null ? dto.getDateValeurFrom().format(fmt) : "…";
            String to   = dto.getDateValeurTo()   != null ? dto.getDateValeurTo().format(fmt)   : "…";
            lines.add("Date valeur : " + from + " → " + to);
        }
        if (dto.getMontantMin() != null || dto.getMontantMax() != null) {
            String min = dto.getMontantMin() != null ? formatMontantPdf(dto.getMontantMin()) : "0";
            String max = dto.getMontantMax() != null ? formatMontantPdf(dto.getMontantMax()) : "∞";
            lines.add("Montant : " + min + " → " + max + " MAD");
        }
        return lines;
    }

    @Transactional(readOnly = true)
    public EncoursDecompteDto getDecomptesEncoursForBanque(EncoursDecompteSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Récupération des encours de décomptes pour la banque ID: {}", banqueId);
        log.debug("Critères de recherche: {}", searchDto);

        // Build the specification to filter the records
        Specification<Decompte> spec = DecompteSpec.byBanqueId(banqueId)
                .and(DecompteSpec.outstandingUntil(endOfMonth))
                .and(DecompteSpec.byMarcheName(searchDto.getMarcheName()))
                .and(DecompteSpec.byMarcheNumero(searchDto.getMarcheNumero()))
                .and(DecompteSpec.hasMontant(searchDto.getMontant()));

        // Fetch the paginated list of decomptes  records
        Sort sort = resolveSort(pageable.getSort());
        Page<Decompte> decomptePage = decompteRepository.findAll(spec,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort));

        log.debug("Nombre de résultats paginés: {}", decomptePage.getNumberOfElements());

        // Fetch the total sum of the montant for the same criteria
        BigDecimal totalDesEncours = decompteRepository.findAll(spec).stream()
                .map(Decompte::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total des encours de décomptes calculé: {} pour {} enregistrement(s)",
                totalDesEncours, decomptePage.getTotalElements());

        // Map the page of entities to a page of DTOs
        Page<DecompteResponseDto> pageDto = decomptePage.map(decompteMapper::toDto);

        // Create and return the final DTO
        EncoursDecompteDto result = new EncoursDecompteDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        return result;
    }


}
