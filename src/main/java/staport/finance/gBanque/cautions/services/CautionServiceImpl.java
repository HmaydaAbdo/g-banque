package staport.finance.gBanque.cautions.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.INatureLigneFonctionnelleService;
import staport.finance.gBanque.cautions.commissions.factory.CommissionCautionStrategieFactory;
import staport.finance.gBanque.cautions.commissions.strategy.CommissionCautionStrategie;
import staport.finance.gBanque.cautions.dtos.request.CautionCreateDto;
import staport.finance.gBanque.cautions.dtos.response.CautionResponseDto;
import staport.finance.gBanque.cautions.dtos.request.CautionSearchCriteria;
import staport.finance.gBanque.cautions.dtos.request.CautionUpdateDto;
import staport.finance.gBanque.cautions.dtos.request.CloturerCautionRequest;
import staport.finance.gBanque.cautions.dtos.response.CloturerCautionResponse;
import staport.finance.gBanque.cautions.entities.Caution;
import staport.finance.gBanque.cautions.exceptions.CautionBusinessException;
import staport.finance.gBanque.cautions.mappers.CautionMapper;
import staport.finance.gBanque.cautions.repository.CautionRepository;
import staport.finance.gBanque.cautions.specifications.CautionSpecification;
import staport.finance.gBanque.commision.dtos.CommissionResponseDto;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.commision.service.ICommissionService;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.impot_taxes.enums.Motif;
import staport.finance.gBanque.impot_taxes.service.IImpotEtTaxeService;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.service.IMarcheService;
import staport.finance.gBanque.reports.service.StaportPdfReportService;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Implémentation du service Caution.
 *
 * <p>Encapsulation des domaines (architecture.md) : toute résolution d'une association
 * appartenant à un autre module passe exclusivement par la façade publique de ce module
 * ({@link IBanqueService}, {@link INatureLigneFonctionnelleService}, {@link IMarcheService},
 * {@link ICommissionService}, {@link IImpotEtTaxeService}) — jamais par injection directe
 * d'un {@code Repository} étranger.
 *
 * <p>Stratégie transactionnelle :
 * <ul>
 *   <li>Classe annotée {@code @Transactional(readOnly = true)} — toutes les lectures
 *       bénéficient du mode lecture seule (optimisation Hibernate + connexion pool).</li>
 *   <li>Méthodes d'écriture annotées {@code @Transactional(isolation = READ_COMMITTED)}
 *       — standard bancaire : évite les dirty reads, tolère les non-repeatable reads
 *       sur des données non critiques.</li>
 * </ul>
 *
 * <p>Anti-N+1 :
 * <ul>
 *   <li>{@code getById} → {@code findByIdAvecAssociations} avec EntityGraph (1 requête JOIN)</li>
 *   <li>{@code rechercher} → {@code findAll(spec, pageable)} avec EntityGraph sur la page courante</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class CautionServiceImpl implements ICautionService {

    private static final Logger log = LoggerFactory.getLogger(CautionServiceImpl.class);

    private final CautionRepository                  cautionRepository;
    private final CautionMapper                      cautionMapper;
    private final IBanqueService                     banqueService;
    private final INatureLigneFonctionnelleService    natureService;
    private final IMarcheService                     marcheService;
    private final ICommissionService                 commissionService;
    private final IImpotEtTaxeService                impotEtTaxeService;
    private final CommissionCautionStrategieFactory  strategieFactory;
    private final StaportPdfReportService            staportPdfReportService;

    /** Taux de TVA légal applicable aux commissions sur cautions bancaires (statutaire, non configurable). */
    private static final BigDecimal TAUX_TVA = new BigDecimal("0.10");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public CautionServiceImpl(
            CautionRepository cautionRepository,
            CautionMapper cautionMapper,
            IBanqueService banqueService,
            INatureLigneFonctionnelleService natureService,
            IMarcheService marcheService,
            ICommissionService commissionService,
            IImpotEtTaxeService impotEtTaxeService,
            CommissionCautionStrategieFactory strategieFactory,
            StaportPdfReportService staportPdfReportService) {
        this.cautionRepository    = cautionRepository;
        this.cautionMapper        = cautionMapper;
        this.banqueService        = banqueService;
        this.natureService        = natureService;
        this.marcheService        = marcheService;
        this.commissionService    = commissionService;
        this.impotEtTaxeService   = impotEtTaxeService;
        this.strategieFactory     = strategieFactory;
        this.staportPdfReportService = staportPdfReportService;
    }

    // ── Création ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.CREATE, entity = Caution.class)
    public CautionResponseDto creer(CautionCreateDto dto) {
        log.info("Création caution — numeroCaution: {}", dto.numeroCaution());

        // 1. Unicité du numéro de caution
        if (cautionRepository.existsByNumeroCaution(dto.numeroCaution())) {
            throw new DuplicateEntryException(
                "Une caution avec le numéro '" + dto.numeroCaution() + "' existe déjà."
            );
        }

        // 2. Résolution des associations — jamais via un Repository étranger
        Banque banque = banqueService.findOrThrow(dto.banqueId());
        NatureLigneFonctionnelle nature = natureService.findOrThrow(dto.natureId());
        Marche marche = dto.marcheId() != null ? marcheService.findOrThrow(dto.marcheId()) : null;

        // 3. Mapping + injection des associations
        Caution caution = cautionMapper.toEntity(dto);
        caution.setBanque(banque);
        caution.setNature(nature);
        caution.setMarche(marche);

        // 4. Persistance
        Caution sauvegardee = cautionRepository.save(caution);
        log.info("Caution créée — id: {}, numeroCaution: {}", sauvegardee.getId(), sauvegardee.getNumeroCaution());

        // 5. Prorata d'entrée — généré immédiatement à la création si la stratégie le supporte.
        //    dateEmission = dateDePrise (≠ fin de mois du batch) pour éviter tout conflit d'idempotence.
        strategieFactory.trouver(banque.getNom()).ifPresent(strategie -> {
            if (strategie.supporteProrataEntree()) {
                // Objet transitoire — ses champs portent le calcul métier de la stratégie ;
                // il n'est jamais persisté directement (encapsulation du module commission).
                Commission prorataCalcule = strategie.calculerProrataEntree(sauvegardee);

                CommissionResponseDto prorataPersiste = commissionService.creerCommissionCaution(
                        sauvegardee.getId(), banque.getId(), prorataCalcule.getMontant(),
                        prorataCalcule.getDateEmission(), prorataCalcule.getLibelle(),
                        prorataCalcule.getObservation());
                log.info("Prorata d'entrée créé — caution id: {}, dateEmission: {}, montant: {}",
                        sauvegardee.getId(), prorataPersiste.getDateEmission(), prorataPersiste.getMontant());

                // TVA sur prorata — uniquement pour les banques dont la stratégie l'exige (ex. BMCI)
                if (strategie.genererTva()) {
                    BigDecimal montantTva = prorataPersiste.getMontant()
                            .multiply(TAUX_TVA)
                            .setScale(2, RoundingMode.HALF_UP);

                    impotEtTaxeService.creerTvaSurCommission(
                            banque.getId(), montantTva, Motif.TVA_COMMISSION_CAUTION,
                            prorataPersiste.getDateEmission(), "TVA " + prorataPersiste.getLibelle(),
                            "TVA/prorata/caution/creation", prorataPersiste.getId(), sauvegardee.getId());
                    log.info("TVA prorata créée — montant: {}", montantTva);
                }
            }
        });

        // 6. Rechargement avec associations pour la réponse (garantit la cohérence du DTO)
        return toDto(sauvegardee.getId());
    }

    // ── Modification ──────────────────────────────────────────────────────────

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Caution.class)
    public CautionResponseDto modifier(Long id, CautionUpdateDto dto) {
        log.info("Modification caution — id: {}", id);

        Caution caution = trouverOuLeverException(id);

        // Règle métier : une caution inactive ne peut pas être modifiée
        if (Boolean.FALSE.equals(caution.getActif())) {
            throw new CautionBusinessException(
                "La caution id: " + id + " est archivée et ne peut pas être modifiée."
            );
        }

        // Vérification unicité numeroCaution si modification de ce champ
        if (!caution.getNumeroCaution().equals(dto.numeroCaution()) &&
                cautionRepository.existsByNumeroCautionAndIdNot(dto.numeroCaution(), id)) {
            throw new DuplicateEntryException(
                "Une caution avec le numéro '" + dto.numeroCaution() + "' existe déjà."
            );
        }

        // Résolution de toutes les associations (banque, nature, marche sont tous modifiables)
        Banque banque = banqueService.findOrThrow(dto.banqueId());
        NatureLigneFonctionnelle nature = natureService.findOrThrow(dto.natureId());
        Marche marche = dto.marcheId() != null ? marcheService.findOrThrow(dto.marcheId()) : null;

        // Application de tous les champs modifiables
        cautionMapper.applyUpdate(dto, caution);
        caution.setBanque(banque);
        caution.setNature(nature);
        caution.setMarche(marche);

        Caution modifiee = cautionRepository.save(caution);
        log.info("Caution modifiée — id: {}", modifiee.getId());

        return toDto(modifiee.getId());
    }

    // ── Lecture ───────────────────────────────────────────────────────────────

    @Override
    public CautionResponseDto getById(Long id) {
        log.debug("Récupération caution — id: {}", id);
        return toDto(id);
    }

    @Override
    public Page<CautionResponseDto> rechercher(CautionSearchCriteria criteres, Pageable pageable) {
        log.debug("Recherche cautions — critères: {}", criteres);

        Pageable stablePageable = pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));
        Page<Caution> page = cautionRepository.findAll(
            CautionSpecification.parCriteres(criteres),
            stablePageable
        );

        if (page.isEmpty()) {
            return Page.empty(pageable);
        }

        // Mapping vers DTOs — les associations sont déjà chargées par l'EntityGraph
        return page.map(cautionMapper::toDto);
    }

    // ── Export PDF ────────────────────────────────────────────────────────────

    @Override
    public byte[] exporterPdf(CautionSearchCriteria criteres) {
        log.debug("Export PDF cautions — critères: {}", criteres);

        List<Caution> cautions = cautionRepository.findAll(
            CautionSpecification.parCriteres(criteres),
            Sort.by(Sort.Direction.DESC, "dateDePrise")
        );

        BigDecimal totalMontant = cautions.stream()
            .map(Caution::getMontant)
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> filterLines = buildFilterSummary(criteres);

        try {
            return staportPdfReportService.generateReport(
                "État des Cautions",
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

                        Paragraph title = new Paragraph("État des Cautions", titleFont);
                        title.setAlignment(Element.ALIGN_CENTER);
                        doc.add(title);

                        String exportInfo = "Exporté le " + LocalDate.now().format(DATE_FMT)
                                + "  •  " + cautions.size() + " caution(s)";
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

                        float[] colWidths = {8f, 11f, 10f, 9f, 14f, 15f, 8f, 8f, 9f, 6f, 7f};
                        PdfPTable table = new PdfPTable(colWidths);
                        table.setWidthPercentage(100f);
                        table.setSpacingBefore(8f);
                        table.setHeaderRows(1);

                        for (String h : new String[]{
                                "N° Caution", "Banque", "Nature", "N° Marché", "Organisme", "Désignation",
                                "Date de prise", "Main levée", "Montant (MAD)", "Taux (%)", "Statut"}) {
                            PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                            c.setBackgroundColor(medBlue);
                            c.setPadding(5f);
                            c.setHorizontalAlignment(Element.ALIGN_CENTER);
                            c.setBorderColor(Color.WHITE);
                            c.setBorderWidth(0.5f);
                            table.addCell(c);
                        }

                        for (int i = 0; i < cautions.size(); i++) {
                            Caution c = cautions.get(i);
                            Color rowBg = i % 2 == 0 ? Color.WHITE : rowAlt;

                            addPdfCell(table, c.getNumeroCaution(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                            addPdfCell(table, c.getBanque() != null ? c.getBanque().getNom() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                            addPdfCell(table, c.getNature() != null ? c.getNature().getLabel() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                            addPdfCell(table, c.getMarche() != null ? c.getMarche().getNumero() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                            addPdfCell(table, c.getOrganisme(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                            addPdfCell(table, c.getDesignation(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                            addPdfCell(table, c.getDateDePrise() != null ? c.getDateDePrise().format(DATE_FMT) : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);
                            addPdfCell(table, c.getDateDeMainLevee() != null ? c.getDateDeMainLevee().format(DATE_FMT) : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);
                            addPdfCell(table, formatMontantPdf(c.getMontant()), cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);
                            addPdfCell(table, c.getTaux() != null ? c.getTaux().toPlainString() : "—", cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);

                            boolean actif = Boolean.TRUE.equals(c.getActif());
                            PdfPCell statusCell = new PdfPCell(new Phrase(
                                    actif ? "ACTIVE" : "INACTIVE",
                                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
                            statusCell.setBackgroundColor(actif ? green : orange);
                            statusCell.setPadding(4f);
                            statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                            statusCell.setBorderColor(borderGray);
                            statusCell.setBorderWidth(0.5f);
                            table.addCell(statusCell);
                        }

                        // Ligne total
                        PdfPCell totalLabel = new PdfPCell(
                                new Phrase("TOTAL — " + cautions.size() + " caution(s)", totalFont));
                        totalLabel.setColspan(8);
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

                        for (int j = 0; j < 2; j++) {
                            PdfPCell empty = new PdfPCell(new Phrase(""));
                            empty.setBackgroundColor(lightBlue);
                            empty.setBorderColor(medBlue);
                            table.addCell(empty);
                        }

                        doc.add(table);

                    } catch (DocumentException e) {
                        throw new RuntimeException("Erreur génération contenu PDF cautions", e);
                    }
                }
            );
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des cautions", e);
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

    private List<String> buildFilterSummary(CautionSearchCriteria criteres) {
        List<String> lines = new ArrayList<>();
        if (criteres == null) return lines;

        if (criteres.numeroCaution() != null && !criteres.numeroCaution().isBlank())
            lines.add("N° Caution : " + criteres.numeroCaution());
        if (criteres.organisme() != null && !criteres.organisme().isBlank())
            lines.add("Organisme : " + criteres.organisme());
        if (criteres.banqueId() != null) {
            try {
                lines.add("Banque : " + banqueService.findOrThrow(criteres.banqueId()).getNom());
            } catch (ResourceNotFoundException ignored) { /* filtre obsolète — ignoré pour l'affichage */ }
        }
        if (criteres.natureId() != null) {
            try {
                lines.add("Nature : " + natureService.findOrThrow(criteres.natureId()).getLabel());
            } catch (ResourceNotFoundException ignored) { /* filtre obsolète — ignoré pour l'affichage */ }
        }
        if (criteres.marcheId() != null) {
            try {
                lines.add("Marché : " + marcheService.findOrThrow(criteres.marcheId()).getNumero());
            } catch (ResourceNotFoundException ignored) { /* filtre obsolète — ignoré pour l'affichage */ }
        }
        if (criteres.dateDePrise() != null)
            lines.add("Date de prise : " + criteres.dateDePrise().format(DATE_FMT));
        if (criteres.montant() != null)
            lines.add("Montant : " + formatMontantPdf(criteres.montant()) + " MAD");
        if (criteres.actif() != null)
            lines.add("Statut : " + (criteres.actif() ? "Active" : "Inactive"));

        return lines;
    }

    // ── Suppression (hard delete) ─────────────────────────────────────────────

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.DELETE, entity = Caution.class)
    public void supprimer(Long id) {
        log.info("Suppression caution — id: {}", id);

        Caution caution = trouverOuLeverException(id);

        // Règle métier : la suppression est interdite lorsque la caution est archivée
        if (Boolean.FALSE.equals(caution.getActif())) {
            throw new CautionBusinessException(
                "La suppression de la caution id: " + id + " est interdite — la caution est archivée."
            );
        }

        cautionRepository.delete(caution);
        log.info("Caution id: {} supprimée définitivement", id);
    }

    // ── Clôture (main levée) ──────────────────────────────────────────────────

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Caution.class)
    public CloturerCautionResponse cloturer(Long id, CloturerCautionRequest request) {
        log.info("Clôture caution — id: {}, dateDeMainLevee: {}", id, request.dateDeMainLevee());

        // 1. Chargement avec associations (banque + nature nécessaires pour la stratégie)
        Caution caution = cautionRepository.findByIdAvecAssociations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caution introuvable — id: " + id));

        // 2. Validations métier
        if (Boolean.FALSE.equals(caution.getActif())) {
            throw new CautionBusinessException("La caution id: " + id + " est déjà clôturée.");
        }

        // 3. Résolution de la stratégie
        String nomBanque = caution.getBanque().getNom();
        CommissionCautionStrategie strategie = strategieFactory.trouver(nomBanque)
                .orElseThrow(() -> new CautionBusinessException(
                        "Aucune stratégie de commission enregistrée pour la banque '" + nomBanque + "'."));

        // 4. Suppression du prorata d'entrée si même mois que la clôture ET encore PREVU.
        //    Cas : caution ouverte et clôturée dans le même mois → le prorata d'entrée
        //    (généré à la création, dateEmission=dateDePrise) couvrirait les mêmes jours
        //    que la commission de clôture. On le supprime pour éviter la double facturation.
        //    Si le prorata est déjà PAYE, il reste intact (il a déjà été réglé).
        if (strategie.supporteProrataEntree()
                && caution.getDateDePrise() != null
                && YearMonth.from(caution.getDateDePrise())
                            .equals(YearMonth.from(request.dateDeMainLevee()))) {

            commissionService.findPrevuByCautionAndDateEmission(caution.getId(), caution.getDateDePrise())
                .ifPresent(prorata -> {
                    // deleteCommission supprime aussi, en cascade, la TVA liée si elle existe
                    // (cas BMCI qui génère une TVA sur le prorata d'entrée).
                    commissionService.deleteCommission(prorata.getId());
                    log.info("Prorata d'entrée PREVU supprimé (même mois clôture) — "
                            + "caution id: {}, banque: {}, dateEmission prorata: {}",
                            caution.getId(), nomBanque, caution.getDateDePrise());
                });
        }

        // 5. Calcul et persistance de la commission de clôture
        // Objet transitoire — ses champs portent le calcul métier de la stratégie ; il n'est
        // jamais persisté directement (encapsulation du module commission).
        Commission commissionCalculee = strategie.calculerMainLevee(caution, request.dateDeMainLevee());
        CommissionResponseDto commissionPersistee = commissionService.creerCommissionCaution(
                caution.getId(), caution.getBanque().getId(), commissionCalculee.getMontant(),
                commissionCalculee.getDateEmission(), commissionCalculee.getLibelle(),
                commissionCalculee.getObservation());
        log.info("Commission de clôture générée — id: {}, montant: {}",
                commissionPersistee.getId(), commissionPersistee.getMontant());

        // 5b. TVA si la stratégie l'exige (ex. BMCI)
        if (strategie.genererTva()) {
            BigDecimal montantTva = commissionPersistee.getMontant()
                    .multiply(TAUX_TVA)
                    .setScale(2, RoundingMode.HALF_UP);

            impotEtTaxeService.creerTvaSurCommission(
                    caution.getBanque().getId(), montantTva, Motif.TVA_COMMISSION_CAUTION,
                    commissionPersistee.getDateEmission(), "TVA " + commissionPersistee.getLibelle(),
                    "TVA/commission/caution/cloture", commissionPersistee.getId(), caution.getId());
            log.info("TVA de clôture générée — montant: {}", montantTva);
        }

        // 6. Clôture de la caution
        caution.setDateDeMainLevee(request.dateDeMainLevee());
        caution.setActif(false);
        cautionRepository.save(caution);
        log.info("Caution id: {} clôturée avec succès", id);

        return new CloturerCautionResponse(
                cautionMapper.toDto(caution),
                commissionPersistee
        );
    }

    // ── Helpers internes ──────────────────────────────────────────────────────

    @Override
    public Caution trouverOuLeverException(Long id) {
        return cautionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Caution introuvable — id: " + id
            ));
    }

    /**
     * Rechargement avec EntityGraph pour construire le DTO de réponse complet.
     * Utilisé en fin de chaque opération d'écriture pour retourner un état cohérent.
     */
    private CautionResponseDto toDto(Long id) {
        Caution caution = cautionRepository.findByIdAvecAssociations(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Caution introuvable après persistance — id: " + id
            ));
        return cautionMapper.toDto(caution);
    }
}
