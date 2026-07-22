package staport.finance.gBanque.factoring.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.INatureLigneFonctionnelleService;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.services.LigneFonctionnelleReader;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.services.LigneFonctionnelleSpecifiqueReader;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.factoring.dtos.request.EncoursFactoringSearchDto;
import staport.finance.gBanque.factoring.dtos.request.FactoringRequestDto;
import staport.finance.gBanque.factoring.dtos.request.FactoringSearchDto;
import staport.finance.gBanque.factoring.dtos.response.EncoursFactoringDto;
import staport.finance.gBanque.factoring.dtos.response.FactoringResponseDto;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.factoring.mapper.FactoringMapper;
import staport.finance.gBanque.factoring.repository.FactoringRepository;
import staport.finance.gBanque.factoring.spec.FactoringSpec;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.service.IMarcheService;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;
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
import java.util.Optional;

/**
 * Implémentation du service Factoring.
 *
 * <p>Encapsulation des domaines (architecture.md) : la résolution du {@link Marche} passe par
 * {@link IMarcheService}, la résolution de la nature de ligne fonctionnelle "FACTORING" par
 * {@link INatureLigneFonctionnelleService} — jamais par injection directe d'un {@code Repository}
 * étranger. {@code LigneFonctionnelleReader}/{@code LigneFonctionnelleSpecifiqueReader} restent
 * inchangés (déjà des façades de lecture cross-domaine conformes).
 *
 * <p>Anti-N+1 : {@code FactoringRepository.findAll} porte l'{@code @EntityGraph}
 * {@code "Factoring.avecAssociations"} (marché + banque du marché).
 *
 * <p>Les totaux d'encours (déblocage/remboursement) sont calculés via une requête {@code SUM}
 * construite avec le même {@link Specification} que la recherche paginée (pas de chargement de
 * la liste complète en mémoire pour sommer côté Java — conventions/performance.md).
 * {@code endOfMonth} est recalculé à chaque appel (et non figé à la construction du bean).
 *
 * <p>{@link IBanqueService} est injecté uniquement pour résoudre le nom de banque affiché dans le
 * résumé des filtres des exports PDF — sans risque de cycle de bean : {@code BanqueServiceImpl} ne
 * dépend d'aucune classe du module Factoring (vérifié par recherche globale).
 */
@Service
public class FactoringServiceImpl implements IFactoringService {

    private static final Logger log = LoggerFactory.getLogger(FactoringServiceImpl.class);
    private static final String FACTORING_NATURE_CODE = "FACTORING";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final FactoringRepository factoringRepository;
    private final IMarcheService marcheService;
    private final IBanqueService banqueService;
    private final FactoringMapper factoringMapper;
    private final LigneFonctionnelleSpecifiqueReader ligneFonctionnelleSpecifiqueReader;
    private final LigneFonctionnelleReader ligneFonctionnelleReader;
    private final INatureLigneFonctionnelleService natureLigneFonctionnelleService;
    private final StaportPdfReportService staportPdfReportService;

    @PersistenceContext
    private EntityManager entityManager;

    public FactoringServiceImpl(FactoringRepository factoringRepository,
                                 IMarcheService marcheService,
                                 IBanqueService banqueService,
                                 FactoringMapper factoringMapper,
                                 LigneFonctionnelleSpecifiqueReader ligneFonctionnelleSpecifiqueReader,
                                 LigneFonctionnelleReader ligneFonctionnelleReader,
                                 INatureLigneFonctionnelleService natureLigneFonctionnelleService,
                                 StaportPdfReportService staportPdfReportService) {
        this.factoringRepository = factoringRepository;
        this.marcheService = marcheService;
        this.banqueService = banqueService;
        this.factoringMapper = factoringMapper;
        this.ligneFonctionnelleSpecifiqueReader = ligneFonctionnelleSpecifiqueReader;
        this.ligneFonctionnelleReader = ligneFonctionnelleReader;
        this.natureLigneFonctionnelleService = natureLigneFonctionnelleService;
        this.staportPdfReportService = staportPdfReportService;
        log.info("FactoringServiceImpl initialisé avec succès");
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = Factoring.class)
    public FactoringResponseDto createFactoring(FactoringRequestDto requestDTO) throws Exception {
        log.info("Début de la création d'un factoring pour le marché ID: {}", requestDTO.marcheId());

        Marche marche = marcheService.findOrThrow(requestDTO.marcheId());
        log.debug("Marché récupéré: {} (Numéro: {})", marche.getNom(), marche.getNumero());

        NatureLigneFonctionnelle factoringNature = getFactoringNature();
        LigneResult ligneResult = findActiveFactoringLine(marche, factoringNature);

        Factoring factoring = factoringMapper.toEntity(requestDTO, marche);
        factoring.setLigneFonctionnelleSpecifique(ligneResult.specifique());
        factoring.setLigneFonctionnelle(ligneResult.generale());
        factoring.setObservation(marche.getNumero());
        factoring.setLibelle(marche.getNom() + " - " + factoring.getType());
        factoring = factoringRepository.save(factoring);

        log.info("Factoring créé avec succès - ID: {}", factoring.getId());

        return factoringMapper.toDto(factoring);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Factoring.class)
    public FactoringResponseDto updateFactoring(Long id, FactoringRequestDto requestDTO) throws Exception {
        log.info("Début de la modification du factoring ID: {}", id);

        Factoring existingFactoring = factoringRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Factoring non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Factoring not found with ID: " + id);
                });

        Marche marche = marcheService.findOrThrow(requestDTO.marcheId());
        log.debug("Marché récupéré pour la mise à jour: {}", marche.getNom());

        if (!existingFactoring.getMarche().getId().equals(marche.getId())) {
            log.warn("Changement de marché détecté (ancien: {}, nouveau: {}), re-validation nécessaire",
                    existingFactoring.getMarche().getNom(), marche.getNom());

            NatureLigneFonctionnelle factoringNature = getFactoringNature();
            LigneResult ligneResult = findActiveFactoringLine(marche, factoringNature);
            existingFactoring.setLigneFonctionnelleSpecifique(ligneResult.specifique());
            existingFactoring.setLigneFonctionnelle(ligneResult.generale());
        }

        existingFactoring.setMarche(marche);
        existingFactoring.setDateEmission(requestDTO.dateEmission());
        existingFactoring.setMontant(requestDTO.montant());
        existingFactoring.setType(requestDTO.type());
        existingFactoring.setObservation(marche.getNumero());
        existingFactoring.setLibelle(marche.getNom() + " - " + existingFactoring.getType());

        Factoring updatedFactoring = factoringRepository.save(existingFactoring);
        log.info("Factoring modifié avec succès - ID: {}", id);

        return factoringMapper.toDto(updatedFactoring);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = Factoring.class)
    public void deleteFactoring(Long id) {
        log.info("Tentative de suppression du factoring ID: {}", id);

        Factoring factoring = factoringRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Impossible de supprimer - Factoring non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Factoring not found with ID: " + id);
                });

        if (factoring.getStatus() != Status.PREVU) {
            log.warn("Tentative de suppression d'un factoring déjà réglé — id: {}", id);
            throw new ValidationException("Le factoring id: " + id + " est déjà réglé et ne peut plus être supprimé.");
        }

        factoringRepository.deleteById(id);
        log.info("Factoring ID: {} supprimé avec succès", id);
    }

    @Override
    @Transactional(readOnly = true)
    public FactoringResponseDto findById(Long id) {
        Factoring factoring = factoringRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Factoring non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Factoring not found with ID: " + id);
                });

        return factoringMapper.toDto(factoring);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FactoringResponseDto> findAll(FactoringSearchDto searchDTO, Pageable pageable) {
        log.info("Recherche de factorings avec critères: {}", searchDTO);

        Specification<Factoring> spec = FactoringSpec.searchByCriteria(searchDTO);
        Page<Factoring> factoringPage = factoringRepository.findAll(spec,
                pageable.getSort().isSorted() ? pageable
                        : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.info("Recherche terminée - {} résultat(s) trouvé(s) sur {} élément(s) au total",
                factoringPage.getNumberOfElements(), factoringPage.getTotalElements());

        return factoringPage.map(factoringMapper::toDto);
    }

    @Override
    @Transactional
    public FactoringResponseDto toggleStatus(Long id) {
        log.info("Changement du statut du factoring ID: {}", id);

        Factoring factoring = factoringRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Factoring non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Factoring not found with ID: " + id);
                });

        Status existingStatus = factoring.getStatus();
        Status newStatus = existingStatus == Status.PAYE ? Status.PREVU : Status.PAYE;

        log.debug("Changement de statut - Ancien: {}, Nouveau: {}", existingStatus, newStatus);
        factoring.setStatus(newStatus);
        Factoring updatedFactoring = factoringRepository.save(factoring);

        log.info("Statut du factoring ID: {} modifié avec succès de {} vers {}", id, existingStatus, newStatus);

        return factoringMapper.toDto(updatedFactoring);
    }

    @Override
    @Transactional(readOnly = true)
    public EncoursFactoringDto getRemboursementEncoursForBanque(EncoursFactoringSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Récupération des encours de remboursement factoring pour la banque ID: {}", banqueId);

        LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        Specification<Factoring> spec = FactoringSpec.byBanqueId(banqueId)
                .and(FactoringSpec.outstandingRemboursementUntil(endOfMonth))
                .and(FactoringSpec.byMarcheName(searchDto.marcheName()))
                .and(FactoringSpec.byMarcheNumero(searchDto.marcheNumero()))
                .and(FactoringSpec.hasMontant(searchDto.montant()));

        Page<Factoring> factoringPage = factoringRepository.findAll(spec,
                pageable.getSort().isSorted() ? pageable
                        : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        BigDecimal totalDesEncours = sumMontant(spec);
        log.info("Total des encours de remboursement factoring calculé: {} pour la banque ID: {}", totalDesEncours, banqueId);

        Page<FactoringResponseDto> pageDto = factoringPage.map(factoringMapper::toDto);

        return new EncoursFactoringDto(totalDesEncours, pageDto);
    }

    @Override
    @Transactional(readOnly = true)
    public EncoursFactoringDto getDeblocageEncoursForBanque(EncoursFactoringSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Récupération des encours de déblocage factoring pour la banque ID: {}", banqueId);

        LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        Specification<Factoring> spec = FactoringSpec.byBanqueId(banqueId)
                .and(FactoringSpec.outstandingDeblocageUntil(endOfMonth))
                .and(FactoringSpec.byMarcheName(searchDto.marcheName()))
                .and(FactoringSpec.byMarcheNumero(searchDto.marcheNumero()))
                .and(FactoringSpec.hasMontant(searchDto.montant()));

        Page<Factoring> factoringPage = factoringRepository.findAll(spec,
                pageable.getSort().isSorted() ? pageable
                        : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        BigDecimal totalDesEncours = sumMontant(spec);
        log.info("Total des encours de déblocage factoring calculé: {} pour la banque ID: {}", totalDesEncours, banqueId);

        Page<FactoringResponseDto> pageDto = factoringPage.map(factoringMapper::toDto);

        return new EncoursFactoringDto(totalDesEncours, pageDto);
    }

    // ── Export PDF ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] exportPdf(FactoringSearchDto searchDTO) {
        log.info("Export PDF factorings — critères: {}", searchDTO);

        List<Factoring> factorings = factoringRepository.findAll(
                FactoringSpec.searchByCriteria(searchDTO),
                Sort.by(Sort.Direction.DESC, "dateEmission"));

        BigDecimal totalMontant = factorings.stream()
                .map(Factoring::getMontant)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> filterLines = buildFilterSummary(searchDTO);

        return generateFactoringPdf("État des Factorings", factorings, totalMontant, filterLines);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportRemboursementEncoursPdf(EncoursFactoringSearchDto searchDto, Long banqueId) {
        log.info("Export PDF encours remboursement factoring — banque ID: {}", banqueId);

        LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
        Specification<Factoring> spec = FactoringSpec.byBanqueId(banqueId)
                .and(FactoringSpec.outstandingRemboursementUntil(endOfMonth))
                .and(FactoringSpec.byMarcheName(searchDto.marcheName()))
                .and(FactoringSpec.byMarcheNumero(searchDto.marcheNumero()))
                .and(FactoringSpec.hasMontant(searchDto.montant()));

        List<Factoring> factorings = factoringRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "dateEmission"));
        BigDecimal totalMontant = sumMontant(spec);
        List<String> filterLines = buildEncoursFilterSummary(searchDto, banqueId);

        return generateFactoringPdf("Encours de Remboursement — Factoring", factorings, totalMontant, filterLines);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportDeblocageEncoursPdf(EncoursFactoringSearchDto searchDto, Long banqueId) {
        log.info("Export PDF encours déblocage factoring — banque ID: {}", banqueId);

        LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
        Specification<Factoring> spec = FactoringSpec.byBanqueId(banqueId)
                .and(FactoringSpec.outstandingDeblocageUntil(endOfMonth))
                .and(FactoringSpec.byMarcheName(searchDto.marcheName()))
                .and(FactoringSpec.byMarcheNumero(searchDto.marcheNumero()))
                .and(FactoringSpec.hasMontant(searchDto.montant()));

        List<Factoring> factorings = factoringRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "dateEmission"));
        BigDecimal totalMontant = sumMontant(spec);
        List<String> filterLines = buildEncoursFilterSummary(searchDto, banqueId);

        return generateFactoringPdf("Encours de Déblocage — Factoring", factorings, totalMontant, filterLines);
    }

    /**
     * Génère le contenu PDF partagé par {@link #exportPdf}, {@link #exportRemboursementEncoursPdf}
     * et {@link #exportDeblocageEncoursPdf}.
     */
    private byte[] generateFactoringPdf(String title, List<Factoring> factorings,
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
                                    + "  •  " + factorings.size() + " ligne(s)";
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

                            float[] colWidths = {10f, 16f, 12f, 11f, 13f, 11f, 11f, 8f};
                            PdfPTable table = new PdfPTable(colWidths);
                            table.setWidthPercentage(100f);
                            table.setSpacingBefore(8f);
                            table.setHeaderRows(1);

                            for (String h : new String[]{
                                    "N° Marché", "Nom Marché", "Banque", "Type",
                                    "Montant (MAD)", "Date Émission", "Date Valeur", "Statut"}) {
                                PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                                c.setBackgroundColor(medBlue);
                                c.setPadding(5f);
                                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                                c.setBorderColor(Color.WHITE);
                                c.setBorderWidth(0.5f);
                                table.addCell(c);
                            }

                            for (int i = 0; i < factorings.size(); i++) {
                                Factoring f = factorings.get(i);
                                Marche marche = f.getMarche();
                                Color rowBg = i % 2 == 0 ? Color.WHITE : rowAlt;

                                addPdfCell(table, marche != null ? marche.getNumero() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, marche != null ? marche.getNom() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, marche != null && marche.getBanque() != null ? marche.getBanque().getNom() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, f.getType() == Type.DEBLOCAGE ? "Déblocage" : "Remboursement", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);
                                addPdfCell(table, formatMontantPdf(f.getMontant()), cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);
                                addPdfCell(table, f.getDateEmission() != null ? f.getDateEmission().format(DATE_FMT) : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);
                                addPdfCell(table, f.getDateValeur() != null ? f.getDateValeur().format(DATE_FMT) : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);

                                boolean paye = f.getStatus() == Status.PAYE;
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
                                    new Phrase("TOTAL — " + factorings.size() + " ligne(s)", totalFont));
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

                            for (int j = 0; j < 3; j++) {
                                PdfPCell empty = new PdfPCell(new Phrase(""));
                                empty.setBackgroundColor(lightBlue);
                                empty.setBorderColor(medBlue);
                                table.addCell(empty);
                            }

                            doc.add(table);

                        } catch (DocumentException e) {
                            throw new RuntimeException("Erreur génération contenu PDF factorings", e);
                        }
                    }
            );
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des factorings", e);
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

    private List<String> buildFilterSummary(FactoringSearchDto criteria) {
        List<String> lines = new ArrayList<>();
        if (criteria == null) return lines;

        if (criteria.marcheId() != null) {
            try {
                lines.add("Marché : " + marcheService.findOrThrow(criteria.marcheId()).getNumero());
            } catch (ResourceNotFoundException ignored) { /* filtre obsolète — ignoré pour l'affichage */ }
        }
        if (criteria.banqueId() != null) {
            try {
                lines.add("Banque : " + banqueService.findOrThrow(criteria.banqueId()).getNom());
            } catch (ResourceNotFoundException ignored) { /* filtre obsolète — ignoré pour l'affichage */ }
        }
        if (criteria.type() != null)
            lines.add("Type : " + (criteria.type() == Type.DEBLOCAGE ? "Déblocage" : "Remboursement"));
        if (criteria.status() != null)
            lines.add("Statut : " + (criteria.status() == Status.PAYE ? "Payé" : "Prévu"));

        return lines;
    }

    private List<String> buildEncoursFilterSummary(EncoursFactoringSearchDto searchDto, Long banqueId) {
        List<String> lines = new ArrayList<>();

        try {
            lines.add("Banque : " + banqueService.findOrThrow(banqueId).getNom());
        } catch (ResourceNotFoundException ignored) { /* filtre obsolète — ignoré pour l'affichage */ }

        if (searchDto == null) return lines;
        if (searchDto.marcheName() != null && !searchDto.marcheName().isBlank())
            lines.add("Nom Marché : " + searchDto.marcheName());
        if (searchDto.marcheNumero() != null && !searchDto.marcheNumero().isBlank())
            lines.add("N° Marché : " + searchDto.marcheNumero());
        if (searchDto.montant() != null)
            lines.add("Montant : " + formatMontantPdf(searchDto.montant()) + " MAD");

        return lines;
    }

    /**
     * Somme des montants correspondant à la {@link Specification} donnée, calculée en base
     * (agrégat SQL) — jamais en chargeant les entités en mémoire pour sommer côté Java
     * (conventions/performance.md).
     */
    private BigDecimal sumMontant(Specification<Factoring> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> query = cb.createQuery(BigDecimal.class);
        Root<Factoring> root = query.from(Factoring.class);
        query.select(cb.coalesce(cb.sum(root.get("montant")), BigDecimal.ZERO));
        var predicate = spec.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }
        return entityManager.createQuery(query).getSingleResult();
    }

    private NatureLigneFonctionnelle getFactoringNature() {
        log.debug("Recherche de la nature de ligne fonctionnelle Factoring (code={})", FACTORING_NATURE_CODE);
        return natureLigneFonctionnelleService.findByCode(FACTORING_NATURE_CODE)
                .orElseThrow(() -> {
                    log.error("Nature de ligne fonctionnelle 'FACTORING' introuvable en base");
                    return new ResourceNotFoundException(
                            "Nature de ligne fonctionnelle FACTORING introuvable. " +
                            "Vérifiez la table nature_ligne_fonctionnelle."
                    );
                });
    }

    /**
     * Recherche et retourne la ligne fonctionnelle Factoring active pour un marché.
     * Priorité : spécifique > générale liée au marché.
     * Lève une exception si aucune ligne n'est trouvée.
     */
    private LigneResult findActiveFactoringLine(Marche marche, NatureLigneFonctionnelle factoringNature) throws Exception {
        log.debug("Recherche d'une ligne Factoring active pour le marché '{}' (ID: {})", marche.getNom(), marche.getId());

        Optional<LigneFonctionnelleSpecifique> ligneSpecifique =
                ligneFonctionnelleSpecifiqueReader.findByMarcheAndNatureLigneFonctionnelleAndActifTrue(marche, factoringNature);
        if (ligneSpecifique.isPresent()) {
            log.info("Ligne spécifique Factoring active trouvée pour le marché '{}' (ID: {})",
                    marche.getNom(), ligneSpecifique.get().getId());
            return new LigneResult(ligneSpecifique.get(), null);
        }

        Optional<LigneFonctionnelle> ligneGenerale =
                ligneFonctionnelleReader.findActiveLineForMarche(marche.getId(), factoringNature.getId());
        if (ligneGenerale.isPresent()) {
            log.info("Ligne générale Factoring active trouvée pour le marché '{}' (ID: {})",
                    marche.getNom(), ligneGenerale.get().getId());
            return new LigneResult(null, ligneGenerale.get());
        }

        log.error("Aucune ligne Factoring active pour le marché '{}'", marche.getNom());
        throw new Exception(
                "Impossible de créer un factoring. Le marché '" + marche.getNom() +
                "' n'est associé à aucune ligne fonctionnelle Factoring active " +
                "(ni spécifique, ni générale liée à ce marché)."
        );
    }

    /** Résultat de la recherche de ligne : exactement l'une des deux est non-null. */
    private record LigneResult(LigneFonctionnelleSpecifique specifique, LigneFonctionnelle generale) {}
}
