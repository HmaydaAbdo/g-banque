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
import staport.finance.gBanque.commision.enums.TypeCommission;
import staport.finance.gBanque.commision.service.ICommissionService;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.impot_taxes.enums.Motif;
import staport.finance.gBanque.impot_taxes.service.IImpotEtTaxeService;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.service.IMarcheService;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.reports.service.StaportPdfReportService;
import staport.finance.gBanque.reports.service.StaportExcelReportService;
import staport.finance.gBanque.reports.utils.MontantFormatter;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final StaportExcelReportService          staportExcelReportService;

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
            StaportPdfReportService staportPdfReportService,
            StaportExcelReportService staportExcelReportService) {
        this.cautionRepository    = cautionRepository;
        this.cautionMapper        = cautionMapper;
        this.banqueService        = banqueService;
        this.natureService        = natureService;
        this.marcheService        = marcheService;
        this.commissionService    = commissionService;
        this.impotEtTaxeService   = impotEtTaxeService;
        this.strategieFactory     = strategieFactory;
        this.staportPdfReportService = staportPdfReportService;
        this.staportExcelReportService = staportExcelReportService;
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

        // 5. Le prorata d'entrée n'est plus créé ici : il est généré par le batch de commissions en fin de période
        //    (mois ou trimestre de prise), avec dateEmission = dateDePrise.

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
        boolean etaitCloturee = Boolean.FALSE.equals(caution.getActif());

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

        // Règle métier : une caution déjà clôturée reste clôturée — modifier() ne permet pas
        // de la rouvrir, seulement de corriger ses champs (dont la date de main levée). Toute
        // correction impactant le calcul de la commission de clôture (banque, montant, taux,
        // dates) la recalcule automatiquement — voir docs/modules/README.md, chapitre Caution.
        if (etaitCloturee) {
            if (caution.getDateDeMainLevee() == null) {
                throw new CautionBusinessException(
                    "La date de main levée est obligatoire pour une caution déjà clôturée."
                );
            }
            recalculerCommissionCloture(caution);
        }

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

    /** Ordre canonique des colonnes exportables — fixe l'ordre d'affichage dans le PDF. */
    private static final List<String> CAUTION_EXPORT_COLUMNS = List.of(
            "numeroCaution", "banque", "nature", "marche", "organisme", "designation",
            "dateDePrise", "dateDeMainLevee", "montant", "taux", "statut"
    );

    private static final Map<String, String> CAUTION_COLUMN_LABELS = Map.ofEntries(
            Map.entry("numeroCaution", "N° Caution"),
            Map.entry("banque", "Banque"),
            Map.entry("nature", "Nature"),
            Map.entry("marche", "N° Marché"),
            Map.entry("organisme", "Organisme"),
            Map.entry("designation", "Désignation"),
            Map.entry("dateDePrise", "Date de prise"),
            Map.entry("dateDeMainLevee", "Main levée"),
            Map.entry("montant", "Montant (MAD)"),
            Map.entry("taux", "Taux (%)"),
            Map.entry("statut", "Statut")
    );

    private static final Map<String, Float> CAUTION_COLUMN_WIDTHS = Map.ofEntries(
            Map.entry("numeroCaution", 8f),
            Map.entry("banque", 11f),
            Map.entry("nature", 10f),
            Map.entry("marche", 9f),
            Map.entry("organisme", 14f),
            Map.entry("designation", 15f),
            Map.entry("dateDePrise", 8f),
            Map.entry("dateDeMainLevee", 8f),
            Map.entry("montant", 9f),
            Map.entry("taux", 6f),
            Map.entry("statut", 7f)
    );

    /**
     * Résout la liste des colonnes à exporter : filtre {@link #CAUTION_EXPORT_COLUMNS} pour ne
     * garder que celles demandées (en préservant l'ordre canonique), en ignorant toute clé
     * inconnue. {@code null}, vide, ou ne contenant aucune clé valide ⇒ toutes les colonnes
     * (comportement par défaut, rétrocompatible avec les appels sans sélection).
     */
    private List<String> resolveCautionColonnes(List<String> demandees) {
        if (demandees == null || demandees.isEmpty()) {
            return CAUTION_EXPORT_COLUMNS;
        }
        List<String> filtrees = CAUTION_EXPORT_COLUMNS.stream()
                .filter(demandees::contains)
                .collect(Collectors.toList());
        return filtrees.isEmpty() ? CAUTION_EXPORT_COLUMNS : filtrees;
    }

    private String cautionColumnValue(String colonne, Caution c) {
        return switch (colonne) {
            case "numeroCaution" -> c.getNumeroCaution();
            case "banque" -> c.getBanque() != null ? c.getBanque().getNom() : "—";
            case "nature" -> c.getNature() != null ? c.getNature().getLabel() : "—";
            case "marche" -> c.getMarche() != null ? c.getMarche().getNumero() : "—";
            case "organisme" -> c.getOrganisme();
            case "designation" -> c.getDesignation();
            case "dateDePrise" -> c.getDateDePrise() != null ? c.getDateDePrise().format(DATE_FMT) : "—";
            case "dateDeMainLevee" -> c.getDateDeMainLevee() != null ? c.getDateDeMainLevee().format(DATE_FMT) : "—";
            case "montant" -> MontantFormatter.format(c.getMontant());
            case "taux" -> c.getTaux() != null ? c.getTaux().toPlainString() : "—";
            default -> "—";
        };
    }

    private int cautionColumnAlign(String colonne) {
        return switch (colonne) {
            case "dateDePrise", "dateDeMainLevee" -> Element.ALIGN_CENTER;
            case "montant", "taux" -> Element.ALIGN_RIGHT;
            default -> Element.ALIGN_LEFT;
        };
    }

    @Override
    public byte[] exporterPdf(CautionSearchCriteria criteres, List<String> colonnesSelectionnees) {
        log.debug("Export PDF cautions — critères: {}, colonnes: {}", criteres, colonnesSelectionnees);

        List<Caution> cautions = cautionRepository.findAll(
            CautionSpecification.parCriteres(criteres),
            Sort.by(Sort.Direction.DESC, "dateDePrise")
        );

        BigDecimal totalMontant = cautions.stream()
            .map(Caution::getMontant)
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> filterLines = buildFilterSummary(criteres);
        List<String> colonnes = resolveCautionColonnes(colonnesSelectionnees);
        boolean montantInclus = colonnes.contains("montant");

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

                        float[] colWidths = new float[colonnes.size()];
                        for (int i = 0; i < colonnes.size(); i++) {
                            colWidths[i] = CAUTION_COLUMN_WIDTHS.get(colonnes.get(i));
                        }
                        PdfPTable table = new PdfPTable(colWidths);
                        table.setWidthPercentage(100f);
                        table.setSpacingBefore(8f);
                        table.setHeaderRows(1);

                        for (String colonne : colonnes) {
                            PdfPCell c = new PdfPCell(new Phrase(CAUTION_COLUMN_LABELS.get(colonne), hdrFont));
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

                            for (String colonne : colonnes) {
                                if (colonne.equals("statut")) {
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
                                } else {
                                    addPdfCell(table, cautionColumnValue(colonne, c), cellFont, rowBg, borderGray,
                                            cautionColumnAlign(colonne));
                                }
                            }
                        }

                        // Ligne total — uniquement si la colonne Montant est incluse dans l'export
                        if (montantInclus) {
                            int montantIndex = colonnes.indexOf("montant");
                            String totalLabelText = "TOTAL — " + cautions.size() + " caution(s)";

                            if (montantIndex > 0) {
                                PdfPCell totalLabel = new PdfPCell(new Phrase(totalLabelText, totalFont));
                                totalLabel.setColspan(montantIndex);
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
                            } else {
                                PdfPCell totalVal = new PdfPCell(new Phrase(
                                        totalLabelText + " : " + MontantFormatter.format(totalMontant) + " MAD", totalFont));
                                totalVal.setBackgroundColor(lightBlue);
                                totalVal.setPadding(5f);
                                totalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                                totalVal.setBorderColor(medBlue);
                                table.addCell(totalVal);
                            }

                            int remaining = colonnes.size() - montantIndex - 1;
                            for (int j = 0; j < remaining; j++) {
                                PdfPCell empty = new PdfPCell(new Phrase(""));
                                empty.setBackgroundColor(lightBlue);
                                empty.setBorderColor(medBlue);
                                table.addCell(empty);
                            }
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

    // ── Export Excel ────────────────────────────────────────────────────────

    /**
     * Génère l'export Excel des cautions — même sélection de données et de colonnes que
     * {@link #exporterPdf} ({@link #CAUTION_EXPORT_COLUMNS}, {@link #resolveCautionColonnes}),
     * seule la mise en forme (POI/{@link StaportExcelReportService}) diffère.
     */
    @Override
    public byte[] exporterExcel(CautionSearchCriteria criteres, List<String> colonnesSelectionnees) {
        log.debug("Export Excel cautions — critères: {}, colonnes: {}", criteres, colonnesSelectionnees);

        List<Caution> cautions = cautionRepository.findAll(
            CautionSpecification.parCriteres(criteres),
            Sort.by(Sort.Direction.DESC, "dateDePrise")
        );

        BigDecimal totalMontant = cautions.stream()
            .map(Caution::getMontant)
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> filterLines = buildFilterSummary(criteres);
        List<String> colonnes = resolveCautionColonnes(colonnesSelectionnees);
        boolean montantInclus = colonnes.contains("montant");

        return staportExcelReportService.generateReport("Cautions", (workbook, sheet) -> {
            short montantFormat = workbook.createDataFormat().getFormat(MontantFormatter.EXCEL_FORMAT);
            int lastCol = colonnes.size() - 1;

            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            org.apache.poi.ss.usermodel.Font subFont = workbook.createFont();
            subFont.setFontHeightInPoints((short) 9);
            subFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            CellStyle subStyle = workbook.createCellStyle();
            subStyle.setFont(subFont);
            subStyle.setAlignment(HorizontalAlignment.CENTER);

            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle textStyle = workbook.createCellStyle();

            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setDataFormat(montantFormat);
            numberStyle.setAlignment(HorizontalAlignment.RIGHT);

            org.apache.poi.ss.usermodel.Font statutFont = workbook.createFont();
            statutFont.setBold(true);
            statutFont.setColor(IndexedColors.WHITE.getIndex());
            CellStyle statutActifStyle = workbook.createCellStyle();
            statutActifStyle.setFont(statutFont);
            statutActifStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            statutActifStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            statutActifStyle.setAlignment(HorizontalAlignment.CENTER);
            CellStyle statutInactifStyle = workbook.createCellStyle();
            statutInactifStyle.setFont(statutFont);
            statutInactifStyle.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
            statutInactifStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            statutInactifStyle.setAlignment(HorizontalAlignment.CENTER);

            org.apache.poi.ss.usermodel.Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            CellStyle totalLabelStyle = workbook.createCellStyle();
            totalLabelStyle.setFont(totalFont);
            totalLabelStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            totalLabelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalLabelStyle.setAlignment(HorizontalAlignment.RIGHT);
            CellStyle totalValueStyle = workbook.createCellStyle();
            totalValueStyle.setFont(totalFont);
            totalValueStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            totalValueStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalValueStyle.setAlignment(HorizontalAlignment.RIGHT);
            totalValueStyle.setDataFormat(montantFormat);

            int rowIdx = 0;

            Row titleRow = sheet.createRow(rowIdx++);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("État des Cautions");
            titleCell.setCellStyle(titleStyle);
            if (lastCol > 0) sheet.addMergedRegion(new CellRangeAddress(titleRow.getRowNum(), titleRow.getRowNum(), 0, lastCol));

            String exportInfo = "Exporté le " + LocalDate.now().format(DATE_FMT)
                    + "  •  " + cautions.size() + " caution(s)";
            if (!filterLines.isEmpty()) {
                exportInfo += "  •  Filtres : " + String.join("  |  ", filterLines);
            }
            Row subRow = sheet.createRow(rowIdx++);
            org.apache.poi.ss.usermodel.Cell subCell = subRow.createCell(0);
            subCell.setCellValue(exportInfo);
            subCell.setCellStyle(subStyle);
            if (lastCol > 0) sheet.addMergedRegion(new CellRangeAddress(subRow.getRowNum(), subRow.getRowNum(), 0, lastCol));

            rowIdx++; // ligne vide de séparation

            Row headerRow = sheet.createRow(rowIdx++);
            for (int i = 0; i < colonnes.size(); i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(CAUTION_COLUMN_LABELS.get(colonnes.get(i)));
                cell.setCellStyle(headerStyle);
            }

            for (Caution c : cautions) {
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < colonnes.size(); i++) {
                    String colonne = colonnes.get(i);
                    org.apache.poi.ss.usermodel.Cell cell = row.createCell(i);
                    switch (colonne) {
                        case "montant" -> {
                            cell.setCellValue(c.getMontant() != null ? c.getMontant().doubleValue() : 0d);
                            cell.setCellStyle(numberStyle);
                        }
                        case "taux" -> {
                            cell.setCellValue(c.getTaux() != null ? c.getTaux().doubleValue() : 0d);
                            cell.setCellStyle(numberStyle);
                        }
                        case "statut" -> {
                            boolean actif = Boolean.TRUE.equals(c.getActif());
                            cell.setCellValue(actif ? "ACTIVE" : "INACTIVE");
                            cell.setCellStyle(actif ? statutActifStyle : statutInactifStyle);
                        }
                        default -> {
                            cell.setCellValue(cautionColumnValue(colonne, c));
                            cell.setCellStyle(textStyle);
                        }
                    }
                }
            }

            if (montantInclus) {
                int montantIndex = colonnes.indexOf("montant");
                Row totalRow = sheet.createRow(rowIdx);
                String totalLabelText = "TOTAL — " + cautions.size() + " caution(s)";

                for (int i = 0; i < colonnes.size(); i++) {
                    org.apache.poi.ss.usermodel.Cell cell = totalRow.createCell(i);
                    cell.setCellStyle(i == montantIndex ? totalValueStyle : totalLabelStyle);
                }

                if (montantIndex > 0) {
                    totalRow.getCell(0).setCellValue(totalLabelText);
                    if (montantIndex > 1) {
                        sheet.addMergedRegion(new CellRangeAddress(totalRow.getRowNum(), totalRow.getRowNum(), 0, montantIndex - 1));
                    }
                    totalRow.getCell(montantIndex).setCellValue(totalMontant.doubleValue());
                } else {
                    totalRow.getCell(0).setCellValue(totalLabelText + " : " + MontantFormatter.format(totalMontant) + " MAD");
                }
            }

            for (int i = 0; i < colonnes.size(); i++) {
                sheet.autoSizeColumn(i);
            }
        });
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
        if (criteres.dateDePriseDebut() != null)
            lines.add("Date de prise min : " + criteres.dateDePriseDebut().format(DATE_FMT));
        if (criteres.dateDePriseFin() != null)
            lines.add("Date de prise max : " + criteres.dateDePriseFin().format(DATE_FMT));
        if (criteres.dateDeMainLeveeDebut() != null)
            lines.add("Date main levée min : " + criteres.dateDeMainLeveeDebut().format(DATE_FMT));
        if (criteres.dateDeMainLeveeFin() != null)
            lines.add("Date main levée max : " + criteres.dateDeMainLeveeFin().format(DATE_FMT));
        if (criteres.montant() != null)
            lines.add("Montant : " + MontantFormatter.format(criteres.montant()) + " MAD");
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
                    commissionService.deleteCommission(prorata.id());
                    log.info("Prorata d'entrée PREVU supprimé (même mois clôture) — "
                            + "caution id: {}, banque: {}, dateEmission prorata: {}",
                            caution.getId(), nomBanque, caution.getDateDePrise());
                });
        }

        // 5. Calcul et persistance de la commission de clôture (+ TVA éventuelle)
        CommissionResponseDto commissionPersistee = genererCommissionCloture(caution, request.dateDeMainLevee());

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

    /**
     * Calcule et persiste la commission de clôture (+ TVA si la stratégie de la banque
     * l'exige) pour une caution — factorisé entre {@link #cloturer} (première clôture) et
     * {@link #recalculerCommissionCloture} (correction d'une caution déjà clôturée). Ne
     * touche ni au prorata d'entrée ni au statut/à la date de main levée de la caution —
     * ces deux aspects restent propres à chaque appelant.
     */
    private CommissionResponseDto genererCommissionCloture(Caution caution, LocalDate dateDeMainLevee) {
        String nomBanque = caution.getBanque().getNom();
        CommissionCautionStrategie strategie = strategieFactory.trouver(nomBanque)
                .orElseThrow(() -> new CautionBusinessException(
                        "Aucune stratégie de commission enregistrée pour la banque '" + nomBanque + "'."));

        // Objet transitoire — ses champs portent le calcul métier de la stratégie ; il n'est
        // jamais persisté directement (encapsulation du module commission).
        Commission commissionCalculee = strategie.calculerMainLevee(caution, dateDeMainLevee);
        CommissionResponseDto commissionPersistee = commissionService.creerCommissionCaution(
                caution.getId(), caution.getBanque().getId(), commissionCalculee.getMontant(),
                commissionCalculee.getDateEmission(), commissionCalculee.getLibelle(),
                commissionCalculee.getObservation(), TypeCommission.CLOTURE);
        log.info("Commission de clôture générée — id: {}, montant: {}",
                commissionPersistee.id(), commissionPersistee.montant());

        if (strategie.genererTva()) {
            BigDecimal montantTva = commissionPersistee.montant()
                    .multiply(TAUX_TVA)
                    .setScale(2, RoundingMode.HALF_UP);

            impotEtTaxeService.creerTvaSurCommission(
                    caution.getBanque().getId(), montantTva, Motif.TVA_COMMISSION_CAUTION,
                    commissionPersistee.dateEmission(), "TVA " + commissionPersistee.libelle(),
                    "TVA/commission/caution/cloture", commissionPersistee.id(), caution.getId());
            log.info("TVA de clôture générée — montant: {}", montantTva);
        }

        return commissionPersistee;
    }

    /**
     * Recalcule la commission de clôture d'une caution déjà clôturée dont un champ impactant
     * ce calcul (banque, montant, taux, dates) vient d'être corrigé via {@link #modifier}.
     *
     * <p>Retrouve la commission {@code CLOTURE} de la caution ({@code TypeCommission}) — pas
     * par libellé/date d'émission, qui peuvent coïncider avec une commission mensuelle du même
     * mois pour les banques sans prorata de sortie (BMCI, CDM, SAHAM BANK). Bloque plutôt que
     * de deviner si :
     * <ul>
     *   <li>aucune commission {@code CLOTURE} n'est trouvée — caution clôturée avant
     *       l'introduction de ce champ ;</li>
     *   <li>plusieurs sont trouvées — anomalie de données ;</li>
     *   <li>elle est déjà réglée ({@code PAYE}) — {@code conventions/architecture.md}, règle 5 :
     *       une opération déjà réglée n'est jamais supprimée/recalculée silencieusement.</li>
     * </ul>
     * Dans ces trois cas, une correction manuelle par un administrateur est requise.
     */
    private void recalculerCommissionCloture(Caution caution) {
        List<CommissionResponseDto> existantes =
                commissionService.findByCautionAndType(caution.getId(), TypeCommission.CLOTURE);

        if (existantes.isEmpty()) {
            throw new CautionBusinessException(
                "Impossible de recalculer la commission de clôture de la caution id: " + caution.getId()
                + " — aucune commission de clôture identifiable (caution clôturée avant l'introduction de "
                + "cette fonctionnalité). Contacter un administrateur pour une correction manuelle."
            );
        }
        if (existantes.size() > 1) {
            throw new CautionBusinessException(
                "Anomalie : " + existantes.size() + " commissions de clôture trouvées pour la caution id: "
                + caution.getId() + " — correction manuelle requise."
            );
        }

        CommissionResponseDto existante = existantes.get(0);
        if (existante.status() != Status.PREVU) {
            throw new CautionBusinessException(
                "La commission de clôture de la caution id: " + caution.getId()
                + " est déjà réglée (PAYE) — modification de la caution impossible."
            );
        }

        // deleteCommission supprime aussi, en cascade, la TVA liée si elle existe.
        commissionService.deleteCommission(existante.id());
        genererCommissionCloture(caution, caution.getDateDeMainLevee());
        log.info("Commission de clôture recalculée — caution id: {}", caution.getId());
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
