package staport.finance.gBanque.recapitulatif.services;

import com.lowagie.text.Chunk;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.dtos.response.BanqueResponse;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.recapitulatif.dtos.request.RecapitulatifSearchDto;
import staport.finance.gBanque.recapitulatif.dtos.response.*;
import staport.finance.gBanque.recapitulatif.enums.PeriodeRecap;
import staport.finance.gBanque.recapitulatif.entities.RecapitulatifEncoursView;
import staport.finance.gBanque.recapitulatif.mappers.RecapitulatifEncoursMapper;
import staport.finance.gBanque.recapitulatif.repositories.RecapitulatifEncoursRepository;
import staport.finance.gBanque.recapitulatif.services.interfaces.IRecapitulatifService;
import staport.finance.gBanque.recapitulatif.specs.RecapitulatifEncoursSpecification;
import staport.finance.gBanque.reports.service.StaportPdfReportService;
import staport.finance.gBanque.transactions.enums.SensOperation;
import staport.finance.gBanque.reports.utils.MontantFormatter;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service pour générer un récapitulatif consolidé de tous les décaissements/encaissements
 * prévisionnels (échéances leasing/CMT, moyens de paiement, agios, commissions, etc.) jusqu'à
 * l'horizon choisi : fin du mois en cours par défaut, ou fin du trimestre (voir {@link PeriodeRecap}).
 *
 * <p>Interroge {@link RecapitulatifEncoursRepository}, adossé à la vue SQL
 * {@code v_recapitulatif_encours} — pagination, tri et agrégation réels en base, remplaçant
 * l'ancienne implémentation qui chargeait ~17 repositories en mémoire (voir historique dans
 * {@code modules_sweep.md}, sweep États des encours / Récapitulatif, Phase 2, 17/07/2026).</p>
 *
 * <p>Depuis le 06/09/2026, les deux écrans Récapitulatif Décaissements et Récapitulatif
 * Encaissements sont fusionnés en un seul : le sens est devenu un critère de recherche optionnel
 * au lieu d'être fixé par la route, et les deux totaux sont retournés ensemble.</p>
 */
@Service
@Transactional(readOnly = true)
public class RecapitulatifServiceImpl implements IRecapitulatifService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color MED_BLUE = Color.decode("#2563eb");
    private static final Color LIGHT_BLUE = Color.decode("#dbeafe");
    private static final Color ROW_ALT = Color.decode("#f8fafc");
    private static final Color BORDER_GRAY = Color.decode("#e5e7eb");

    private final RecapitulatifEncoursRepository encoursRepository;
    private final RecapitulatifEncoursMapper mapper;
    private final IBanqueService banqueService;
    private final StaportPdfReportService staportPdfReportService;

    public RecapitulatifServiceImpl(
            RecapitulatifEncoursRepository encoursRepository,
            RecapitulatifEncoursMapper mapper,
            IBanqueService banqueService,
            StaportPdfReportService staportPdfReportService) {
        this.encoursRepository = encoursRepository;
        this.mapper = mapper;
        this.banqueService = banqueService;
        this.staportPdfReportService = staportPdfReportService;
    }

    /**
     * Borne de l'horizon, recalculée à chaque appel à partir de la date du jour — ce service est un
     * singleton Spring, une borne figée à la construction du bean resterait bloquée sur la date de
     * démarrage du backend (bug identifié le 28/07/2026). {@code null} vaut fin de mois.
     */
    private LocalDate horizon(PeriodeRecap periode) {
        return (periode != null ? periode : PeriodeRecap.FIN_DE_MOIS).horizon(LocalDate.now());
    }

    @Override
    public AgregatedRecapDto getRecapitulatifForCurrentMonthForGivenBanque(
            Pageable pageable, RecapitulatifSearchDto searchDto, Long banqueId) {

        Specification<RecapitulatifEncoursView> spec = buildSearchSpecification(searchDto, banqueId);

        Page<RecaputilatifItem> page = encoursRepository.findAll(spec, normalizeSort(pageable))
                .map(mapper::toDto);

        Map<Type, BigDecimal> totaux = encoursRepository.sumMontantParSens(spec);

        return new AgregatedRecapDto(
                totaux.getOrDefault(Type.DECAISSEMENT, BigDecimal.ZERO),
                totaux.getOrDefault(Type.ENCAISSEMENT, BigDecimal.ZERO),
                page);
    }

    /**
     * Construit la Specification unique de l'écran à partir des critères saisis. La même
     * instance sert à la fois à la page retournée et aux totaux : c'est ce qui garantit que les
     * totaux affichés correspondent exactement aux lignes filtrées, et que le total du sens exclu
     * vaut zéro dès qu'un sens précis est demandé (le regroupement par sens ne renvoie alors
     * qu'une seule clé).
     */
    private Specification<RecapitulatifEncoursView> buildSearchSpecification(
            RecapitulatifSearchDto searchDto, Long banqueId) {

        return RecapitulatifEncoursSpecification.searchByCriteria(
                searchDto, banqueId, toLegacySens(searchDto),
                resolveDateReferenceDepuis(searchDto), resolveDateReferenceUntil(searchDto));
    }

    /**
     * Borne basse optionnelle — contrairement à {@link #resolveDateReferenceUntil}, aucune valeur
     * par défaut n'est appliquée quand elle est absente : {@code null} signifie simplement
     * « aucune borne basse », pas un repli sur une date calculée.
     */
    private LocalDate resolveDateReferenceDepuis(RecapitulatifSearchDto searchDto) {
        return (searchDto != null) ? searchDto.dateReferenceDepuis() : null;
    }

    /**
     * Convertit le sens demandé ({@link SensOperation}, type public du projet) vers l'enum legacy
     * {@link Type} porté par la vue SQL. Les deux enums déclarent les mêmes constantes, la
     * correspondance par nom suffit donc. {@code null} traverse tel quel : la Specification traite
     * alors le sens comme non filtré et retourne les deux sens.
     */
    private Type toLegacySens(RecapitulatifSearchDto searchDto) {
        SensOperation sens = (searchDto != null) ? searchDto.sens() : null;
        return sens == null ? null : Type.valueOf(sens.name());
    }

    /**
     * Les colonnes triables côté Angular (écran Récapitulatif) incluent "debit"/"credit"/
     * "dateEcheance", qui n'existent pas comme propriétés distinctes sur
     * {@link RecapitulatifEncoursView} : la vue ne porte qu'une seule colonne "montant" (sa
     * direction étant donnée par la colonne "sens"), et la date d'échéance y est exposée sous
     * "dateEcheanceAffichage". On les retraduit ici plutôt que de les exposer tel quel côté base
     * pour ne pas changer le contrat frontend.
     */
    private Pageable normalizeSort(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }
        Sort normalized = Sort.by(pageable.getSort().stream()
                .map(order -> new Sort.Order(order.getDirection(), mapSortProperty(order.getProperty())))
                .toList());
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), normalized);
    }

    private String mapSortProperty(String property) {
        return switch (property) {
            case "debit", "credit" -> "montant";
            case "dateEcheance" -> "dateEcheanceAffichage";
            default -> property;
        };
    }

    @Override
    public byte[] exportRecapitulatifPdf(RecapitulatifSearchDto searchDto, Long banqueId) {
        BanqueResponse banque = banqueService.getBanqueById(banqueId);

        Specification<RecapitulatifEncoursView> spec = buildSearchSpecification(searchDto, banqueId);

        List<RecaputilatifItem> items =
                encoursRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "dateReference")).stream()
                        .map(mapper::toDto)
                        .toList();

        Map<Type, BigDecimal> totaux = encoursRepository.sumMontantParSens(spec);
        BigDecimal totalDebit = totaux.getOrDefault(Type.DECAISSEMENT, BigDecimal.ZERO);
        BigDecimal totalCredit = totaux.getOrDefault(Type.ENCAISSEMENT, BigDecimal.ZERO);

        List<String> filterLines = new ArrayList<>();
        filterLines.add("Banque : " + banque.nom());
        if (searchDto != null) {
            if (searchDto.sens() != null) {
                filterLines.add("Sens : " + libelleSens(searchDto.sens()));
            }
            if (searchDto.libelleOuObservation() != null && !searchDto.libelleOuObservation().isBlank()) {
                filterLines.add("Libellé / Observation : " + searchDto.libelleOuObservation());
            }
            if (searchDto.numero() != null && !searchDto.numero().isBlank()) {
                filterLines.add("N° de pièce : " + searchDto.numero());
            }
            if (searchDto.transactionSources() != null && !searchDto.transactionSources().isEmpty()) {
                filterLines.add("Source : " + searchDto.transactionSources());
            }
            if (searchDto.dateReferenceDepuis() != null) {
                filterLines.add("Date de référence depuis : " + searchDto.dateReferenceDepuis().format(DATE_FMT));
            }
            if (searchDto.dateReferenceJusqua() != null) {
                filterLines.add("Date de référence jusqu'au : " + searchDto.dateReferenceJusqua().format(DATE_FMT));
            }
            if (searchDto.montant() != null) {
                filterLines.add("Montant : " + MontantFormatter.format(searchDto.montant()) + " MAD");
            }
            if (searchDto.commissionOperation() != null) {
                filterLines.add("Motif Commission : " + searchDto.commissionOperation());
            }
            if (searchDto.impotEtTaxeMotif() != null) {
                filterLines.add("Motif Impôt/Taxe : " + searchDto.impotEtTaxeMotif());
            }
        }
        if (searchDto == null || searchDto.dateReferenceJusqua() == null) {
            PeriodeRecap periode = periodeDe(searchDto);
            filterLines.add("Horizon : " + periode.libelle() + " (" + horizon(periode).format(DATE_FMT) + ")");
        }

        String title = titreRapport(searchDto) + " — " + banque.nom();

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
                            + "  •  " + items.size() + " ligne(s)";
                    if (!filterLines.isEmpty()) {
                        exportInfo += "  •  Filtres : " + String.join("  |  ", filterLines);
                    }
                    Paragraph sub = new Paragraph(exportInfo, subFont);
                    sub.setAlignment(Element.ALIGN_CENTER);
                    sub.setSpacingAfter(8f);
                    doc.add(sub);

                    float[] colWidths = {8f, 12f, 12f, 22f, 11f, 11f, 12f, 12f};
                    PdfPTable table = new PdfPTable(colWidths);
                    table.setWidthPercentage(100f);
                    table.setSpacingBefore(8f);
                    table.setHeaderRows(1);

                    for (String h : new String[]{
                            "ID", "Date émission", "Date échéance", "Libellé",
                            "Débit (MAD)", "Crédit (MAD)", "Source", "Observations"}) {
                        PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                        c.setBackgroundColor(MED_BLUE);
                        c.setPadding(5f);
                        c.setHorizontalAlignment(Element.ALIGN_CENTER);
                        c.setBorderColor(Color.WHITE);
                        c.setBorderWidth(0.5f);
                        table.addCell(c);
                    }

                    for (int i = 0; i < items.size(); i++) {
                        RecaputilatifItem it = items.get(i);
                        Color rowBg = i % 2 == 0 ? Color.WHITE : ROW_ALT;

                        addRecapPdfCell(table, String.valueOf(it.id()), cellFont, rowBg, Element.ALIGN_CENTER);
                        addRecapPdfCell(table, it.dateEmission() != null ? it.dateEmission().format(DATE_FMT) : "—", cellFont, rowBg, Element.ALIGN_CENTER);
                        addRecapPdfCell(table, it.dateEcheance() != null ? it.dateEcheance().format(DATE_FMT) : "—", cellFont, rowBg, Element.ALIGN_CENTER);
                        addRecapPdfCell(table, it.libelle(), cellFont, rowBg, Element.ALIGN_LEFT);
                        addRecapPdfCell(table, MontantFormatter.format(it.debit()), cellFont, rowBg, Element.ALIGN_RIGHT);
                        addRecapPdfCell(table, MontantFormatter.format(it.credit()), cellFont, rowBg, Element.ALIGN_RIGHT);
                        addRecapPdfCell(table, it.source() != null ? it.source().name() : "—", cellFont, rowBg, Element.ALIGN_CENTER);
                        addRecapPdfCell(table, it.observations(), cellFont, rowBg, Element.ALIGN_LEFT);
                    }

                    // Ligne de total : 4 (libellé) + 1 (débit) + 1 (crédit) + 2 (vide) = 8 cellules,
                    // soit exactement le nombre de colonnes du tableau. Les deux totaux sont posés
                    // sous leur colonne respective : l'écran pouvant désormais mélanger les deux
                    // sens, un total unique ne suffirait plus.
                    PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL PRÉVISIONNEL — " + items.size() + " ligne(s)", totalFont));
                    totalLabel.setColspan(4);
                    totalLabel.setBackgroundColor(LIGHT_BLUE);
                    totalLabel.setPadding(5f);
                    totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalLabel.setBorderColor(MED_BLUE);
                    table.addCell(totalLabel);

                    addRecapPdfTotalCell(table, MontantFormatter.format(totalDebit) + " MAD", totalFont);
                    addRecapPdfTotalCell(table, MontantFormatter.format(totalCredit) + " MAD", totalFont);

                    PdfPCell trailing = new PdfPCell(new Phrase(""));
                    trailing.setColspan(2);
                    trailing.setBackgroundColor(LIGHT_BLUE);
                    trailing.setPadding(5f);
                    trailing.setBorderColor(MED_BLUE);
                    table.addCell(trailing);

                    doc.add(table);
                } catch (DocumentException e) {
                    throw new RuntimeException("Erreur lors de la génération du contenu du PDF du récapitulatif.", e);
                }
            });
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF du récapitulatif.", e);
        }
    }

    /**
     * Intitulé du rapport, adapté au filtre de sens actif — aucun filtre (« Tous » côté écran)
     * produit un titre couvrant les deux sens.
     */
    private String titreRapport(RecapitulatifSearchDto searchDto) {
        SensOperation sens = (searchDto != null) ? searchDto.sens() : null;
        if (sens == SensOperation.DECAISSEMENT) {
            return "Récapitulatif des Décaissements Prévisionnels";
        }
        if (sens == SensOperation.ENCAISSEMENT) {
            return "Récapitulatif des Encaissements Prévisionnels";
        }
        return "Récapitulatif des Encaissements et Décaissements Prévisionnels";
    }

    private String libelleSens(SensOperation sens) {
        return sens == SensOperation.DECAISSEMENT ? "Décaissement" : "Encaissement";
    }

    /**
     * Détermine la borne supérieure ("jusqu'à") appliquée sur {@code dateReference} : la date
     * choisie par l'utilisateur si elle est renseignée dans les filtres de la source, sinon
     * l'horizon de l'écran (fin de mois par défaut, ou fin de trimestre).
     */
    private LocalDate resolveDateReferenceUntil(RecapitulatifSearchDto searchDto) {
        return (searchDto != null && searchDto.dateReferenceJusqua() != null)
                ? searchDto.dateReferenceJusqua()
                : horizon(periodeDe(searchDto));
    }

    private PeriodeRecap periodeDe(RecapitulatifSearchDto searchDto) {
        return (searchDto != null && searchDto.periode() != null) ? searchDto.periode() : PeriodeRecap.FIN_DE_MOIS;
    }

    private void addRecapPdfCell(PdfPTable table, String text, Font font, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", font));
        cell.setBackgroundColor(bg);
        cell.setPadding(4f);
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(BORDER_GRAY);
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
    }

    private void addRecapPdfTotalCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(LIGHT_BLUE);
        cell.setPadding(5f);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setBorderColor(MED_BLUE);
        table.addCell(cell);
    }

    @Override
    public RecaputilatifGlobal getRecapitulatifGlobal(Long banqueId, PeriodeRecap periode) {
        BanqueResponse banque = this.banqueService.getBanqueById(banqueId);
        // Plafond FC stocké en positif en base (colonne ligne_fonctionnelle.plafond) — conservé
        // tel quel ici (pas de negate()) pour rester cohérent avec RecapGeneralDetailleServiceImpl
        // (ResumeSectionDto.plafondLigne), qui affiche le même plafond en positif. Voir
        // RecaputilatifGlobal.disponibilitePrevue() pour le calcul de marge qui en découle
        // (retour utilisateur du 31/08/2026 : l'ancien negate() ici inversait le signe de cette
        // marge par rapport à la convention déjà utilisée dans l'autre rapport).
        BigDecimal plafondFc = banqueService.getPlafondFC(banqueId);

        LocalDate horizon = horizon(periode);
        List<RecaputilatifItem> decaissements = fetchOutstanding(banqueId, Type.DECAISSEMENT, horizon);
        List<RecaputilatifItem> encaissements = fetchOutstanding(banqueId, Type.ENCAISSEMENT, horizon);

        return new RecaputilatifGlobal(
                banque.id(),
                banque.nom(),
                banque.soldeActuel(),
                plafondFc,
                encaissements,
                decaissements
        );
    }

    private List<RecaputilatifItem> fetchOutstanding(Long banqueId, Type sens, LocalDate horizon) {
        Specification<RecapitulatifEncoursView> spec = Specification
                .where(RecapitulatifEncoursSpecification.byBanqueId(banqueId))
                .and(RecapitulatifEncoursSpecification.bySens(sens))
                .and(RecapitulatifEncoursSpecification.outstandingUntil(horizon));

        return encoursRepository.findAll(spec).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public List<RecaputilatifItem> getEncoursToutesBanques(PeriodeRecap periode) {
        Specification<RecapitulatifEncoursView> spec = RecapitulatifEncoursSpecification.outstandingUntil(horizon(periode));
        return encoursRepository.findAll(spec, Sort.by("banqueId", "dateReference")).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public List<RecaputilatifGlobal> getAllRecapitulatifsGlobaux(PeriodeRecap periode) {
        List<Long> allBankIds = this.banqueService.getAllBanqueIds();

        return allBankIds.stream()
                .map(id -> getRecapitulatifGlobal(id, periode))
                .collect(Collectors.toList());
    }

    @Override
    public EncoursPrevusDto getEncoursPrevus(Long banqueId, PeriodeRecap periode) {
        Map<Type, BigDecimal> sums = new EnumMap<>(Type.class);
        for (Object[] row : encoursRepository.sumByBanqueAndSensUntil(banqueId, horizon(periode))) {
            sums.put((Type) row[0], (BigDecimal) row[1]);
        }
        return new EncoursPrevusDto(sums.get(Type.ENCAISSEMENT), sums.get(Type.DECAISSEMENT));
    }

    @Override
    public Map<Long, EncoursPrevusDto> getEncoursPrevusParBanque(PeriodeRecap periode) {
        Map<Long, Map<Type, BigDecimal>> sumsByBanque = new HashMap<>();
        for (Object[] row : encoursRepository.sumAllBanquesBySensUntil(horizon(periode))) {
            sumsByBanque
                    .computeIfAbsent((Long) row[0], k -> new EnumMap<>(Type.class))
                    .put((Type) row[1], (BigDecimal) row[2]);
        }

        Map<Long, EncoursPrevusDto> result = new HashMap<>();
        sumsByBanque.forEach((banqueId, sums) -> result.put(banqueId,
                new EncoursPrevusDto(sums.get(Type.ENCAISSEMENT), sums.get(Type.DECAISSEMENT))));
        return result;
    }

    @Override
    public List<SourceRecapDto> getRecapitulatifParSource(RecapitulatifSearchDto searchDto, Long banqueId) {
        Specification<RecapitulatifEncoursView> spec = buildSearchSpecification(searchDto, banqueId);

        return encoursRepository.aggregateBySource(spec, LocalDate.now()).stream()
                .sorted(Comparator.comparingLong(SourceRecapDto::operationsCount).reversed()
                        .thenComparing(s -> s.source().name()))
                .toList();
    }
}
