package staport.finance.gBanque.leasing.contratLeasing.service.imp;

import com.lowagie.text.Chunk;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.leasing.config.services.interfaces.ILeasingConfigService;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingPhaseRequestDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingRequestDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingResponseDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingSearchDTO;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasingPhase;
import staport.finance.gBanque.leasing.contratLeasing.mapper.ContratLeasingMapper;
import staport.finance.gBanque.leasing.contratLeasing.repositories.ContratLeasingRepository;
import staport.finance.gBanque.leasing.contratLeasing.service.ContratLeasingService;
import staport.finance.gBanque.leasing.contratLeasing.specification.ContratLeasingSpecification;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.MontantsContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.services.interfaces.IEcheanceLeasingService;
import staport.finance.gBanque.leasing.contratCadre.entities.ContratCadre;
import staport.finance.gBanque.leasing.contratCadre.services.IContratCadreService;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.services.interfaces.OrganismeLeasingService;
import staport.finance.gBanque.reports.service.StaportExcelReportService;
import staport.finance.gBanque.reports.service.StaportPdfReportService;
import staport.finance.gBanque.reports.utils.MontantFormatter;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ContratLeasingServiceImpl implements ContratLeasingService {

    private static final Logger log = LoggerFactory.getLogger(ContratLeasingServiceImpl.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color MED_BLUE = Color.decode("#2563eb");
    private static final Color LIGHT_BLUE = Color.decode("#dbeafe");
    private static final Color ROW_ALT = Color.decode("#f8fafc");
    private static final Color GREEN = Color.decode("#16a34a");
    private static final Color ORANGE = Color.decode("#ea580c");
    private static final Color BORDER_GRAY = Color.decode("#e5e7eb");

    private final ContratLeasingRepository contratLeasingRepository;
    private final ContratLeasingMapper contratLeasingMapper;
    private final IBanqueService banqueService;
    private final IContratCadreService contratCadreService;
    private final IEcheanceLeasingService echeanceLeasingService;
    private final ILeasingConfigService leasingConfigService;
    private final OrganismeLeasingService organismeLeasingService;
    private final StaportPdfReportService staportPdfReportService;
    private final StaportExcelReportService staportExcelReportService;

    public ContratLeasingServiceImpl(IEcheanceLeasingService echeanceLeasingService,
                                     ContratLeasingRepository contratLeasingRepository,
                                     ContratLeasingMapper contratLeasingMapper,
                                     IBanqueService banqueService,
                                     IContratCadreService contratCadreService,
                                     ILeasingConfigService leasingConfigService,
                                     OrganismeLeasingService organismeLeasingService,
                                     StaportPdfReportService staportPdfReportService,
                                     StaportExcelReportService staportExcelReportService) {
        this.contratLeasingRepository = contratLeasingRepository;
        this.contratLeasingMapper = contratLeasingMapper;
        this.banqueService = banqueService;
        this.contratCadreService = contratCadreService;
        this.echeanceLeasingService = echeanceLeasingService;
        this.leasingConfigService = leasingConfigService;
        this.organismeLeasingService = organismeLeasingService;
        this.staportPdfReportService = staportPdfReportService;
        this.staportExcelReportService = staportExcelReportService;
    }

    // ── Méthode interne centralisée ──────────────────────────────────────────

    private ContratLeasing findContratOrThrow(Long id) {
        return contratLeasingRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("ContratLeasing non trouvé — ID : {}", id);
                    return new ResourceNotFoundException("ContratLeasing with ID " + id + " not found.");
                });
    }

    @Override
    @Transactional(readOnly = true)
    public ContratLeasing findOrThrow(Long id) {
        return findContratOrThrow(id);
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = ContratLeasing.class)
    public ContratLeasingResponseDTO createContratLeasing(ContratLeasingRequestDTO requestDTO) {
        log.info("Création d'un contrat de leasing — type : {}, numéro : {}",
                requestDTO.typeContrat(), requestDTO.numeroContrat());

        validateTypeSpecificContratLeasingRequest(requestDTO);

        Banque banque = banqueService.findOrThrow(requestDTO.banqueDePrelevementId());
        ContratCadre contratCadre = contratCadreService.resolveActif(requestDTO.organismeLeasingId());

        ContratLeasing contratLeasing = contratLeasingMapper.toEntity(requestDTO, banque);
        contratLeasing.setContratCadre(contratCadre);
        setValeurResiduelleBasedOnType(contratLeasing, requestDTO.montantAchatTTC());

        ContratLeasing savedContrat = contratLeasingRepository.save(contratLeasing);
        log.info("Contrat de leasing créé — ID : {}", savedContrat.getId());

        echeanceLeasingService.generateEcheances(savedContrat);

        return toDto(savedContrat);
    }

    @Override
    @Transactional(readOnly = true)
    public ContratLeasingResponseDTO getContratLeasingById(Long id) {
        ContratLeasing contrat = findContratOrThrow(id);
        log.debug("Contrat récupéré — numéro : {}", contrat.getNumeroContrat());
        return toDto(contrat);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = ContratLeasing.class)
    public ContratLeasingResponseDTO updateContratLeasing(Long id, ContratLeasingRequestDTO requestDTO) {
        log.info("Mise à jour du contrat de leasing — ID : {}", id);

        ContratLeasing existingContrat = findContratOrThrow(id);
        validateTypeSpecificContratLeasingRequest(requestDTO);

        Banque banque = banqueService.findOrThrow(requestDTO.banqueDePrelevementId());
        ContratCadre contratCadre = contratCadreService.resolveActif(requestDTO.organismeLeasingId());

        contratLeasingMapper.updateEntityFromDto(requestDTO, existingContrat, banque);
        existingContrat.setContratCadre(contratCadre);
        setValeurResiduelleBasedOnType(existingContrat, existingContrat.getMontantAchatTTC());

        ContratLeasing updatedContrat = contratLeasingRepository.save(existingContrat);
        echeanceLeasingService.regenerateEcheances(updatedContrat);

        log.info("Contrat de leasing mis à jour — ID : {}", id);
        return toDto(updatedContrat);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = ContratLeasing.class)
    public void deleteContratLeasing(Long id) {
        log.info("Suppression du contrat de leasing — ID : {}", id);

        ContratLeasing contrat = findContratOrThrow(id);

        boolean hasPaidEcheances = echeanceLeasingService.hasPaidEcheances(id);
        if (hasPaidEcheances) {
            log.warn("Suppression bloquée — contrat ID : {} contient des échéances payées", id);
            throw new ValidationException(
                    "Impossible de supprimer le contrat avec l'ID " + id +
                            " car il contient des échéances déjà payées. " +
                            "Veuillez le marquer comme inactif au lieu de le supprimer."
            );
        }

        echeanceLeasingService.deleteEcheancesAndCommissionByContrat(contrat);
        contratLeasingRepository.deleteById(id);

        log.info("Contrat de leasing supprimé — ID : {}", id);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = ContratLeasing.class)
    public ContratLeasingResponseDTO markAsInactive(Long id) {
        log.info("Clôture du contrat de leasing — ID : {}", id);

        ContratLeasing contrat = findContratOrThrow(id);

        if (contrat.isCompleted()) {
            log.warn("Contrat ID : {} est déjà inactif", id);
            throw new ValidationException("Le contrat avec l'ID " + id + " est déjà inactif.");
        }

        boolean hasPendingInstallments = contrat.getEcheances().stream()
                .anyMatch(echeance -> echeance.getStatutPaiement() == StatutEcheanceLeasing.PREVU);

        if (hasPendingInstallments) {
            log.warn("Clôture bloquée — contrat ID : {} contient encore des échéances PREVU", id);
            throw new ValidationException("Le contrat ne peut être marqué comme inactif que si toutes les échéances ont été payées.");
        }

        contrat.setCompleted(true);
        ContratLeasing updatedContrat = contratLeasingRepository.save(contrat);

        log.info("Contrat ID : {} marqué comme inactif", id);
        return toDto(updatedContrat);
    }

    // ── Listes ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<ContratLeasingResponseDTO> getInactiveContrats(ContratLeasingSearchDTO searchDTO, Pageable pageable) {
        Specification<ContratLeasing> spec = Specification
                .where(ContratLeasingSpecification.getInactiveContrats())
                .and(ContratLeasingSpecification.searchByCriteria(searchDTO));
        return toDtoPage(contratLeasingRepository.findAll(spec, effectivePageable(pageable)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContratLeasingResponseDTO> getPendingLienReleaseContrats(ContratLeasingSearchDTO searchDTO, Pageable pageable) {
        Specification<ContratLeasing> spec = Specification
                .where(ContratLeasingSpecification.getPendingLienReleaseContrats())
                .and(ContratLeasingSpecification.searchByCriteria(searchDTO));
        return toDtoPage(contratLeasingRepository.findAll(spec, effectivePageable(pageable)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContratLeasingResponseDTO> getActiveContrats(ContratLeasingSearchDTO searchDTO, Pageable pageable) {
        Specification<ContratLeasing> spec = Specification
                .where(ContratLeasingSpecification.getActiveContrats())
                .and(ContratLeasingSpecification.searchByCriteria(searchDTO));
        return toDtoPage(contratLeasingRepository.findAll(spec, effectivePageable(pageable)));
    }

    // ── Validation métier ────────────────────────────────────────────────────

    private void validateTypeSpecificContratLeasingRequest(ContratLeasingRequestDTO requestDTO) {
        if (requestDTO.typeContrat() == null) {
            log.warn("Type de contrat manquant dans la requête");
            throw new ValidationException("Le type de contrat est obligatoire.");
        }
        switch (requestDTO.typeContrat()) {
            case LINEAIRE -> validateLineaireContrat(requestDTO);
            case PROGRESSIF -> validateProgressifContrat(requestDTO);
            case DEGRESSIF -> validateDegressifContrat(requestDTO);
            default -> {
                log.warn("Type de contrat inconnu : {}", requestDTO.typeContrat());
                throw new ValidationException("Type de contrat inconnu: " + requestDTO.typeContrat());
            }
        }
    }

    private void validateLineaireContrat(ContratLeasingRequestDTO requestDTO) {
        if (requestDTO.phases() == null || requestDTO.phases().size() != 1) {
            throw new ValidationException("Un contrat de type LINEAIRE doit avoir exactement une seule phase.");
        }
        ContratLeasingPhaseRequestDTO singlePhase = requestDTO.phases().get(0);
        if (singlePhase.numeroPhase() == null || singlePhase.numeroPhase() != 1) {
            throw new ValidationException("Pour un contrat LINEAIRE, la phase unique doit avoir le numéro 1.");
        }
        if (!singlePhase.dureeMois().equals(requestDTO.periodeEnMois())) {
            throw new ValidationException(String.format(
                    "Pour un contrat LINEAIRE, la durée de la phase unique (%d mois) doit correspondre à la période totale du contrat (%d mois).",
                    singlePhase.dureeMois(), requestDTO.periodeEnMois()
            ));
        }
    }

    private void validateProgressifContrat(ContratLeasingRequestDTO requestDTO) {
        validatePhasedContrat(requestDTO);
        List<ContratLeasingPhaseRequestDTO> sortedPhases = requestDTO.phases().stream()
                .sorted(Comparator.comparingInt(ContratLeasingPhaseRequestDTO::numeroPhase))
                .toList();
        for (int i = 1; i < sortedPhases.size(); i++) {
            ContratLeasingPhaseRequestDTO current = sortedPhases.get(i);
            ContratLeasingPhaseRequestDTO previous = sortedPhases.get(i - 1);
            if (current.mensualiteTTC().compareTo(previous.mensualiteTTC()) < 0) {
                throw new ValidationException(String.format(
                        "Pour un contrat PROGRESSIF, la mensualité de la phase %d (%.2f) doit être supérieure ou égale à celle de la phase %d (%.2f).",
                        current.numeroPhase(), current.mensualiteTTC(),
                        previous.numeroPhase(), previous.mensualiteTTC()
                ));
            }
        }
    }

    private void validateDegressifContrat(ContratLeasingRequestDTO requestDTO) {
        validatePhasedContrat(requestDTO);
        List<ContratLeasingPhaseRequestDTO> sortedPhases = requestDTO.phases().stream()
                .sorted(Comparator.comparingInt(ContratLeasingPhaseRequestDTO::numeroPhase))
                .toList();
        for (int i = 1; i < sortedPhases.size(); i++) {
            ContratLeasingPhaseRequestDTO current = sortedPhases.get(i);
            ContratLeasingPhaseRequestDTO previous = sortedPhases.get(i - 1);
            if (current.mensualiteTTC().compareTo(previous.mensualiteTTC()) > 0) {
                throw new ValidationException(String.format(
                        "Pour un contrat DEGRESSIF, la mensualité de la phase %d (%.2f) doit être inférieure ou égale à celle de la phase %d (%.2f).",
                        current.numeroPhase(), current.mensualiteTTC(),
                        previous.numeroPhase(), previous.mensualiteTTC()
                ));
            }
        }
    }

    private void validatePhasedContrat(ContratLeasingRequestDTO requestDTO) {
        if (requestDTO.phases() == null || requestDTO.phases().isEmpty()) {
            throw new ValidationException("Un contrat de type " + requestDTO.typeContrat().name() + " doit définir des phases.");
        }

        AtomicInteger totalPhaseDuration = new AtomicInteger(0);
        Set<Integer> phaseNumbers = new HashSet<>();

        for (ContratLeasingPhaseRequestDTO phase : requestDTO.phases()) {
            if (!phaseNumbers.add(phase.numeroPhase())) {
                throw new ValidationException("Les numéros de phase doivent être uniques par contrat.");
            }
            totalPhaseDuration.addAndGet(phase.dureeMois());
        }

        if (!Integer.valueOf(totalPhaseDuration.get()).equals(requestDTO.periodeEnMois())) {
            throw new ValidationException(String.format(
                    "La somme des durées des phases (%d mois) ne correspond pas à la période totale du contrat (%d mois).",
                    totalPhaseDuration.get(), requestDTO.periodeEnMois()
            ));
        }

        List<Integer> sortedPhaseNumbers = requestDTO.phases().stream()
                .map(ContratLeasingPhaseRequestDTO::numeroPhase)
                .sorted()
                .toList();
        for (int i = 0; i < sortedPhaseNumbers.size(); i++) {
            if (sortedPhaseNumbers.get(i) != (i + 1)) {
                throw new ValidationException("Les numéros de phase doivent être séquentiels et commencer par 1 (e.g., 1, 2, 3...).");
            }
        }
    }

    // ── Calcul ───────────────────────────────────────────────────────────────

    private void setValeurResiduelleBasedOnType(ContratLeasing contratLeasing, BigDecimal montantAchatTTC) {
        BigDecimal valeurResiduelle = montantAchatTTC
                .multiply(leasingConfigService.getValeurResiduelle())
                .setScale(2, RoundingMode.HALF_UP);
        contratLeasing.setValeurResiduelle(valeurResiduelle);
    }

    // ── Mapping DTO ──────────────────────────────────────────────────────────

    /**
     * Le plafond affiché est toujours le plafond en cours de l'organisme (contrat cadre ACTIF non échu),
     * jamais celui du contrat cadre signé à la création — d'où le calcul ici plutôt que dans le mapper.
     */
    private ContratLeasingResponseDTO toDto(ContratLeasing contrat) {
        return toDtos(List.of(contrat)).get(0);
    }

    private Page<ContratLeasingResponseDTO> toDtoPage(Page<ContratLeasing> page) {
        return new PageImpl<>(toDtos(page.getContent()), page.getPageable(), page.getTotalElements());
    }

    /** Une seule requête de plafonds pour tous les contrats de la liste. */
    private List<ContratLeasingResponseDTO> toDtos(List<ContratLeasing> contrats) {
        Map<Long, BigDecimal> plafonds = plafondsEnCours(contrats);
        return contrats.stream()
                .map(c -> contratLeasingMapper.toDto(c).withPlafond(plafonds.get(organismeId(c))))
                .toList();
    }

    private Map<Long, BigDecimal> plafondsEnCours(List<ContratLeasing> contrats) {
        return contratCadreService.findPlafondsEnCours(
                contrats.stream().map(this::organismeId).collect(Collectors.toSet()));
    }

    private Long organismeId(ContratLeasing contrat) {
        return contrat.getContratCadre().getOrganisme().getId();
    }

    // ── Utilitaire ───────────────────────────────────────────────────────────

    private Pageable effectivePageable(Pageable pageable) {
        return pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));
    }

    // ── Export (PDF / Excel) ─────────────────────────────────────────────────

    /** Ordre canonique des colonnes exportables — fixe l'ordre d'affichage dans le PDF et l'Excel. */
    private static final List<String> LEASING_EXPORT_COLUMNS = List.of(
            "numeroContrat", "banque", "organisme", "immobilisation", "type", "montantAchatTTC", "plafond",
            "echeances", "dateDebut", "dateFin", "duree", "statut", "endettement", "resteAPayer", "dejaPaye"
    );

    private static final Map<String, String> LEASING_COLUMN_LABELS = Map.ofEntries(
            Map.entry("numeroContrat", "N° Contrat"),
            Map.entry("banque", "Banque"),
            Map.entry("organisme", "Organisme Leasing"),
            Map.entry("immobilisation", "Immobilisation"),
            Map.entry("type", "Type"),
            Map.entry("montantAchatTTC", "Montant Achat TTC (MAD)"),
            Map.entry("plafond", "Plafond organisme (MAD)"),
            Map.entry("echeances", "Échéances"),
            Map.entry("dateDebut", "Date Début"),
            Map.entry("dateFin", "Date Fin"),
            Map.entry("duree", "Durée"),
            Map.entry("statut", "Statut"),
            Map.entry("endettement", "Endettement (MAD)"),
            Map.entry("resteAPayer", "Reste à payer (MAD)"),
            Map.entry("dejaPaye", "Déjà payé (MAD)")
    );

    private static final Map<String, Float> LEASING_COLUMN_WIDTHS = Map.ofEntries(
            Map.entry("numeroContrat", 9f),
            Map.entry("banque", 8f),
            Map.entry("organisme", 8f),
            Map.entry("immobilisation", 10f),
            Map.entry("type", 6f),
            Map.entry("montantAchatTTC", 8f),
            Map.entry("plafond", 8f),
            Map.entry("echeances", 9f),
            Map.entry("dateDebut", 6f),
            Map.entry("dateFin", 6f),
            Map.entry("duree", 5f),
            Map.entry("statut", 6f),
            Map.entry("endettement", 8f),
            Map.entry("resteAPayer", 8f),
            Map.entry("dejaPaye", 8f)
    );

    /** Colonnes de montants — alignées à droite et numériques dans l'Excel. */
    private static final Set<String> LEASING_AMOUNT_COLUMNS =
            Set.of("montantAchatTTC", "plafond", "endettement", "resteAPayer", "dejaPaye");

    /** Colonnes de montants totalisées en bas du document (le plafond, propre à l'organisme, ne s'additionne pas). */
    private static final Set<String> LEASING_TOTAL_COLUMNS =
            Set.of("montantAchatTTC", "endettement", "resteAPayer", "dejaPaye");

    /** Contrat enrichi des valeurs calculées nécessaires à l'export. */
    private record ExportRow(ContratLeasing contrat, BigDecimal plafond, MontantsContratLeasing montants) {

        BigDecimal endettement() {
            return montants != null ? montants.endettement() : BigDecimal.ZERO;
        }

        BigDecimal dejaPaye() {
            return montants != null ? montants.dejaPaye() : BigDecimal.ZERO;
        }

        BigDecimal resteAPayer() {
            return endettement().subtract(dejaPaye());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportActiveContratsPdf(ContratLeasingSearchDTO searchDTO, List<String> colonnes) {
        log.info("Export PDF des contrats de leasing actifs — critères: {}, colonnes: {}", searchDTO, colonnes);
        return generateContratsPdf("Contrats de Leasing Actifs",
                ContratLeasingSpecification.getActiveContrats(), searchDTO, colonnes);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportPendingLienReleaseContratsPdf(ContratLeasingSearchDTO searchDTO, List<String> colonnes) {
        log.info("Export PDF des contrats de leasing en attente de main levée — critères: {}, colonnes: {}", searchDTO, colonnes);
        return generateContratsPdf("Contrats de Leasing en Attente de Main Levée",
                ContratLeasingSpecification.getPendingLienReleaseContrats(), searchDTO, colonnes);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportInactiveContratsPdf(ContratLeasingSearchDTO searchDTO, List<String> colonnes) {
        log.info("Export PDF des contrats de leasing clôturés — critères: {}, colonnes: {}", searchDTO, colonnes);
        return generateContratsPdf("Contrats de Leasing Archivés",
                ContratLeasingSpecification.getInactiveContrats(), searchDTO, colonnes);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportActiveContratsExcel(ContratLeasingSearchDTO searchDTO, List<String> colonnes) {
        log.info("Export Excel des contrats de leasing actifs — critères: {}, colonnes: {}", searchDTO, colonnes);
        return generateContratsExcel("Contrats de Leasing Actifs",
                ContratLeasingSpecification.getActiveContrats(), searchDTO, colonnes);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportPendingLienReleaseContratsExcel(ContratLeasingSearchDTO searchDTO, List<String> colonnes) {
        log.info("Export Excel des contrats de leasing en attente de main levée — critères: {}, colonnes: {}", searchDTO, colonnes);
        return generateContratsExcel("Contrats de Leasing en Attente de Main Levée",
                ContratLeasingSpecification.getPendingLienReleaseContrats(), searchDTO, colonnes);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportInactiveContratsExcel(ContratLeasingSearchDTO searchDTO, List<String> colonnes) {
        log.info("Export Excel des contrats de leasing clôturés — critères: {}, colonnes: {}", searchDTO, colonnes);
        return generateContratsExcel("Contrats de Leasing Archivés",
                ContratLeasingSpecification.getInactiveContrats(), searchDTO, colonnes);
    }

    /**
     * Résout la liste des colonnes à exporter : filtre {@link #LEASING_EXPORT_COLUMNS} pour ne garder
     * que celles demandées (ordre canonique préservé), en ignorant toute clé inconnue. {@code null},
     * vide, ou sans aucune clé valide ⇒ toutes les colonnes.
     */
    private List<String> resolveLeasingColonnes(List<String> demandees) {
        if (demandees == null || demandees.isEmpty()) {
            return LEASING_EXPORT_COLUMNS;
        }
        List<String> filtrees = LEASING_EXPORT_COLUMNS.stream()
                .filter(demandees::contains)
                .collect(Collectors.toList());
        return filtrees.isEmpty() ? LEASING_EXPORT_COLUMNS : filtrees;
    }

    /** Charge les contrats filtrés et leurs valeurs calculées (plafond en cours, endettement, déjà payé) en 3 requêtes. */
    private List<ExportRow> loadExportRows(Specification<ContratLeasing> statusSpec, ContratLeasingSearchDTO searchDTO) {
        Specification<ContratLeasing> searchSpec = ContratLeasingSpecification.searchByCriteria(searchDTO);
        List<ContratLeasing> contrats = contratLeasingRepository
                .findAll(Specification.where(statusSpec).and(searchSpec), Sort.by(Sort.Direction.ASC, "id"));

        Map<Long, BigDecimal> plafonds = plafondsEnCours(contrats);
        Map<Long, MontantsContratLeasing> montants = echeanceLeasingService.getMontantsByContratIds(
                contrats.stream().map(ContratLeasing::getId).toList());

        return contrats.stream()
                .map(c -> new ExportRow(c, plafonds.get(organismeId(c)), montants.get(c.getId())))
                .toList();
    }

    /** Valeur numérique d'une colonne de montants ({@code null} pour un plafond en cours inexistant). */
    private BigDecimal amountValue(String colonne, ExportRow row) {
        return switch (colonne) {
            case "montantAchatTTC" -> row.contrat().getMontantAchatTTC();
            case "plafond" -> row.plafond();
            case "endettement" -> row.endettement();
            case "resteAPayer" -> row.resteAPayer();
            case "dejaPaye" -> row.dejaPaye();
            default -> throw new IllegalArgumentException("Colonne de montant inconnue : " + colonne);
        };
    }

    /** Texte d'une colonne (les colonnes de montants sont formatées à la française). */
    private String columnText(String colonne, ExportRow row) {
        ContratLeasing c = row.contrat();
        return switch (colonne) {
            case "numeroContrat" -> c.getNumeroContrat();
            case "banque" -> c.getBanqueDePrelevement() != null ? c.getBanqueDePrelevement().getNom() : "—";
            case "organisme" -> c.getContratCadre() != null && c.getContratCadre().getOrganisme() != null
                    ? c.getContratCadre().getOrganisme().getNom() : "—";
            case "immobilisation" -> c.getImmobilisation();
            case "type" -> c.getTypeContrat() != null ? c.getTypeContrat().name() : "—";
            case "echeances" -> echeancesDisplay(c);
            case "dateDebut" -> c.getDateDebut() != null ? c.getDateDebut().format(DATE_FMT) : "—";
            case "dateFin" -> c.getDateDebut() != null && c.getPeriodeEnMois() != null
                    ? c.getDateDebut().plusMonths(c.getPeriodeEnMois()).format(DATE_FMT) : "—";
            case "duree" -> c.getPeriodeEnMois() != null ? c.getPeriodeEnMois() + " mois" : "—";
            case "statut" -> c.isCompleted() ? "CLÔTURÉ" : "EN COURS";
            default -> MontantFormatter.format(amountValue(colonne, row));
        };
    }

    /** Mensualité(s) du contrat — même affichage que la liste : montant unique ou fourchette min - max entre phases. */
    private String echeancesDisplay(ContratLeasing c) {
        List<BigDecimal> mensualites = c.getPhases().stream().map(ContratLeasingPhase::getMensualiteTTC).toList();
        if (mensualites.isEmpty()) {
            return "N/A";
        }
        BigDecimal min = mensualites.stream().min(Comparator.naturalOrder()).orElseThrow();
        BigDecimal max = mensualites.stream().max(Comparator.naturalOrder()).orElseThrow();
        return min.compareTo(max) == 0
                ? MontantFormatter.format(min) + " MAD"
                : MontantFormatter.format(min) + " MAD - " + MontantFormatter.format(max) + " MAD";
    }

    /** Totaux des colonnes de montants sélectionnées, dans l'ordre des colonnes. */
    private Map<String, BigDecimal> computeTotals(List<String> colonnes, List<ExportRow> rows) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (String colonne : colonnes) {
            if (LEASING_TOTAL_COLUMNS.contains(colonne)) {
                totals.put(colonne, rows.stream()
                        .map(r -> amountValue(colonne, r))
                        .filter(java.util.Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP));
            }
        }
        return totals;
    }

    private String exportInfoLine(int count, List<String> filterLines) {
        String exportInfo = "Exporté le " + LocalDate.now().format(DATE_FMT) + "  •  " + count + " contrat(s)";
        if (!filterLines.isEmpty()) {
            exportInfo += "  •  Filtres : " + String.join("  |  ", filterLines);
        }
        return exportInfo;
    }

    /**
     * Génère le PDF partagé par les 3 écrans de liste (actifs / en attente de main levée /
     * archivés) : même mise en forme, seuls le titre et la spécification de statut varient.
     */
    private byte[] generateContratsPdf(String title, Specification<ContratLeasing> statusSpec,
                                       ContratLeasingSearchDTO searchDTO, List<String> colonnesDemandees) {
        List<ExportRow> rows = loadExportRows(statusSpec, searchDTO);
        List<String> colonnes = resolveLeasingColonnes(colonnesDemandees);
        Map<String, BigDecimal> totals = computeTotals(colonnes, rows);
        List<String> filterLines = buildFilterSummary(searchDTO);
        float fontSize = colonnes.size() > 8 ? 7f : 8f;

        try {
            return staportPdfReportService.generateReport(title, PageSize.A4.rotate(), (doc, writer) -> {
                try {
                    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
                    Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.decode("#374151"));
                    Font hdrFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, fontSize, Color.WHITE);
                    Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, fontSize, Color.BLACK);
                    Font statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, fontSize, Color.WHITE);
                    Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.decode("#1e3a8a"));

                    doc.add(Chunk.NEWLINE);
                    doc.add(Chunk.NEWLINE);

                    Paragraph titleParagraph = new Paragraph(title, titleFont);
                    titleParagraph.setAlignment(Element.ALIGN_CENTER);
                    doc.add(titleParagraph);

                    Paragraph sub = new Paragraph(exportInfoLine(rows.size(), filterLines), subFont);
                    sub.setAlignment(Element.ALIGN_CENTER);
                    sub.setSpacingAfter(8f);
                    doc.add(sub);

                    float[] colWidths = new float[colonnes.size()];
                    for (int i = 0; i < colonnes.size(); i++) {
                        colWidths[i] = LEASING_COLUMN_WIDTHS.get(colonnes.get(i));
                    }
                    PdfPTable table = new PdfPTable(colWidths);
                    table.setWidthPercentage(100f);
                    table.setSpacingBefore(8f);
                    table.setHeaderRows(1);

                    for (String colonne : colonnes) {
                        PdfPCell c = new PdfPCell(new Phrase(LEASING_COLUMN_LABELS.get(colonne), hdrFont));
                        c.setBackgroundColor(MED_BLUE);
                        c.setPadding(5f);
                        c.setHorizontalAlignment(Element.ALIGN_CENTER);
                        c.setBorderColor(Color.WHITE);
                        c.setBorderWidth(0.5f);
                        table.addCell(c);
                    }

                    for (int i = 0; i < rows.size(); i++) {
                        ExportRow row = rows.get(i);
                        Color rowBg = i % 2 == 0 ? Color.WHITE : ROW_ALT;

                        for (String colonne : colonnes) {
                            if ("statut".equals(colonne)) {
                                boolean termine = row.contrat().isCompleted();
                                PdfPCell statusCell = new PdfPCell(new Phrase(columnText(colonne, row), statusFont));
                                statusCell.setBackgroundColor(termine ? GREEN : ORANGE);
                                statusCell.setPadding(4f);
                                statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                                statusCell.setBorderColor(BORDER_GRAY);
                                statusCell.setBorderWidth(0.5f);
                                table.addCell(statusCell);
                            } else {
                                addPdfCell(table, columnText(colonne, row), cellFont, rowBg, BORDER_GRAY, pdfAlignment(colonne));
                            }
                        }
                    }

                    if (!totals.isEmpty()) {
                        addPdfTotalRow(table, colonnes, totals, rows.size(), totalFont);
                    }

                    doc.add(table);
                } catch (DocumentException e) {
                    throw new RuntimeException("Erreur lors de la génération du contenu du PDF des contrats de leasing.", e);
                }
            });
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des contrats de leasing.", e);
        }
    }

    private int pdfAlignment(String colonne) {
        if (LEASING_AMOUNT_COLUMNS.contains(colonne)) {
            return Element.ALIGN_RIGHT;
        }
        return switch (colonne) {
            case "type", "dateDebut", "dateFin", "duree" -> Element.ALIGN_CENTER;
            default -> Element.ALIGN_LEFT;
        };
    }

    /** Ligne de totaux : libellé sur les colonnes précédant le premier total, puis un total sous chaque colonne de montants totalisée. */
    private void addPdfTotalRow(PdfPTable table, List<String> colonnes, Map<String, BigDecimal> totals, int count, Font totalFont) {
        String label = "TOTAL — " + count + " contrat(s)";
        int firstTotal = colonnes.indexOf(totals.keySet().iterator().next());

        if (firstTotal > 0) {
            PdfPCell totalLabel = new PdfPCell(new Phrase(label, totalFont));
            totalLabel.setColspan(firstTotal);
            totalLabel.setBackgroundColor(LIGHT_BLUE);
            totalLabel.setPadding(5f);
            totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalLabel.setBorderColor(MED_BLUE);
            table.addCell(totalLabel);
        }

        for (int i = Math.max(firstTotal, 0); i < colonnes.size(); i++) {
            String colonne = colonnes.get(i);
            BigDecimal total = totals.get(colonne);
            String text = "";
            if (total != null) {
                text = MontantFormatter.format(total) + " MAD";
                if (firstTotal == 0 && i == 0) {
                    text = label + " : " + text;
                }
            }
            PdfPCell cell = new PdfPCell(new Phrase(text, totalFont));
            cell.setBackgroundColor(LIGHT_BLUE);
            cell.setPadding(5f);
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cell.setBorderColor(MED_BLUE);
            table.addCell(cell);
        }
    }

    /** Génère l'Excel — mêmes données et mêmes colonnes que {@link #generateContratsPdf}, seule la mise en forme diffère. */
    private byte[] generateContratsExcel(String title, Specification<ContratLeasing> statusSpec,
                                         ContratLeasingSearchDTO searchDTO, List<String> colonnesDemandees) {
        List<ExportRow> rows = loadExportRows(statusSpec, searchDTO);
        List<String> colonnes = resolveLeasingColonnes(colonnesDemandees);
        Map<String, BigDecimal> totals = computeTotals(colonnes, rows);
        List<String> filterLines = buildFilterSummary(searchDTO);

        return staportExcelReportService.generateReport("Contrats Leasing", (workbook, sheet) -> {
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

            CellStyle emptyAmountStyle = workbook.createCellStyle();
            emptyAmountStyle.setAlignment(HorizontalAlignment.RIGHT);

            org.apache.poi.ss.usermodel.Font statutFont = workbook.createFont();
            statutFont.setBold(true);
            statutFont.setColor(IndexedColors.WHITE.getIndex());
            CellStyle statutTermineStyle = workbook.createCellStyle();
            statutTermineStyle.setFont(statutFont);
            statutTermineStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            statutTermineStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            statutTermineStyle.setAlignment(HorizontalAlignment.CENTER);
            CellStyle statutEnCoursStyle = workbook.createCellStyle();
            statutEnCoursStyle.setFont(statutFont);
            statutEnCoursStyle.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
            statutEnCoursStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            statutEnCoursStyle.setAlignment(HorizontalAlignment.CENTER);

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
            titleCell.setCellValue(title);
            titleCell.setCellStyle(titleStyle);
            if (lastCol > 0) sheet.addMergedRegion(new CellRangeAddress(titleRow.getRowNum(), titleRow.getRowNum(), 0, lastCol));

            Row subRow = sheet.createRow(rowIdx++);
            org.apache.poi.ss.usermodel.Cell subCell = subRow.createCell(0);
            subCell.setCellValue(exportInfoLine(rows.size(), filterLines));
            subCell.setCellStyle(subStyle);
            if (lastCol > 0) sheet.addMergedRegion(new CellRangeAddress(subRow.getRowNum(), subRow.getRowNum(), 0, lastCol));

            rowIdx++; // ligne vide de séparation

            Row headerRow = sheet.createRow(rowIdx++);
            for (int i = 0; i < colonnes.size(); i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(LEASING_COLUMN_LABELS.get(colonnes.get(i)));
                cell.setCellStyle(headerStyle);
            }

            for (ExportRow exportRow : rows) {
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < colonnes.size(); i++) {
                    String colonne = colonnes.get(i);
                    org.apache.poi.ss.usermodel.Cell cell = row.createCell(i);
                    if (LEASING_AMOUNT_COLUMNS.contains(colonne)) {
                        BigDecimal montant = amountValue(colonne, exportRow);
                        if (montant != null) {
                            cell.setCellValue(montant.doubleValue());
                            cell.setCellStyle(numberStyle);
                        } else {
                            cell.setCellValue("—");
                            cell.setCellStyle(emptyAmountStyle);
                        }
                    } else if ("statut".equals(colonne)) {
                        cell.setCellValue(columnText(colonne, exportRow));
                        cell.setCellStyle(exportRow.contrat().isCompleted() ? statutTermineStyle : statutEnCoursStyle);
                    } else {
                        cell.setCellValue(columnText(colonne, exportRow));
                        cell.setCellStyle(textStyle);
                    }
                }
            }

            if (!totals.isEmpty()) {
                int firstTotal = colonnes.indexOf(totals.keySet().iterator().next());
                String label = "TOTAL — " + rows.size() + " contrat(s)";

                if (firstTotal == 0) {
                    // Le premier total occupe la colonne 0 : le libellé est porté par une ligne dédiée au-dessus.
                    org.apache.poi.ss.usermodel.Cell labelCell = sheet.createRow(rowIdx++).createCell(0);
                    labelCell.setCellValue(label);
                    labelCell.setCellStyle(totalLabelStyle);
                }

                Row totalRow = sheet.createRow(rowIdx);
                for (int i = 0; i < colonnes.size(); i++) {
                    org.apache.poi.ss.usermodel.Cell cell = totalRow.createCell(i);
                    BigDecimal total = totals.get(colonnes.get(i));
                    if (total != null) {
                        cell.setCellValue(total.doubleValue());
                        cell.setCellStyle(totalValueStyle);
                    } else {
                        cell.setCellStyle(totalLabelStyle);
                    }
                }

                if (firstTotal > 0) {
                    totalRow.getCell(0).setCellValue(label);
                    if (firstTotal > 1) {
                        sheet.addMergedRegion(new CellRangeAddress(totalRow.getRowNum(), totalRow.getRowNum(), 0, firstTotal - 1));
                    }
                }
            }

            for (int i = 0; i < colonnes.size(); i++) {
                sheet.autoSizeColumn(i);
            }
        });
    }

    private List<String> buildFilterSummary(ContratLeasingSearchDTO dto) {
        List<String> lines = new ArrayList<>();
        if (dto == null) return lines;

        if (dto.numeroContrat() != null && !dto.numeroContrat().isBlank()) {
            lines.add("Numéro Contrat : " + dto.numeroContrat());
        }
        if (dto.banqueDePrelevementId() != null) {
            try {
                lines.add("Banque : " + banqueService.findOrThrow(dto.banqueDePrelevementId()).getNom());
            } catch (ResourceNotFoundException e) {
                log.debug("Banque id={} introuvable pour le résumé de filtre export — ignorée", dto.banqueDePrelevementId());
            }
        }
        if (dto.organismeLeasingId() != null) {
            try {
                lines.add("Organisme : " + organismeLeasingService.findOrThrow(dto.organismeLeasingId()).getNom());
            } catch (ResourceNotFoundException e) {
                log.debug("Organisme id={} introuvable pour le résumé de filtre export — ignoré", dto.organismeLeasingId());
            }
        }
        if (dto.immobilisation() != null && !dto.immobilisation().isBlank()) {
            lines.add("Immobilisation : " + dto.immobilisation());
        }
        if (dto.typeContrat() != null) {
            lines.add("Type : " + dto.typeContrat().name());
        }
        if (dto.dateDebutMin() != null) {
            lines.add("Date début min : " + dto.dateDebutMin().format(DATE_FMT));
        }
        if (dto.dateDebutMax() != null) {
            lines.add("Date début max : " + dto.dateDebutMax().format(DATE_FMT));
        }
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
}
