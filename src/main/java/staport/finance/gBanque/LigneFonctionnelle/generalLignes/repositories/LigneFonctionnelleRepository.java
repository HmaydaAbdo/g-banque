package staport.finance.gBanque.LigneFonctionnelle.generalLignes.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities.LigneFonctionnelle;
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
     * Finds all functional lines for a specific bank and nature, ordered by their start date ascending.
     * Used for historical data retrieval.
     *
     * @param banqueId The ID of the bank.
     * @param natureLigneFonctionnelleId The ID of the nature of the functional line.
     * @return A list of LigneFonctionnelle entities, sorted by dateDepart ascending.
     */
    List<LigneFonctionnelle> findByBanqueIdAndNatureLigneFonctionnelleIdOrderByDateDepartAsc(Long banqueId, Long natureLigneFonctionnelleId);


    Optional<LigneFonctionnelle> findByBanqueIdAndNatureLigneFonctionnelleIdAndMarcheIsNullAndActifTrue(
            Long banqueId,
            Long natureLigneFonctionnelleId
    );

    // Pour vérifier les doublons de lignes spécifiques à un marché
    Optional<LigneFonctionnelle> findByMarcheIdAndNatureLigneFonctionnelleIdAndActifTrue(
            Long marcheId,
            Long natureLigneFonctionnelleId
    );





    @Query("""
    SELECT lf.banque.id, lf.plafond
    FROM LigneFonctionnelle lf
    WHERE lf.banque.id IN :ids
    AND lf.natureLigneFonctionnelle.code = :code
    AND lf.actif = true
""")
    List<Object[]> findActivePlafondsByBankIds(@Param("ids") List<Long> ids, @Param("code") String code);
}