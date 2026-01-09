package staport.finance.gBanque.security.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.security.entities.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Get all distinct usernames
     */
    @Query("SELECT DISTINCT u.username FROM User u")
    List<String> findDistinctUsernames();
}
