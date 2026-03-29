package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleRequest;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleResponse;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleSearchCriteria;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.mappers.NatureLigneFonctionnelleMapper;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories.NatureLigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.specifications.NatureLigneFonctionnelleSpecification;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;

import java.util.List;

/**
 * Business logic implementation for the NatureLigneFonctionnelle module.

 * IMPORTANT — class name is intentionally preserved as {@code NatureLigneFonctionnelleService}
 * (not renamed to *Impl) because the {@code NatureLigneFonctionnelleAuditAspect} targets
 * this class by its fully qualified name. Renaming it would silently break audit logging.

 * The controller injects {@link INatureLigneFonctionnelleService} (DIP).
 */
@Service
@Transactional(readOnly = true)
public class NatureLigneFonctionnelleService implements INatureLigneFonctionnelleService {

    private static final Logger logger = LoggerFactory.getLogger(NatureLigneFonctionnelleService.class);
    public static final String FC_CODE = "FC";

    private final NatureLigneFonctionnelleRepository repository;
    private final NatureLigneFonctionnelleMapper mapper;

    public NatureLigneFonctionnelleService(
            NatureLigneFonctionnelleRepository repository,
            NatureLigneFonctionnelleMapper mapper) {
        this.repository                   = repository;
        this.mapper                       = mapper;
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @Override
    @Transactional

    public NatureLigneFonctionnelleResponse createNatureLigneFonctionnelle(NatureLigneFonctionnelleRequest request) {
        logger.info("Création NatureLigneFonctionnelle — label: {}, code: {}", request.label(), request.code());

        if (repository.existsByLabel(request.label())) {
            throw new DuplicateEntryException("Le label '" + request.label() + "' est déjà utilisé.");
        }

        if (repository.existsByCode(request.code())) {
            throw new DuplicateEntryException("Le code '" + request.code() + "' est déjà utilisé.");
        }

        NatureLigneFonctionnelle saved = repository.save(mapper.toEntity(request));
        return mapper.toDto(saved);
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @Override
    public NatureLigneFonctionnelleResponse getNatureLigneFonctionnelleById(Long id) {
        return mapper.toDto(findOrThrow(id));
    }

    @Override
    public Page<NatureLigneFonctionnelleResponse> getAllNatureLigneFonctionnelles(
            NatureLigneFonctionnelleSearchCriteria criteria, Pageable pageable) {

        Specification<NatureLigneFonctionnelle> spec =
                NatureLigneFonctionnelleSpecification.searchByCriteria(criteria);
        return repository.findAll(spec, pageable).map(mapper::toDto);
    }

    @Override
    public List<NatureLigneFonctionnelleResponse> getAllNatureLigneFonctionnellesList() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    // ── Update ───────────────────────────────────────────────────────────────

    /**
     * Updates label and description only. {@code code} is immutable — the mapper
     * intentionally ignores any code value present in the request.

     * This method is targeted by {@link staport.finance.gBanque.logs.config.NatureLigneFonctionnelleAuditAspect};
     * do not rename it without updating the aspect pointcut.
     */
    @Override
    @Transactional

    public NatureLigneFonctionnelleResponse updateNatureLigneFonctionnelle(Long id, NatureLigneFonctionnelleRequest request) {
        NatureLigneFonctionnelle existing = findOrThrow(id);

        // Business Logic: Check label uniqueness only if the label has actually changed
        if (!existing.getLabel().equals(request.label()) && repository.existsByLabel(request.label())) {
            throw new DuplicateEntryException("Le nouveau label est déjà utilisé par une autre nature.");
        }

        // Explicit updates
        existing.setLabel(request.label());
        existing.setDescription(request.description());

        return mapper.toDto(existing);
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteNatureLigneFonctionnelle(Long id) {
        // If you delete and it's used, DB throws DataIntegrityViolationException.
        // You can catch this in your GlobalExceptionHandler to show a nice message.
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("ID introuvable: " + id);
        }
        repository.deleteById(id);
    }


    public Long findFaciliteDeCaisseNatureId() {
        return repository.findByCode(FC_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("Code système FC introuvable."))
                .getId();
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private NatureLigneFonctionnelle findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nature de ligne fonctionnelle non trouvée avec l'ID : " + id));
    }
}
