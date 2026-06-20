package staport.finance.gBanque.security.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.security.entities.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * Override de la recherche par spécification (utilisée par {@code GET /users}) avec
     * le même EntityGraph — évite le N+1 sur une page de résultats.
     */
    @EntityGraph(attributePaths = {"departmentRoles", "departmentRoles.department"})
    @Override
    Page<User> findAll(Specification<User> spec, Pageable pageable);

    /**
     * Find user by username, avec departmentRoles + department chargés en une seule requête
     * (JOIN FETCH via EntityGraph). Critique pour {@code UserDetailsServiceImpl} qui parcourt
     * {@code user.getDepartmentRoles()} et {@code udr.getDepartment().getName()} pour
     * construire les authorities à chaque login — sans ça, c'est un N+1 à chaque connexion.
     */
    @EntityGraph(attributePaths = {"departmentRoles", "departmentRoles.department"})
    Optional<User> findByUsername(String username);

    /**
     * Override de {@code findById} avec le même EntityGraph — utilisé par
     * {@code UserController}/{@code UserService} pour exposer l'utilisateur en JSON
     * avec ses départements, sans N+1 lors de la sérialisation.
     */
    @EntityGraph(attributePaths = {"departmentRoles", "departmentRoles.department"})
    @Override
    Optional<User> findById(Long id);

    /**
     * Override de {@code findAll} — même raison : {@code GET /users} sérialise
     * une liste d'utilisateurs avec leurs départements, donc un seul aller-retour
     * en base plutôt qu'un N+1 par utilisateur.
     */
    @EntityGraph(attributePaths = {"departmentRoles", "departmentRoles.department"})
    @Override
    List<User> findAll();

    /**
     * Get all distinct usernames
     */
    @Query("SELECT DISTINCT u.username FROM User u")
    List<String> findDistinctUsernames();
}
