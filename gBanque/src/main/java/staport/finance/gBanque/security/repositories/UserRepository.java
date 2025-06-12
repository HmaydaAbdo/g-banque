package staport.finance.gBanque.security.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import staport.finance.gBanque.security.entities.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}