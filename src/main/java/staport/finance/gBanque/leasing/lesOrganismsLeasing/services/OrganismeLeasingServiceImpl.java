package staport.finance.gBanque.leasing.lesOrganismsLeasing.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingRequestDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingResponseDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingSearchDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.entities.OrganismeLeasing;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.mappers.OrganismeLeasingMapper;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.repositories.OrganismeLeasingRepository;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.services.interfaces.OrganismeLeasingService;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.specifications.OrganismeLeasingSpecification;

@Service
public class OrganismeLeasingServiceImpl implements OrganismeLeasingService {

    private static final Logger log = LoggerFactory.getLogger(OrganismeLeasingServiceImpl.class);
    private final OrganismeLeasingRepository repository;
    private final OrganismeLeasingMapper mapper;


    //constructors
    public OrganismeLeasingServiceImpl(OrganismeLeasingRepository repository, OrganismeLeasingMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @AuditableAction(operation = AuditOperation.CREATE, entity = OrganismeLeasing.class)
    @Transactional
    @Override
    public OrganismeLeasingResponseDTO create(OrganismeLeasingRequestDTO dto) {
        OrganismeLeasing entity = mapper.toEntity(dto);
        OrganismeLeasing savedEntity = repository.save(entity);
        log.info("Organisme de leasing créé avec l'ID : {}", savedEntity.getId());
        return mapper.toDto(savedEntity);
    }

    @AuditableAction(operation = AuditOperation.UPDATE, entity = OrganismeLeasing.class)
    @Transactional
    @Override
    public OrganismeLeasingResponseDTO update(Long id, OrganismeLeasingRequestDTO dto) {
        OrganismeLeasing existing = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("OrganismeLeasing non trouvé — ID : {}", id);
                    return new ResourceNotFoundException("OrganismeLeasing non trouvé avec l'ID: " + id);
                });
        mapper.updateOrganismeLeasingFromDto(dto, existing);
        OrganismeLeasing updatedEntity = repository.save(existing);
        log.info("Organisme de leasing mis à jour — ID : {}", id);
        return mapper.toDto(updatedEntity);
    }

    @Transactional(readOnly = true)
    @Override
    public OrganismeLeasingResponseDTO getById(Long id) {
        OrganismeLeasing entity = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("OrganismeLeasing non trouvé — ID : {}", id);
                    return new ResourceNotFoundException("OrganismeLeasing non trouvé avec l'ID: " + id);
                });
        return mapper.toDto(entity);
    }

    @AuditableAction(operation = AuditOperation.DELETE, entity = OrganismeLeasing.class)
    @Transactional
    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            log.warn("OrganismeLeasing non trouvé — ID : {}", id);
            throw new ResourceNotFoundException("OrganismeLeasing non trouvé avec l'ID: " + id);
        }
        repository.deleteById(id);
        log.info("Organisme de leasing supprimé — ID : {}", id);
    }

    @Transactional(readOnly = true)
    @Override
    public OrganismeLeasing findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("OrganismeLeasing non trouvé — ID : {}", id);
                    return new ResourceNotFoundException("OrganismeLeasing non trouvé avec l'ID: " + id);
                });
    }

    @Transactional(readOnly = true)
    @Override
    public Page<OrganismeLeasingResponseDTO> search(OrganismeLeasingSearchDTO criteria, Pageable pageable) {
        Specification<OrganismeLeasing> spec = OrganismeLeasingSpecification.withFilters(criteria);
        return repository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"))).map(mapper::toDto);
    }
}