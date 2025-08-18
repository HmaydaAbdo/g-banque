package staport.finance.gBanque.leasing.lesOrganismsLeasing.services;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
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

    private final OrganismeLeasingRepository repository;
    private final OrganismeLeasingMapper mapper; // CHANGE: Use your new manual mapper

    //constructors
    public OrganismeLeasingServiceImpl(OrganismeLeasingRepository repository, OrganismeLeasingMapper mapper) { // CHANGE: Update constructor parameter
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    @Override
    public OrganismeLeasingResponseDTO create(OrganismeLeasingRequestDTO dto) {
        OrganismeLeasing entity = mapper.toEntity(dto);
        //  dateCreation and dateModification are handled by @PrePersist
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    @Override
    public OrganismeLeasingResponseDTO update(Long id, OrganismeLeasingRequestDTO dto) {
        OrganismeLeasing existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OrganismeLeasing non trouvé avec l'ID: " + id));

        // Apply DTO changes to the existing, managed entity
        mapper.updateOrganismeLeasingFromDto(dto, existing);

        // dateModification is handled by @PreUpdate in the OrganismeLeasing entity,
        // so no need to set it manually here.

        return mapper.toDto(repository.save(existing));
    }

    @Override
    public OrganismeLeasingResponseDTO getById(Long id) {
        OrganismeLeasing entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OrganismeLeasing non trouvé avec l'ID: " + id));
        return mapper.toDto(entity);
    }

    @Transactional
    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("OrganismeLeasing non trouvé avec l'ID: " + id);
        }
        repository.deleteById(id);
    }

    @Override
    public Page<OrganismeLeasingResponseDTO> search(OrganismeLeasingSearchDTO criteria, Pageable pageable) {
        Specification<OrganismeLeasing> spec = OrganismeLeasingSpecification.withFilters(criteria);
        return repository.findAll(spec, pageable).map(mapper::toDto);
    }
}