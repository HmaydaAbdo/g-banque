
package staport.finance.gBanque.LigneFonctionnelle.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.LigneFonctionnelle.entities.LigneFonctionnelle;
import java.util.Optional;
import java.util.List;

/**
 * Repository interface for LigneFonctionnelle entities.
 * Extends JpaRepository for basic CRUD operations and JpaSpecificationExecutor for advanced querying.
 */
@Repository
public interface LigneFonctionnelleRepository extends JpaRepository<LigneFonctionnelle, Long>, JpaSpecificationExecutor<LigneFonctionnelle> {

    /**
     * Finds an active functional line by bank ID and functional line nature ID.
     *
     * @param banqueId The ID of the bank.
     * @param natureLigneFonctionnelleId The ID of the nature of the functional line.
     * @return An Optional containing the active LigneFonctionnelle if found.
     */
    Optional<LigneFonctionnelle> findByBanqueIdAndNatureLigneFonctionnelleIdAndActifTrue(Long banqueId, Long natureLigneFonctionnelleId);

    /**
     * Finds all functional lines associated with a specific bank ID.
     *
     * @param banqueId The ID of the bank.
     * @return A list of LigneFonctionnelle entities.
     */
    List<LigneFonctionnelle> findByBanqueId(Long banqueId);

    /**
     * Counts the number of functional lines associated with a specific NatureLigneFonctionnelle ID.
     * Used typically for integrity checks before deleting a NatureLigneFonctionnelle.
     *
     * @param natureLigneFonctionnelleId The ID of the nature of the functional line.
     * @return The count of functional lines.
     */
    long countByNatureLigneFonctionnelleId(Long natureLigneFonctionnelleId);

    /**
     * Finds an functional line for a given bank and nature, filtering by its active status.
     *
     * @param banqueId The ID of the bank.
     * @param natureLigneFonctionnelleId The ID of the nature of the functional line.
     * @param actif The active status (true for active, false for inactive).
     * @return An Optional containing the LigneFonctionnelle if found.
     */
    Optional<LigneFonctionnelle> findByBanqueIdAndNatureLigneFonctionnelleIdAndActif(Long banqueId, Long natureLigneFonctionnelleId, boolean actif);

    /**
     * Finds all functional lines for a specific bank and nature, ordered by their start date ascending.
     * Used for historical data retrieval.
     *
     * @param banqueId The ID of the bank.
     * @param natureLigneFonctionnelleId The ID of the nature of the functional line.
     * @return A list of LigneFonctionnelle entities, sorted by dateDepart ascending.
     */
    List<LigneFonctionnelle> findByBanqueIdAndNatureLigneFonctionnelleIdOrderByDateDepartAsc(Long banqueId, Long natureLigneFonctionnelleId);

    /**
     * Finds all active functional lines, ordered by their end date in ascending order.
     * This is crucial for the batch job to process lines efficiently based on their expiration.
     *
     * @return A list of active LigneFonctionnelle entities, sorted by dateFin ascending.
     */
    List<LigneFonctionnelle> findAllByActifTrueOrderByDateFinAsc();
}
