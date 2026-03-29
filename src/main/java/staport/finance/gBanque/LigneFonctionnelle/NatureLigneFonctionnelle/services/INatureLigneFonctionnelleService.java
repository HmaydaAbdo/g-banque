package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleRequest;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleResponse;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleSearchCriteria;

import java.util.List;

/**
 * Service contract for the NatureLigneFonctionnelle module.

 * Controllers must inject this interface (DIP), never the concrete class.
 * The concrete implementation is {@link NatureLigneFonctionnelleService}.

 * Note: the concrete class name is intentionally preserved so that the
 * {@code NatureLigneFonctionnelleAuditAspect} pointcut (which targets the FQCN)
 * continues to fire on {@code updateNatureLigneFonctionnelle}.
 */
public interface INatureLigneFonctionnelleService {

    /**
     * Creates a new nature. Both {@code label} and {@code code} must be unique.
     *
     * @throws staport.finance.gBanque.exception.customExceptions.DuplicateEntryException
     *         if label or code already exists
     */
    NatureLigneFonctionnelleResponse createNatureLigneFonctionnelle(NatureLigneFonctionnelleRequest request);

    NatureLigneFonctionnelleResponse getNatureLigneFonctionnelleById(Long id);

    Page<NatureLigneFonctionnelleResponse> getAllNatureLigneFonctionnelles(
            NatureLigneFonctionnelleSearchCriteria criteria, Pageable pageable);

    /** Non-paginated list — intended for dropdowns / select inputs. */
    List<NatureLigneFonctionnelleResponse> getAllNatureLigneFonctionnellesList();

    /**
     * Updates label and description. {@code code} is immutable and is ignored.
     *
     * @throws staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException if not found
     * @throws staport.finance.gBanque.exception.customExceptions.DuplicateEntryException   if new label conflicts
     */
    NatureLigneFonctionnelleResponse updateNatureLigneFonctionnelle(Long id, NatureLigneFonctionnelleRequest request);

    /**
     * Deletes a nature only if no {@code LigneFonctionnelle} references it.
     *
     * @throws staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException if not found
     * @throws staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.exceptions.NatureLFDeletionException
     *         if referenced by existing lignes fonctionnelles
     */
    void deleteNatureLigneFonctionnelle(Long id);


     Long findFaciliteDeCaisseNatureId();
}
