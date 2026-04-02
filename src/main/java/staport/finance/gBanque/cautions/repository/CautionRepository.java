package staport.finance.gBanque.cautions.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import staport.finance.gBanque.cautions.entities.Caution;

import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour l'entité {@link Caution}.
 *
 * <p>Stratégie anti-N+1 :
 * <ul>
 *   <li>{@code findByIdAvecAssociations} utilise le {@code @EntityGraph "Caution.avecAssociations"}
 *       pour charger banque, nature et marche en une seule requête JOIN (usage : getById).</li>
 *   <li>{@code findAll(Specification, Pageable)} avec {@code @EntityGraph} pour la recherche paginée
 *       — évite N requêtes supplémentaires pour chaque association lors du parcours de page.</li>
 * </ul>
 */
public interface CautionRepository extends JpaRepository<Caution, Long>,
                                           JpaSpecificationExecutor<Caution> {

    /**
     * Recherche par ID avec chargement des associations en une seule requête.
     * Utiliser exclusivement pour {@code getById} — pas pour les listes.
     */
    @EntityGraph(value = "Caution.avecAssociations")
    @Query("SELECT c FROM Caution c WHERE c.id = :id")
    Optional<Caution> findByIdAvecAssociations(@Param("id") Long id);

    /**
     * Recherche paginée avec critères dynamiques et chargement des associations.
     * L'EntityGraph garantit zéro requête N+1 lors du mapping vers le DTO.
     */
    @EntityGraph(value = "Caution.avecAssociations")
    Page<Caution> findAll(Specification<Caution> spec, Pageable pageable);

    /**
     * Charge toutes les cautions actives avec leurs associations (banque + nature + marche)
     * en une seule requête JOIN — utilisé exclusivement par la génération de commissions.
     *
     * <p>L'EntityGraph garantit zéro requête N+1 lors du parcours des cautions
     * dans le service de génération.
     */
    @EntityGraph(value = "Caution.avecAssociations")
    @Query("SELECT c FROM Caution c WHERE c.actif = true")
    List<Caution> findActivesAvecBanqueEtNature();

    /**
     * Vérifie l'unicité du numéro de caution — utilisé avant création et modification.
     */
    boolean existsByNumeroCaution(String numeroCaution);

    /**
     * Vérifie l'unicité en excluant la caution courante — utilisé lors de la modification.
     */
    boolean existsByNumeroCautionAndIdNot(String numeroCaution, Long id);
}
