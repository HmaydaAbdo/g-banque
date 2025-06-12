package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleSearchCriteria;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.specifications.NatureLigneFonctionnelleSpecification;
import staport.finance.gBanque.LigneFonctionnelle.repositories.LigneFonctionnelleRepository;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleRequest;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleResponse;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.mappers.NatureLigneFonctionnelleMapper;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories.NatureLigneFonctionnelleRepository;
import java.util.List;

@Service
public class NatureLigneFonctionnelleService {

    private final NatureLigneFonctionnelleRepository repository;
    private final NatureLigneFonctionnelleMapper mapper;
    private final LigneFonctionnelleRepository ligneFonctionnelleRepository;

    @Autowired
    public NatureLigneFonctionnelleService(
            NatureLigneFonctionnelleRepository repository,
            NatureLigneFonctionnelleMapper mapper,
            LigneFonctionnelleRepository ligneFonctionnelleRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.ligneFonctionnelleRepository = ligneFonctionnelleRepository;
    }

    @Transactional
    public NatureLigneFonctionnelleResponse createNatureLigneFonctionnelle(NatureLigneFonctionnelleRequest request) {

        if (repository.findByLabel(request.getLabel()).isPresent()) {
            throw new DuplicateEntryException("Une nature de ligne fonctionnelle avec le label '" + request.getLabel() + "' existe déjà.");
        }
        NatureLigneFonctionnelle entity = mapper.toEntity(request);
        NatureLigneFonctionnelle savedEntity = repository.save(entity);
        return mapper.toDto(savedEntity);
    }

    @Transactional(readOnly = true)
    public NatureLigneFonctionnelleResponse getNatureLigneFonctionnelleById(Long id) {
        NatureLigneFonctionnelle entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nature de ligne fonctionnelle non trouvée avec l'ID: " + id));
        return mapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public Page<NatureLigneFonctionnelleResponse> getAllNatureLigneFonctionnelles(NatureLigneFonctionnelleSearchCriteria criteria, Pageable pageable) {
        Specification<NatureLigneFonctionnelle> spec = NatureLigneFonctionnelleSpecification.searchByCriteria(criteria);
        Page<NatureLigneFonctionnelle> entitiesPage = repository.findAll(spec, pageable);
        return entitiesPage.map(mapper::toDto);
    }

    // This method can be kept if you need a non-paginated list for dropdowns etc.
    @Transactional(readOnly = true)
    public List<NatureLigneFonctionnelleResponse> getAllNatureLigneFonctionnellesList() {
        List<NatureLigneFonctionnelle> entities = repository.findAll();
        return mapper.toDtoList(entities);
    }

    @Transactional
    public NatureLigneFonctionnelleResponse updateNatureLigneFonctionnelle(Long id, NatureLigneFonctionnelleRequest request) {
        NatureLigneFonctionnelle existingEntity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nature de ligne fonctionnelle non trouvée avec l'ID: " + id));

        // Check for duplicate label, excluding the current entity being updated
        if (repository.findByLabel(request.getLabel())
                .filter(nlf -> !nlf.getId().equals(id))
                .isPresent()) {
            throw new DuplicateEntryException("Une autre nature de ligne fonctionnelle avec le label '" + request.getLabel() + "' existe déjà.");
        }

        mapper.updateEntityFromDto(request, existingEntity);
        NatureLigneFonctionnelle updatedEntity = repository.save(existingEntity);
        return mapper.toDto(updatedEntity);
    }

    @Transactional
    public void deleteNatureLigneFonctionnelle(Long id) {
        // Check if the NatureLigneFonctionnelle exists
        NatureLigneFonctionnelle natureToDelete = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nature de ligne fonctionnelle non trouvée avec l'ID: " + id));

        //  Check if this NatureLigneFonctionnelle is associated with any LigneFonctionnelle entities
        if (ligneFonctionnelleRepository.countByNatureLigneFonctionnelleId(id) > 0) {
            throw new DuplicateEntryException("Impossible de supprimer la nature de ligne fonctionnelle car elle est associée à une ou plusieurs lignes fonctionnelles existantes.");
        }

        repository.delete(natureToDelete);
    }

    // Helper method to fetch entity by ID (useful for other services)
    @Transactional(readOnly = true)
    public NatureLigneFonctionnelle getNatureLigneFonctionnelleEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nature de ligne fonctionnelle non trouvée avec l'ID: " + id));
    }
}