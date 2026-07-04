package staport.finance.gBanque.leasing.contratCadre.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.leasing.contratCadre.dtos.ContratCadreRequestDTO;
import staport.finance.gBanque.leasing.contratCadre.dtos.ContratCadreResponseDTO;
import staport.finance.gBanque.leasing.contratCadre.dtos.ContratCadreSearchDTO;
import staport.finance.gBanque.leasing.contratCadre.dtos.ContratCadreUpdateDTO;
import staport.finance.gBanque.leasing.contratCadre.entities.ContratCadre;
import staport.finance.gBanque.leasing.contratCadre.enums.StatutContratCadre;
import staport.finance.gBanque.leasing.contratCadre.mappers.ContratCadreMapper;
import staport.finance.gBanque.leasing.contratCadre.repositories.ContratCadreRepository;
import staport.finance.gBanque.leasing.contratCadre.specifications.ContratCadreSpecification;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.entities.OrganismeLeasing;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.services.interfaces.OrganismeLeasingService;

import java.time.LocalDate;
import java.util.List;

@Service
public class ContratCadreServiceImpl implements IContratCadreService {

    private static final Logger log = LoggerFactory.getLogger(ContratCadreServiceImpl.class);

    private final ContratCadreRepository repository;
    private final ContratCadreMapper mapper;
    private final OrganismeLeasingService organismeLeasingService;

    public ContratCadreServiceImpl(ContratCadreRepository repository,
                                   ContratCadreMapper mapper,
                                   OrganismeLeasingService organismeLeasingService) {
        this.repository             = repository;
        this.mapper                 = mapper;
        this.organismeLeasingService = organismeLeasingService;
    }

    @AuditableAction(operation = AuditOperation.CREATE, entity = ContratCadre.class)
    @Transactional
    @Override
    public ContratCadreResponseDTO create(ContratCadreRequestDTO dto) {
        if (repository.existsByOrganisme_IdAndStatut(dto.organismeId(), StatutContratCadre.ACTIF)) {
            throw new ValidationException(
                    "L'organisme ID " + dto.organismeId() + " possède déjà un contrat cadre ACTIF. " +
                    "Archivez-le avant d'en créer un nouveau.");
        }
        OrganismeLeasing organisme = organismeLeasingService.findOrThrow(dto.organismeId());
        ContratCadre entity = mapper.toEntity(dto);
        entity.setOrganisme(organisme);
        entity.setStatut(StatutContratCadre.ACTIF);
        ContratCadre saved = repository.save(entity);
        log.info("Contrat cadre créé — ID : {}, organisme : {}", saved.getId(), organisme.getNom());
        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ContratCadreResponseDTO> search(ContratCadreSearchDTO criteria, Pageable pageable) {
        Specification<ContratCadre> spec = ContratCadreSpecification.searchByCriteria(criteria);
        return repository.findAll(spec, pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ContratCadreResponseDTO> findByOrganisme(Long organismeId) {
        return repository.findByOrganisme_IdOrderByDateDebutDesc(organismeId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public ContratCadreResponseDTO getById(Long id) {
        return mapper.toDto(findOrThrow(id));
    }

    @AuditableAction(operation = AuditOperation.UPDATE, entity = ContratCadre.class)
    @Transactional
    @Override
    public ContratCadreResponseDTO update(Long id, ContratCadreUpdateDTO dto) {
        ContratCadre entity = findOrThrow(id);
        mapper.updateEntity(dto, entity);
        ContratCadre saved = repository.save(entity);
        log.info("Contrat cadre mis à jour — ID : {}", id);
        return mapper.toDto(saved);
    }

    @AuditableAction(operation = AuditOperation.UPDATE, entity = ContratCadre.class)
    @Transactional
    @Override
    public ContratCadreResponseDTO archiver(Long id) {
        ContratCadre entity = findOrThrow(id);
        if (entity.getStatut() != StatutContratCadre.ACTIF) {
            throw new ValidationException(
                    "Seul un contrat cadre ACTIF peut être archivé. Statut actuel : " + entity.getStatut());
        }
        entity.setStatut(StatutContratCadre.ARCHIVE);
        ContratCadre saved = repository.save(entity);
        log.info("Contrat cadre archivé — ID : {}", id);
        return mapper.toDto(saved);
    }

    @Transactional
    @Override
    public ContratCadre resolveActif(Long organismeId) {
        ContratCadre cadre = repository
                .findByOrganisme_IdAndStatut(organismeId, StatutContratCadre.ACTIF)
                .orElseThrow(() -> {
                    log.warn("Aucun contrat cadre ACTIF pour l'organisme ID : {}", organismeId);
                    return new ValidationException(
                            "Aucun contrat cadre actif pour l'organisme ID " + organismeId +
                            ". Veuillez en créer un avant de signer un contrat leasing.");
                });

        // Expiration lazy : si la date de fin est dépassée, on passe en EXPIRE et on rejette
        if (cadre.getDateFin().isBefore(LocalDate.now())) {
            cadre.setStatut(StatutContratCadre.EXPIRE);
            repository.save(cadre);
            log.warn("Contrat cadre ID {} expiré (dateFin={}) — statut mis à jour en EXPIRE",
                    cadre.getId(), cadre.getDateFin());
            throw new ValidationException(
                    "Le contrat cadre de l'organisme ID " + organismeId +
                    " est expiré depuis le " + cadre.getDateFin() +
                    ". Veuillez en créer un nouveau contrat cadre.");
        }

        return cadre;
    }

    // --- Méthode interne ---
    private ContratCadre findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("ContratCadre non trouvé — ID : {}", id);
                    return new ResourceNotFoundException("Contrat cadre non trouvé avec l'ID : " + id);
                });
    }
}
