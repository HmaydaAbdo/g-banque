package staport.finance.gBanque.user.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.user.entities.UserDepartmentRole;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDepartmentRoleRepository extends JpaRepository<UserDepartmentRole, Long> {

    /**
     * Utilisé par GET /users/{id}/department-roles — department chargé en une requête
     * (JOIN FETCH) pour éviter le N+1 quand la liste est sérialisée en JSON.
     */
    @EntityGraph(attributePaths = {"department"})
    List<UserDepartmentRole> findByUserId(Long userId);

    List<UserDepartmentRole> findByDepartmentId(Long departmentId);

    @EntityGraph(attributePaths = {"department"})
    Optional<UserDepartmentRole> findByUserIdAndDepartmentId(Long userId, Long departmentId);

    boolean existsByDepartmentId(Long departmentId);
}
