package staport.finance.gBanque.leasing.lesOrganismsLeasing.services;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(OrganismeLeasingServiceImpl.class);
    private final OrganismeLeasingRepository repository;
    private final OrganismeLeasingMapper mapper;


    //constructors
    public OrganismeLeasingServiceImpl(OrganismeLeasingRepository repository, OrganismeLeasingMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    @Override
    public OrganismeLeasingResponseDTO create(OrganismeLeasingRequestDTO dto) {
        log.info("Début de la création d'un organisme de leasing dans la couche service");
        log.debug("Données du DTO à créer : {}", dto);

        OrganismeLeasing entity = mapper.toEntity(dto);
        OrganismeLeasing savedEntity = repository.save(entity);

        log.info("Organisme de leasing créé avec succès dans la base de données avec l'ID : {}", savedEntity.getId());

        return mapper.toDto(savedEntity);
    }

    @Transactional
    @Override
    public OrganismeLeasingResponseDTO update(Long id, OrganismeLeasingRequestDTO dto) {
        log.info("Début de la mise à jour de l'organisme de leasing avec l'ID : {} dans la couche service", id);
        log.debug("Nouvelles données du DTO : {}", dto);

        OrganismeLeasing existing = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("Organisme de leasing non trouvé avec l'ID : {}", id);
                    return new ResourceNotFoundException("OrganismeLeasing non trouvé avec l'ID: " + id);
                });

        log.debug("Organisme de leasing trouvé, application des modifications");
        mapper.updateOrganismeLeasingFromDto(dto, existing);

        OrganismeLeasing updatedEntity = repository.save(existing);

        log.info("Organisme de leasing mis à jour avec succès dans la base de données avec l'ID : {}", id);

        return mapper.toDto(updatedEntity);
    }

    @Override
    public OrganismeLeasingResponseDTO getById(Long id) {
        log.info("Recherche de l'organisme de leasing avec l'ID : {} dans la couche service", id);

        OrganismeLeasing entity = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("Organisme de leasing non trouvé avec l'ID : {}", id);
                    return new ResourceNotFoundException("OrganismeLeasing non trouvé avec l'ID: " + id);
                });

        log.debug("Organisme de leasing trouvé : {}", entity.getNom());

        return mapper.toDto(entity);
    }

    @Transactional
    @Override
    public void delete(Long id) {
        log.info("Début de la suppression de l'organisme de leasing avec l'ID : {} dans la couche service", id);

        if (!repository.existsById(id)) {
            log.error("Organisme de leasing non trouvé avec l'ID : {}", id);
            throw new ResourceNotFoundException("OrganismeLeasing non trouvé avec l'ID: " + id);
        }

        repository.deleteById(id);

        log.info("Organisme de leasing supprimé avec succès de la base de données avec l'ID : {}", id);
    }

    @Override
    public Page<OrganismeLeasingResponseDTO> search(OrganismeLeasingSearchDTO criteria, Pageable pageable) {
        log.info("Début de la recherche d'organismes de leasing dans la couche service");
        log.debug("Critères de recherche : {}, Page : {}, Taille : {}",
                criteria, pageable.getPageNumber(), pageable.getPageSize());

        Specification<OrganismeLeasing> spec = OrganismeLeasingSpecification.withFilters(criteria);
        Page<OrganismeLeasingResponseDTO> results = repository.findAll(spec, pageable).map(mapper::toDto);

        log.info("Recherche terminée. {} résultats trouvés sur {} au total",
                results.getNumberOfElements(), results.getTotalElements());

        return results;
    }
}