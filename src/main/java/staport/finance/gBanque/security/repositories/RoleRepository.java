package staport.finance.gBanque.security.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.security.entities.Role;
import staport.finance.gBanque.security.enums.RoleName;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    /**
     * Find role by role name enum
     */
    Optional<Role> findByRoleName(RoleName roleName);
}