package staport.finance.gBanque.LigneFonctionnelle.generalLignes.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
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
     * Surcharge de {@code JpaSpecificationExecutor.findAll(Specification, Sort)} avec fetch
     * anticipé des relations affichées à l'export PDF (banque, nature, marchés) — évite le
     * N+1/LazyInitializationException hors transaction. Utilisée uniquement par l'export PDF
     * (non paginé, contrairement à {@code findAll(Specification, Pageable)} utilisé par les écrans).
     */
    @EntityGraph(attributePaths = {"banque", "natureLigneFonctionnelle", "marches"})
    List<LigneFonctionnelle> findAll(Specification<LigneFonctionnelle> spec, Sort sort);

    /**
     * Finds an active functional line by bank ID and functional line nature ID.
     *
     * @param banqueId The ID of the bank.
     * @param natureLigneFonctionnelleId The ID of the nature of the functional line.
     * @return An Optional containing the active LigneFonctionnelle if found.
     */
    Optional<LigneFonctionnelle> findByBanqueIdAndNatureLigneFonctionnelleIdAndActifTrue(Long banqueId, Long natureLigneFonctionnelleId);

    /**
     * Idem, mais résolue via le code système (immuable) de la nature plutôt que son ID technique.
     * Préféré pour toute association métier automatique (ex. Lcn ↔ Escompte Commercial) — voir
     * {@code NatureLigneFonctionnelleService}.
     */
    Optional<LigneFonctionnelle> findByBanqueIdAndNatureLigneFonctionnelleCodeAndActifTrue(Long banqueId, String natureLigneFonctionnelleCode);


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
     * Ligne générale active pour une banque et une nature : aucun marché associé (marches IS EMPTY).
     * PESSIMISTIC_WRITE garantit un SELECT ... FOR UPDATE — empêche deux transactions concurrentes
     * de lire simultanément "pas de doublon" et de créer deux lignes identiques.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM LigneFonctionnelle l WHERE l.banque.id = :banqueId AND l.natureLigneFonctionnelle.id = :natureId AND l.marches IS EMPTY AND l.actif = true")
    Optional<LigneFonctionnelle> findActiveGeneralLine(@Param("banqueId") Long banqueId, @Param("natureId") Long natureId);

    /**
     * Ligne active (générale ou non) qui inclut le marché donné dans sa collection de marchés.
     * PESSIMISTIC_WRITE : verrou exclusif sur les lignes candidates pour éviter les doublons concurrents.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM LigneFonctionnelle l JOIN l.marches m WHERE m.id = :marcheId AND l.natureLigneFonctionnelle.id = :natureId AND l.actif = true")
    Optional<LigneFonctionnelle> findActiveLineForMarche(@Param("marcheId") Long marcheId, @Param("natureId") Long natureId);





    @Query("""
    SELECT lf.banque.id, lf.plafond
    FROM LigneFonctionnelle lf
    WHERE lf.banque.id IN :ids
    AND lf.natureLigneFonctionnelle.code = :code
    AND lf.actif = true
""")
    List<Object[]> findActivePlafondsByBankIds(@Param("ids") List<Long> ids, @Param("code") String code);

    /** Vérifie l'existence d'au moins une ligne fonctionnelle active pour une banque donnée. */
    boolean existsByBanqueIdAndActifTrue(Long banqueId);

    /**
     * Vérifie l'existence d'au moins une ligne fonctionnelle générale référençant cette nature.
     * Utilisé comme guard de suppression par {@code NatureLigneFonctionnelleService}.
     */
    boolean existsByNatureLigneFonctionnelleId(Long natureLigneFonctionnelleId);
}