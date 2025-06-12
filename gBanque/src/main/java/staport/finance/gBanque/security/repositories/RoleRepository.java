package staport.finance.gBanque.security.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import staport.finance.gBanque.security.entities.Role;
import staport.finance.gBanque.security.enums.RoleName;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(RoleName roleName);

}