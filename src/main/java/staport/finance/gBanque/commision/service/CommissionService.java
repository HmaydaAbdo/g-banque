package staport.finance.gBanque.commision.service;


import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import staport.finance.gBanque.commision.dtos.CommissionFusionRequest;
import staport.finance.gBanque.commision.dtos.CommissionFusionResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories.NatureLigneFonctionnelleRepository;
import staport.finance.gBanque.commision.dtos.*;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.commision.mapper.CommissionMapper;
import staport.finance.gBanque.commision.repository.CommisionRepository;
import staport.finance.gBanque.commision.specs.CommissionSpecifications;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.impot_taxes.repositories.ImpotEtTaxeRepository;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.reports.service.StaportPdfReportService;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


@Service
public class CommissionService implements ICommissionService {

    private static final Logger log = LogManager.getLogger(CommissionService.class);
    /** Taux TVA légal applicable aux commissions sur cautions bancaires (10%). */
    private static final BigDecimal TAUX_TVA_COMMISSION_CAUTION = new BigDecimal("0.10");

    @PersistenceContext
    private EntityManager entityManager;

    private final CommisionRepository       commissionRepository;
    private final BanqueRepository          banqueRepository;
    private final CommissionMapper          commissionMapper;
    private final ImpotEtTaxeRepository     impotEtTaxeRepository;
    private final ICommissionConfigService  commissionConfigService;
    private final NatureLigneFonctionnelleRepository natureRepository;
    private final StaportPdfReportService   staportPdfReportService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

    public CommissionService(CommisionRepository commissionRepository,
                             BanqueRepository banqueRepository,
                             CommissionMapper commissionMapper,
                             ImpotEtTaxeRepository impotEtTaxeRepository,
                             ICommissionConfigService commissionConfigService,
                             NatureLigneFonctionnelleRepository natureRepository,
                             StaportPdfReportService staportPdfReportService) {
        this.commissionRepository   = commissionRepository;
        this.banqueRepository       = banqueRepository;
        this.commissionMapper       = commissionMapper;
        this.impotEtTaxeRepository  = impotEtTaxeRepository;
        this.commissionConfigService = commissionConfigService;
        this.natureRepository       = natureRepository;
        this.staportPdfReportService = staportPdfReportService;
    }

    // --- Create ---
    @Transactional
    public CommissionResponseDto createCommission(CommissionRequestDto requestDto) {
        log.info("Creating commission for banqueId={} libelle={}", requestDto.getBanqueId(), requestDto.getLibelle());

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque not found with id={}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque not found");
                });

        Commission commission = commissionMapper.toEntity(requestDto, banque);
        commissionRepository.save(commission);

        log.info("Commission created with id={}", commission.getId());
        return commissionMapper.toResponseDto(commission);
    }

    // --- Get by ID ---
    @Transactional(readOnly = true)
    public CommissionResponseDto getById(Long id) {
        log.info("Fetching commission with id={}", id);

        Commission commission = commissionRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Commission not found with id={}", id);
                    return new IllegalArgumentException("Commission not found");
                });

        return commissionMapper.toResponseDto(commission);
    }

    // --- Search with pagination + aggregate total ---
    @Transactional(readOnly = true)
    public EncoursCommissionDto searchCommissions(CommissionSearchDto searchDto, Pageable pageable) {
        log.info("Searching commissions with criteria: {}", searchDto);

        Specification<Commission> spec = CommissionSpecifications.buildSpecification(searchDto);

        Pageable stablePageable = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));

        Page<CommissionResponseDto> page = commissionRepository
                .findAll(spec, stablePageable)
                .map(commissionMapper::toResponseDto);

        BigDecimal total = sumMontant(spec);

        EncoursCommissionDto result = new EncoursCommissionDto();
        result.setPage(page);
        result.setTotalDesEncours(total);
        return result;
    }

    // --- Update ---
    @Transactional
    public CommissionResponseDto updateCommission(Long id, CommissionRequestDto requestDto) {
        log.info("Updating commission id={} with libelle={}", id, requestDto.getLibelle());

        Commission commission = commissionRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Commission not found with id={}", id);
                    return new IllegalArgumentException("Commission not found");
                });

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque not found with id={}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque not found");
                });

        commission.setLibelle(requestDto.getLibelle());
        commission.setMontant(requestDto.getMontant());
        commission.setBanque(banque);
        commission.setOperation(requestDto.getOperation());
        commission.setDateEmission(requestDto.getDateEmission());

        commissionRepository.save(commission);
        log.info("Commission updated successfully id={}", commission.getId());

        // Cascade TVA : uniquement pour les commissions sur cautions
        if (TransactionSource.CAUTION.equals(commission.getOperation())) {
            impotEtTaxeRepository.findByCommission_Id(id).ifPresent(tva -> {
                BigDecimal nouveauMontantTva = requestDto.getMontant()
                        .multiply(TAUX_TVA_COMMISSION_CAUTION)
                        .setScale(2, RoundingMode.HALF_UP);
                log.info("Mise à jour TVA liée — commission id={}, ancien montant TVA={}, nouveau montant TVA={}",
                        id, tva.getMontant(), nouveauMontantTva);
                tva.setMontant(nouveauMontantTva);
                impotEtTaxeRepository.save(tva);
            });
        }

        return commissionMapper.toResponseDto(commission);
    }


    // --- Delete ---
    @Transactional
    public void deleteCommission(Long id) {
        log.info("Deleting commission with id={}", id);

        Commission commission = commissionRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Commission not found with id={}", id);
                    return new IllegalArgumentException("Commission not found");
                });

        // Cascade TVA : uniquement pour les commissions sur cautions
        if (TransactionSource.CAUTION.equals(commission.getOperation())) {
            impotEtTaxeRepository.findByCommission_Id(id).ifPresent(tva -> {
                log.info("Suppression TVA liée — commission id={}, montant TVA={}",
                        id, tva.getMontant());
                impotEtTaxeRepository.delete(tva);
            });
        }

        commissionRepository.delete(commission);
        log.info("Commission deleted successfully id={}", id);
    }
    /**
     * Retrieves a paginated list of outstanding commissions and calculates their total amount.
     * Outstanding commissions are those with a 'PREVU' status and an emission date in the past.
     *
     * @param pageable Pagination and sorting information.
     * @param searchDto Optional search criteria (e.g., for filtering by bank, libelle, etc.).
     * @return An EncoursCommissionDto containing the page of commissions and the total outstanding amount.
     */
    @Transactional(readOnly = true)
    public EncoursCommissionDto getOutstandingCommissions(CommissionSearchDto searchDto, Pageable pageable) {
        // Build the specification to filter the records
        Specification<Commission> spec = Specification.where(CommissionSpecifications.outstandingUntil(endOfMonth))
                .and(CommissionSpecifications.hasBanque(searchDto.getBanqueId()))
                .and(CommissionSpecifications.hasLibelle(searchDto.getLibelle()))
                .and(CommissionSpecifications.hasOperation(searchDto.getOperation()))
                .and(CommissionSpecifications.hasMontant(searchDto.getMontant()));

        // Garantir un tri stable par id pour éviter les doublons inter-pages
        Pageable stablePageable = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));

        Page<CommissionResponseDto> pageDto = commissionRepository
                .findAll(spec, stablePageable)
                .map(commissionMapper::toResponseDto);

        BigDecimal totalDesEncours = sumMontant(spec);

        EncoursCommissionDto result = new EncoursCommissionDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        return result;
    }


    // ── Export PDF ────────────────────────────────────────────────────────────

    /**
     * Génère un export PDF de l'intégralité des commissions correspondant aux critères fournis
     * (mêmes filtres que {@link #searchCommissions}, {@code natureId} inclus) — sans pagination :
     * le fichier imprime la totalité des résultats filtrés, pas seulement la page affichée à l'écran.
     */
    @Transactional(readOnly = true)
    public byte[] exporterPdf(CommissionSearchDto searchDto) {
        log.info("Export PDF commissions — critères: {}", searchDto);

        Specification<Commission> spec = CommissionSpecifications.buildSpecification(searchDto);
        List<Commission> commissions = commissionRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "dateEmission"));

        BigDecimal totalMontant = sumMontant(spec);
        List<String> filterLines = buildFilterSummary(searchDto);

        try {
            return staportPdfReportService.generateReport(
                "État des Commissions",
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

                        Paragraph title = new Paragraph("État des Commissions", titleFont);
                        title.setAlignment(Element.ALIGN_CENTER);
                        doc.add(title);

                        String exportInfo = "Exporté le " + LocalDate.now().format(DATE_FMT)
                                + "  •  " + commissions.size() + " commission(s)";
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

                        float[] colWidths = {16f, 12f, 11f, 11f, 11f, 10f, 8f};
                        PdfPTable table = new PdfPTable(colWidths);
                        table.setWidthPercentage(100f);
                        table.setSpacingBefore(8f);
                        table.setHeaderRows(1);

                        for (String h : new String[]{
                                "Libellé", "Banque", "Nature", "Montant commission (MAD)",
                                "Montant caution (MAD)", "Date émission", "Statut"}) {
                            PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                            c.setBackgroundColor(medBlue);
                            c.setPadding(5f);
                            c.setHorizontalAlignment(Element.ALIGN_CENTER);
                            c.setBorderColor(Color.WHITE);
                            c.setBorderWidth(0.5f);
                            table.addCell(c);
                        }

                        for (int i = 0; i < commissions.size(); i++) {
                            Commission c = commissions.get(i);
                            Color rowBg = i % 2 == 0 ? Color.WHITE : rowAlt;

                            addPdfCell(table, c.getLibelle(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                            addPdfCell(table, c.getBanque() != null ? c.getBanque().getNom() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                            addPdfCell(table, (c.getCaution() != null && c.getCaution().getNature() != null)
                                    ? c.getCaution().getNature().getLabel() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                            addPdfCell(table, formatMontantPdf(c.getMontant()), cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);
                            addPdfCell(table, c.getCaution() != null ? formatMontantPdf(c.getCaution().getMontant()) : "—", cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);
                            addPdfCell(table, c.getDateEmission() != null ? c.getDateEmission().format(DATE_FMT) : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);

                            boolean paye = c.getStatus() == Status.PAYE;
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
                                new Phrase("TOTAL — " + commissions.size() + " commission(s)", totalFont));
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

                        for (int j = 0; j < 3; j++) {
                            PdfPCell empty = new PdfPCell(new Phrase(""));
                            empty.setBackgroundColor(lightBlue);
                            empty.setBorderColor(medBlue);
                            table.addCell(empty);
                        }

                        doc.add(table);

                    } catch (DocumentException e) {
                        throw new RuntimeException("Erreur génération contenu PDF commissions", e);
                    }
                }
            );
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des commissions", e);
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

    private List<String> buildFilterSummary(CommissionSearchDto dto) {
        List<String> lines = new ArrayList<>();
        if (dto == null) return lines;

        if (dto.getBanqueId() != null)
            banqueRepository.findById(dto.getBanqueId())
                .ifPresent(b -> lines.add("Banque : " + b.getNom()));
        if (dto.getLibelle() != null && !dto.getLibelle().isBlank())
            lines.add("Libellé : " + dto.getLibelle());
        if (dto.getOperation() != null)
            lines.add("Opération : " + dto.getOperation());
        if (dto.getMontant() != null)
            lines.add("Montant : " + formatMontantPdf(dto.getMontant()) + " MAD");
        if (dto.getStatus() != null)
            lines.add("Statut : " + (dto.getStatus() == Status.PAYE ? "Payée" : "Prévue"));
        if (dto.getNatureId() != null)
            natureRepository.findById(dto.getNatureId())
                .ifPresent(n -> lines.add("Nature : " + n.getLabel()));

        return lines;
    }

    /**
     * Supprime toutes les commissions liées à un contrat de leasing.
     * Utilise le numéro de contrat pour identifier les commissions via leur libellé.
     *
     * @param numeroContrat Le numéro du contrat de leasing
     */
    @Transactional
    public void deleteCommissionsByContratLeasing(String numeroContrat) {
        log.info("Début de la suppression des commissions pour le contrat N° : {}", numeroContrat);

        if (numeroContrat == null || numeroContrat.trim().isEmpty()) {
            log.warn("Tentative de suppression de commissions avec un numéro de contrat vide");
            return;
        }

        // Pattern de recherche : toutes les commissions dont le libellé contient le numéro du contrat
        String libellePattern = "Commission Prelevement Leasing contrat N°" + numeroContrat + "%";

        log.debug("Pattern de recherche : {}", libellePattern);

        // Rechercher toutes les commissions correspondantes (sans filtre de date)
        List<Commission> commissions = commissionRepository.findByLibelleLike(libellePattern);

        if (commissions.isEmpty()) {
            log.info("Aucune commission trouvée pour le contrat N° : {}", numeroContrat);
            return;
        }

        int nombreCommissions = commissions.size();
        log.debug("Nombre de commissions trouvées : {}", nombreCommissions);

        // Supprimer toutes les commissions
        commissionRepository.deleteAll(commissions);

        log.info("Suppression terminée : {} commission(s) supprimée(s) pour le contrat N° : {}",
                nombreCommissions, numeroContrat);

    }

    // ── Fusion ────────────────────────────────────────────────────────────────
    /**
     * Fusionne plusieurs commissions sur cautions en une seule.
     *
     * <p>Contraintes validées :
     * <ul>
     *   <li>Minimum 2 IDs</li>
     *   <li>Toutes {@code operation = CAUTION}, {@code status = PREVU}</li>
     *   <li>Même banque et même {@code dateEmission} pour toutes</li>
     * </ul>
     *
     * <p>Opérations atomiques (tout ou rien) :
     * <ol>
     *   <li>Suppression des TVA liées aux commissions sources (si {@code genererTva = true})</li>
     *   <li>Suppression des commissions sources</li>
     *   <li>Création de la commission fusionnée</li>
     *   <li>Création de la TVA fusionnée (10 %) si {@code genererTva = true}</li>
     * </ol>
     */
    @Transactional
    public CommissionFusionResponse fusionner(CommissionFusionRequest request) {
        List<Long> ids = request.getIds();
        log.info("Fusion demandée — {} commission(s), libellé='{}'", ids.size(), request.getLibelle());

        // ── 1. Chargement ────────────────────────────────────────────────────
        List<Commission> sources = commissionRepository.findAllById(ids);

        if (sources.size() != ids.size()) {
            List<Long> trouvees  = sources.stream().map(Commission::getId).toList();
            List<Long> manquantes = ids.stream().filter(id -> !trouvees.contains(id)).toList();
            throw new IllegalArgumentException("Commission(s) introuvable(s) : " + manquantes);
        }

        // ── 2. Validations métier ────────────────────────────────────────────
        for (Commission c : sources) {
            if (!TransactionSource.CAUTION.equals(c.getOperation())) {
                throw new IllegalArgumentException(
                        "La commission id=" + c.getId() + " n'est pas de type CAUTION.");
            }
            if (!Status.PREVU.equals(c.getStatus())) {
                throw new IllegalArgumentException(
                        "La commission id=" + c.getId() + " est déjà réglée (PAYE) — fusion impossible.");
            }
        }

        // Même banque
        long nbBanques = sources.stream().map(c -> c.getBanque().getId()).distinct().count();
        if (nbBanques > 1) {
            throw new IllegalArgumentException(
                    "Toutes les commissions doivent appartenir à la même banque.");
        }

        // Fusion interdite pour BMCI (TVA séparée — la fusion briserait la cohérence comptable)
        String nomBanque = sources.get(0).getBanque().getNom();
        if (nomBanque != null && nomBanque.toUpperCase().contains("BMCI")) {
            throw new IllegalArgumentException(
                    "La fusion n'est pas autorisée pour les commissions BMCI (TVA séparée).");
        }



        Banque banque          = sources.get(0).getBanque();
        LocalDate dateEmission = sources.get(0).getDateEmission();

        // ── 3. Calcul du montant fusionné (TTC = somme HT × 1,10) ────────────
        BigDecimal montantHt = sources.stream()
                .map(Commission::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal montantFusionne = montantHt
                .multiply(BigDecimal.ONE.add(TAUX_TVA_COMMISSION_CAUTION))
                .setScale(2, RoundingMode.HALF_UP);

        // ── 4. Suppression des TVA liées aux sources (toujours) ──────────────
        impotEtTaxeRepository.deleteByCommissionIdIn(ids);
        log.info("TVA sources supprimées pour les commissions {}", ids);

        // ── 5. Suppression des commissions sources ───────────────────────────
        commissionRepository.deleteAllById(ids);
        log.info("Commissions sources supprimées : {}", ids);

        // ── 6. Création de la commission fusionnée ────────────────────────────
        Commission fusionnee = new Commission();
        fusionnee.setLibelle(request.getLibelle());
        fusionnee.setMontant(montantFusionne);
        fusionnee.setBanque(banque);
        fusionnee.setOperation(TransactionSource.CAUTION);
        fusionnee.setDateEmission(dateEmission);
        fusionnee.setStatus(Status.PREVU);
        fusionnee = commissionRepository.save(fusionnee);
        log.info("Commission fusionnée créée — id={}, montant={}", fusionnee.getId(), montantFusionne);

        // Aucune TVA créée après fusion
        return new CommissionFusionResponse(commissionMapper.toResponseDto(fusionnee));
    }

    // ── Aggregate helper ──────────────────────────────────────────────────────
    /**
     * Émet un seul {@code SELECT SUM(c.montant) FROM Commission c WHERE ...}
     * sans charger les entités en mémoire.
     */
    private BigDecimal sumMontant(Specification<Commission> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> cq = cb.createQuery(BigDecimal.class);
        Root<Commission> root = cq.from(Commission.class);
        cq.select(cb.sum(root.get("montant")));
        Predicate predicate = spec.toPredicate(root, cq, cb);
        if (predicate != null) {
            cq.where(predicate);
        }
        BigDecimal result = entityManager.createQuery(cq).getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }

    @Transactional
    public void toggleCommisionStatus(Long commissionId) {

        Commission commission = commissionRepository.findById(commissionId)
                .orElseThrow(() -> {
                    log.error("Commission avec l'id {} introuvable", commissionId);
                    return new ResourceNotFoundException(
                            "Commission avec l'id " + commissionId + " introuvable"
                    );
                });

        log.info("Commission avec l'id {} trouvée. Statut actuel : {}", commissionId, commission.getStatus());
        commission.setStatus(commission.getStatus() == Status.PAYE ? Status.PREVU : Status.PAYE);

        log.info("Statut de la commission avec l'id {} mis à jour. Nouveau statut : {}", commissionId, commission.getStatus());

        commissionRepository.save(commission);
    }


    @Transactional
    public void generateCommissionIfNeeded(Virement virement) {

        // Guard 1 : banque émettrice obligatoire
        if (virement.getBanqueEmettrice() == null
                || virement.getBanqueEmettrice().getNom() == null) {
            return;
        }

        // Guard 2 : virement interne (bénéficiaire STAPORT, dédoublé encaissement/décaissement
        // par VirementPaymentProcessor) — opération entre banques Staport, aucune commission
        // n'est générée, ni côté encaissement ni côté décaissement.
        if (virement.getBeneficiaire() != null
                && virement.getBeneficiaire().trim().equalsIgnoreCase("STAPORT")) {
            log.debug("Virement {} interne (bénéficiaire STAPORT, {}) — commission non générée", virement.getNumero(), virement.getType());
            return;
        }

        // Guard 3 : uniquement les virements ENCOURS
        if (virement.getStatut() != StatutMoyenPaiement.ENCOURS) {
            return;
        }

        String nomEmettrice = virement.getBanqueEmettrice().getNom().trim();

        // Guard 4 : au moins une règle configurée pour cette banque + Virement
        if (!commissionConfigService.hasCommission(nomEmettrice, TransactionSource.Virement)) {
            log.debug("Aucune règle commission pour banque='{}' type=Virement — ignoré", nomEmettrice);
            return;
        }

        // Résolution de la banque bénéficiaire :
        // FK Banque en priorité, sinon champ texte libre (nomBanqueBeneficiaireLibre)
        String nomBeneficiaire = resolveBeneficiaireName(virement);

        // Résolution du montant : SI_MATCH si bénéficiaire = BMCI (27,50),
        // SI_NON_MATCH sinon (33,00)
        BigDecimal montant = commissionConfigService
                .resolveCommission(nomEmettrice, nomBeneficiaire, TransactionSource.Virement)
                .orElse(null);

        if (montant == null) {
            log.warn("Aucun montant résolu pour banque='{}', bénéficiaire='{}', type=Virement — commission non générée",
                    nomEmettrice, nomBeneficiaire);
            return;
        }

        // Idempotence : une seule commission par numéro de virement
        String libelle = buildLibelle(virement);
        if (commissionRepository.existsByOperationAndLibelle(TransactionSource.Virement, libelle)) {
            log.debug("Commission déjà existante pour virement {} — ignoré", virement.getNumero());
            return;
        }

        Commission commission = new Commission();
        commission.setMontant(montant);
        commission.setBanque(virement.getBanqueEmettrice());
        commission.setOperation(TransactionSource.Virement);
        commission.setDateEmission(virement.getDateEmission());
        commission.setLibelle(libelle);
        commission.setStatus(Status.PREVU);

        commissionRepository.save(commission);
        log.info("Commission créée — virement N°{}, émettrice='{}', bénéficiaire='{}', montant={}",
                virement.getNumero(), nomEmettrice, nomBeneficiaire, montant);
    }

    /**
     * Résout le nom de la banque bénéficiaire d'un virement.
     * Priorité : relation FK {@code banqueBeneficiare} → champ libre {@code nomBanqueBeneficiaireLibre}.
     * Retourne {@code null} si aucun n'est renseigné.
     */
    private String resolveBeneficiaireName(Virement virement) {
        if (virement.getBanqueBeneficiare() != null
                && virement.getBanqueBeneficiare().getNom() != null) {
            return virement.getBanqueBeneficiare().getNom().trim();
        }
        return virement.getNomBanqueBeneficiaireLibre() != null
                ? virement.getNomBanqueBeneficiaireLibre().trim()
                : null;
    }

    private String buildLibelle(Virement virement) {
        return "Commission virement N° " + virement.getNumero();
    }

}
