package staport.finance.gBanque.Agios.service;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Service
public class AgiosService {

    private static final Logger log = LoggerFactory.getLogger(AgiosService.class);

    private final AgiosRepository agiosRepository;
    private final IBanqueService banqueService;
    private final AgiosMapper agiosMapper;

    public AgiosService(AgiosRepository agiosRepository,
                        IBanqueService banqueService,
                        AgiosMapper agiosMapper) {
        this.agiosRepository = agiosRepository;
        this.banqueService   = banqueService;
        this.agiosMapper     = agiosMapper;
    }

    // ── Création ──────────────────────────────────────────────────────────────

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
        findAgiosOuLeverException(id);
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
