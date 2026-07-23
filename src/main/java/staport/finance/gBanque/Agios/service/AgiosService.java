package staport.finance.gBanque.Agios.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
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
import staport.finance.gBanque.Agios.dtos.AgiosRequestDto;
import staport.finance.gBanque.Agios.dtos.AgiosResponseDto;
import staport.finance.gBanque.Agios.dtos.AgiosSearchDto;
import staport.finance.gBanque.Agios.dtos.EncoursAgiosDto;
import staport.finance.gBanque.Agios.entities.Agios;
import staport.finance.gBanque.Agios.exceptions.AgiosBusinessException;
import staport.finance.gBanque.Agios.exceptions.AgiosNotFoundException;
import staport.finance.gBanque.Agios.mapper.AgiosMapper;
import staport.finance.gBanque.Agios.repository.AgiosRepository;
import staport.finance.gBanque.Agios.spec.AgiosSpec;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.reports.service.StaportPdfReportService;

import java.awt.Color;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class AgiosService implements IAgiosService {

    private static final Logger log = LoggerFactory.getLogger(AgiosService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color MED_BLUE = Color.decode("#2563eb");
    private static final Color LIGHT_BLUE = Color.decode("#dbeafe");
    private static final Color ROW_ALT = Color.decode("#f8fafc");
    private static final Color BORDER_GRAY = Color.decode("#e5e7eb");

    private final AgiosRepository agiosRepository;
    private final IBanqueService banqueService;
    private final AgiosMapper agiosMapper;
    private final StaportPdfReportService staportPdfReportService;

    public AgiosService(AgiosRepository agiosRepository,
                        IBanqueService banqueService,
                        AgiosMapper agiosMapper,
                        StaportPdfReportService staportPdfReportService) {
        this.agiosRepository = agiosRepository;
        this.banqueService   = banqueService;
        this.agiosMapper     = agiosMapper;
        this.staportPdfReportService = staportPdfReportService;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsChargeFixeGeneree(Long banqueId, String libelle, LocalDate debut, LocalDate fin) {
        return agiosRepository.existsByBanqueIdAndLibelleAndDateEmissionBetween(banqueId, libelle, debut, fin);
    }

    // ── Création ──────────────────────────────────────────────────────────────

    @Override
    @AuditableAction(operation = AuditOperation.CREATE, entity = Agios.class)
    @Transactional
    public AgiosResponseDto createAgios(AgiosRequestDto dto) {
        log.info("Création d'un agios — banqueId: {}, montant: {}", dto.banqueId(), dto.montant());

        Banque banque = findBanqueOuLeverException(dto.banqueId());

        Agios agios = new Agios();
        agios.setLibelle(dto.libelle());
        agios.setObservation(dto.observation());
        agios.setMontant(dto.montant());
        agios.setBanque(banque);
        agios.setDateEmission(dto.dateEmission());

        Agios saved = agiosRepository.save(agios);
        log.info("Agios créé — id: {}, statut: {}", saved.getId(), saved.getStatus());
        return agiosMapper.toResponseDto(saved);
    }

    // ── Lecture ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AgiosResponseDto getById(Long id) {
        log.info("Récupération de l'agios — id: {}", id);
        return agiosMapper.toResponseDto(findAgiosOuLeverException(id));
    }

    @Transactional(readOnly = true)
    public Page<AgiosResponseDto> searchAgios(AgiosSearchDto dto, Pageable pageable) {
        log.info("Recherche d'agios — page: {}, taille: {}", pageable.getPageNumber(), pageable.getPageSize());
        return agiosRepository.findAll(AgiosSpec.searchByCriteria(dto), pageable)
                .map(agiosMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public byte[] exportAgiosPdf(AgiosSearchDto dto) {
        log.info("Export PDF des agios ponctuels - Critères: {}", dto);

        List<AgiosResponseDto> agios = agiosRepository
                .findAll(AgiosSpec.searchByCriteria(dto), Sort.by(Sort.Direction.DESC, "dateEmission"))
                .stream()
                .map(agiosMapper::toResponseDto)
                .toList();

        return generateAgiosListPdf("Agios", agios, buildFilterSummary(dto), null);
    }

    @Transactional(readOnly = true)
    public byte[] exportOutstandingAgiosPdf(AgiosSearchDto dto, Long banqueId) {
        log.info("Export PDF des agios en cours - banqueId: {}, Critères: {}", banqueId, dto);

        LocalDate finDuMois = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        Specification<Agios> spec = Specification
                .where(AgiosSpec.outstandingUntil(finDuMois))
                .and(AgiosSpec.hasBanque(banqueId))
                .and(AgiosSpec.hasLibelle(dto.libelle()))
                .and(AgiosSpec.hasMontant(dto.montant()));

        List<AgiosResponseDto> agios = agiosRepository
                .findAll(spec, Sort.by(Sort.Direction.ASC, "dateEmission"))
                .stream()
                .map(agiosMapper::toResponseDto)
                .toList();

        List<String> filterLines = buildFilterSummary(new AgiosSearchDto(banqueId, dto.libelle(), dto.montant(), null));

        return generateAgiosListPdf("Encours Agios", agios, filterLines, "encours");
    }

    @Transactional(readOnly = true)
    public EncoursAgiosDto getOutstandingAgios(AgiosSearchDto dto, Pageable pageable) {
        LocalDate finDuMois = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        Specification<Agios> spec = Specification
                .where(AgiosSpec.outstandingUntil(finDuMois))
                .and(AgiosSpec.hasBanque(dto.banqueId()))
                .and(AgiosSpec.hasLibelle(dto.libelle()))
                .and(AgiosSpec.hasMontant(dto.montant()));

        Pageable orderedPageable = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));

        Page<AgiosResponseDto> pageDto = agiosRepository.findAll(spec, orderedPageable)
                .map(agiosMapper::toResponseDto);

        BigDecimal totalDesEncours = agiosRepository.findAll(spec).stream()
                .map(Agios::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Encours agios récupérés — banqueId: {}, total: {}", dto.banqueId(), totalDesEncours);
        return new EncoursAgiosDto(totalDesEncours, pageDto);
    }

    // ── Modification ──────────────────────────────────────────────────────────

    @AuditableAction(operation = AuditOperation.UPDATE, entity = Agios.class)
    @Transactional
    public AgiosResponseDto updateAgios(Long id, AgiosRequestDto dto) {
        log.info("Mise à jour de l'agios — id: {}", id);

        Agios agios  = findAgiosOuLeverException(id);
        Banque banque = findBanqueOuLeverException(dto.banqueId());

        agios.setLibelle(dto.libelle());
        agios.setMontant(dto.montant());
        agios.setBanque(banque);
        agios.setDateEmission(dto.dateEmission());
        agios.setObservation(dto.observation());

        Agios updated = agiosRepository.save(agios);
        log.info("Agios mis à jour — id: {}", updated.getId());
        return agiosMapper.toResponseDto(updated);
    }

    // ── Suppression ───────────────────────────────────────────────────────────

    @AuditableAction(operation = AuditOperation.DELETE, entity = Agios.class)
    @Transactional
    public void deleteAgios(Long id) {
        log.info("Suppression de l'agios — id: {}", id);
        Agios agios = findAgiosOuLeverException(id);

        if (agios.getStatus() != Status.PREVU) {
            log.warn("Tentative de suppression d'un agios déjà réglé — id: {}", id);
            throw new AgiosBusinessException("L'agios id: " + id + " est déjà réglé et ne peut plus être supprimé.");
        }

        agiosRepository.deleteById(id);
        log.info("Agios supprimé — id: {}", id);
    }

    // ── Toggle statut ─────────────────────────────────────────────────────────

    @AuditableAction(operation = AuditOperation.UPDATE, entity = Agios.class)
    @Transactional
    public AgiosResponseDto toggleStatus(Long id) {
        log.info("Basculement du statut de l'agios — id: {}", id);

        Agios agios     = findAgiosOuLeverException(id);
        Status newStatus = agios.getStatus() == Status.PAYE ? Status.PREVU : Status.PAYE;
        agios.setStatus(newStatus);

        Agios updated = agiosRepository.save(agios);
        log.info("Statut basculé — id: {}, nouveau statut: {}", id, newStatus);
        return agiosMapper.toResponseDto(updated);
    }

    // ── Méthodes privées ──────────────────────────────────────────────────────

    private Agios findAgiosOuLeverException(Long id) {
        return agiosRepository.findById(id)
                .orElseThrow(() -> new AgiosNotFoundException(id));
    }

    private Banque findBanqueOuLeverException(Long banqueId) {
        return banqueService.findOrThrow(banqueId);
    }
}
