package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleRequest;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleResponse;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleSearchCriteria;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.enums.PorteeNature;

import java.util.List;

/**
 * Service contract for the NatureLigneFonctionnelle module.
 *
 * Controllers must inject this interface (DIP), never the concrete class.
 * The concrete implementation is {@link NatureLigneFonctionnelleService}.
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

    /**
     * Non-paginated list — pour les dropdowns.
     * Si {@code portee} est null, toutes les natures sont retournées.
     * Si {@code portee} est fourni, seules les natures compatibles sont retournées :
     * ex. portee=GENERALE → natures GENERALE + GENERALE_ET_SPECIFIQUE.
     */
    List<NatureLigneFonctionnelleResponse> getAllNatureLigneFonctionnellesList(PorteeNature portee);

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
