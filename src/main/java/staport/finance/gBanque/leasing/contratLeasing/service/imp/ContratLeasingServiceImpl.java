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
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.leasing.config.services.interfaces.ILeasingConfigService;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingPhaseRequestDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingRequestDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingResponseDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingSearchDTO;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.contratLeasing.mapper.ContratLeasingMapper;
import staport.finance.gBanque.leasing.contratLeasing.repositories.ContratLeasingRepository;
import staport.finance.gBanque.leasing.contratLeasing.service.ContratLeasingService;
import staport.finance.gBanque.leasing.contratLeasing.specification.ContratLeasingSpecification;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.services.interfaces.IEcheanceLeasingService;
import staport.finance.gBanque.leasing.contratCadre.entities.ContratCadre;
import staport.finance.gBanque.leasing.contratCadre.services.IContratCadreService;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.services.interfaces.OrganismeLeasingService;
import staport.finance.gBanque.reports.service.StaportPdfReportService;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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

    public ContratLeasingServiceImpl(IEcheanceLeasingService echeanceLeasingService,
                                     ContratLeasingRepository contratLeasingRepository,
                                     ContratLeasingMapper contratLeasingMapper,
                                     IBanqueService banqueService,
                                     IContratCadreService contratCadreService,
                                     ILeasingConfigService leasingConfigService,
                                     OrganismeLeasingService organismeLeasingService,
                                     StaportPdfReportService staportPdfReportService) {
        this.contratLeasingRepository = contratLeasingRepository;
        this.contratLeasingMapper = contratLeasingMapper;
        this.banqueService = banqueService;
        this.contratCadreService = contratCadreService;
        this.echeanceLeasingService = echeanceLeasingService;
        this.leasingConfigService = leasingConfigService;
        this.organismeLeasingService = organismeLeasingService;
        this.staportPdfReportService = staportPdfReportService;
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

        return contratLeasingMapper.toDto(savedContrat);
    }

    @Override
    @Transactional(readOnly = true)
    public ContratLeasingResponseDTO getContratLeasingById(Long id) {
        ContratLeasing contrat = findContratOrThrow(id);
        log.debug("Contrat récupéré — numéro : {}", contrat.getNumeroContrat());
        return contratLeasingMapper.toDto(contrat);
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
        return contratLeasingMapper.toDto(updatedContrat);
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
        return contratLeasingMapper.toDto(updatedContrat);
    }

    // ── Listes ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<ContratLeasingResponseDTO> getInactiveContrats(ContratLeasingSearchDTO searchDTO, Pageable pageable) {
        Specification<ContratLeasing> spec = Specification
                .where(ContratLeasingSpecification.getInactiveContrats())
                .and(ContratLeasingSpecification.searchByCriteria(searchDTO));
        return contratLeasingRepository.findAll(spec, effectivePageable(pageable)).map(contratLeasingMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContratLeasingResponseDTO> getPendingLienReleaseContrats(ContratLeasingSearchDTO searchDTO, Pageable pageable) {
        Specification<ContratLeasing> spec = Specification
                .where(ContratLeasingSpecification.getPendingLienReleaseContrats())
                .and(ContratLeasingSpecification.searchByCriteria(searchDTO));
        return contratLeasingRepository.findAll(spec, effectivePageable(pageable)).map(contratLeasingMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContratLeasingResponseDTO> getActiveContrats(ContratLeasingSearchDTO searchDTO, Pageable pageable) {
        Specification<ContratLeasing> spec = Specification
                .where(ContratLeasingSpecification.getActiveContrats())
                .and(ContratLeasingSpecification.searchByCriteria(searchDTO));
        return contratLeasingRepository.findAll(spec, effectivePageable(pageable)).map(contratLeasingMapper::toDto);
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

    // ── Utilitaire ───────────────────────────────────────────────────────────

    private Pageable effectivePageable(Pageable pageable) {
        return pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));
    }

    // ── Export PDF ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] exportActiveContratsPdf(ContratLeasingSearchDTO searchDTO) {
        log.info("Export PDF des contrats de leasing actifs — critères: {}", searchDTO);
        return generateContratsPdf("Contrats de Leasing Actifs",
                ContratLeasingSpecification.getActiveContrats(), searchDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportPendingLienReleaseContratsPdf(ContratLeasingSearchDTO searchDTO) {
        log.info("Export PDF des contrats de leasing en attente de main levée — critères: {}", searchDTO);
        return generateContratsPdf("Contrats de Leasing en Attente de Main Levée",
                ContratLeasingSpecification.getPendingLienReleaseContrats(), searchDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportInactiveContratsPdf(ContratLeasingSearchDTO searchDTO) {
        log.info("Export PDF des contrats de leasing clôturés — critères: {}", searchDTO);
        return generateContratsPdf("Contrats de Leasing Archivés",
                ContratLeasingSpecification.getInactiveContrats(), searchDTO);
    }

    /**
     * Génère le PDF partagé par les 3 écrans de liste (actifs / en attente de main levée /
     * archivés) : même mise en forme, seuls le titre et la spécification de statut varient.
     */
    private byte[] generateContratsPdf(String title, Specification<ContratLeasing> statusSpec, ContratLeasingSearchDTO searchDTO) {
        Specification<ContratLeasing> searchSpec = ContratLeasingSpecification.searchByCriteria(searchDTO);
        List<ContratLeasing> contrats = contratLeasingRepository
                .findAll(Specification.where(statusSpec).and(searchSpec), Sort.by(Sort.Direction.ASC, "id"));

        BigDecimal totalMontantAchat = contrats.stream()
                .map(ContratLeasing::getMontantAchatTTC)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        List<String> filterLines = buildFilterSummary(searchDTO);

        try {
            return staportPdfReportService.generateReport(title, PageSize.A4.rotate(), (doc, writer) -> {
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
                            + "  •  " + contrats.size() + " contrat(s)";
                    if (!filterLines.isEmpty()) {
                        exportInfo += "  •  Filtres : " + String.join("  |  ", filterLines);
                    }
                    Paragraph sub = new Paragraph(exportInfo, subFont);
                    sub.setAlignment(Element.ALIGN_CENTER);
                    sub.setSpacingAfter(8f);
                    doc.add(sub);

                    float[] colWidths = {14f, 12f, 12f, 14f, 10f, 13f, 10f, 9f};
                    PdfPTable table = new PdfPTable(colWidths);
                    table.setWidthPercentage(100f);
                    table.setSpacingBefore(8f);
                    table.setHeaderRows(1);

                    for (String h : new String[]{
                            "Numéro Contrat", "Organisme", "Banque", "Immobilisation",
                            "Type", "Montant Achat TTC (MAD)", "Date Début", "Statut"}) {
                        PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                        c.setBackgroundColor(MED_BLUE);
                        c.setPadding(5f);
                        c.setHorizontalAlignment(Element.ALIGN_CENTER);
                        c.setBorderColor(Color.WHITE);
                        c.setBorderWidth(0.5f);
                        table.addCell(c);
                    }

                    for (int i = 0; i < contrats.size(); i++) {
                        ContratLeasing c = contrats.get(i);
                        Color rowBg = i % 2 == 0 ? Color.WHITE : ROW_ALT;

                        addPdfCell(table, c.getNumeroContrat(), cellFont, rowBg, BORDER_GRAY, Element.ALIGN_LEFT);
                        addPdfCell(table, c.getContratCadre() != null && c.getContratCadre().getOrganisme() != null
                                ? c.getContratCadre().getOrganisme().getNom() : "—", cellFont, rowBg, BORDER_GRAY, Element.ALIGN_LEFT);
                        addPdfCell(table, c.getBanqueDePrelevement() != null ? c.getBanqueDePrelevement().getNom() : "—", cellFont, rowBg, BORDER_GRAY, Element.ALIGN_LEFT);
                        addPdfCell(table, c.getImmobilisation(), cellFont, rowBg, BORDER_GRAY, Element.ALIGN_LEFT);
                        addPdfCell(table, c.getTypeContrat() != null ? c.getTypeContrat().name() : "—", cellFont, rowBg, BORDER_GRAY, Element.ALIGN_CENTER);
                        addPdfCell(table, formatMontantPdf(c.getMontantAchatTTC()), cellFont, rowBg, BORDER_GRAY, Element.ALIGN_RIGHT);
                        addPdfCell(table, c.getDateDebut() != null ? c.getDateDebut().format(DATE_FMT) : "—", cellFont, rowBg, BORDER_GRAY, Element.ALIGN_CENTER);

                        boolean termine = c.isCompleted();
                        PdfPCell statusCell = new PdfPCell(new Phrase(
                                termine ? "CLÔTURÉ" : "EN COURS",
                                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
                        statusCell.setBackgroundColor(termine ? GREEN : ORANGE);
                        statusCell.setPadding(4f);
                        statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        statusCell.setBorderColor(BORDER_GRAY);
                        statusCell.setBorderWidth(0.5f);
                        table.addCell(statusCell);
                    }

                    PdfPCell totalLabel = new PdfPCell(
                            new Phrase("TOTAL — " + contrats.size() + " contrat(s)", totalFont));
                    totalLabel.setColspan(5);
                    totalLabel.setBackgroundColor(LIGHT_BLUE);
                    totalLabel.setPadding(5f);
                    totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalLabel.setBorderColor(MED_BLUE);
                    table.addCell(totalLabel);

                    PdfPCell totalVal = new PdfPCell(
                            new Phrase(formatMontantPdf(totalMontantAchat) + " MAD", totalFont));
                    totalVal.setBackgroundColor(LIGHT_BLUE);
                    totalVal.setPadding(5f);
                    totalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalVal.setBorderColor(MED_BLUE);
                    table.addCell(totalVal);

                    for (int j = 0; j < 2; j++) {
                        PdfPCell empty = new PdfPCell(new Phrase(""));
                        empty.setBackgroundColor(LIGHT_BLUE);
                        empty.setBorderColor(MED_BLUE);
                        table.addCell(empty);
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
