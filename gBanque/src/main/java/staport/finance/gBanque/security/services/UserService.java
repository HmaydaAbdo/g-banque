package staport.finance.gBanque.security.services;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import staport.finance.gBanque.security.entities.User;
import staport.finance.gBanque.security.repositories.UserRepository;

import java.util.List;

@Service
public class UserService {


    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Retrieves the full User entity for the currently authenticated principal.
     * This method is crucial for passing the User object (with ID) to the NotificationService.
     *
     * @return The User entity.
     * @throws IllegalStateException if no user is authenticated or the user is not found in the database.
     */
    public User getCurrentUser() {
        // 1. Get the Authentication object from the Security Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("User not authenticated.");
        }

        // The name in the Authentication object is the username
        String username = authentication.getName();

        // 2. Fetch the full User entity from the database using the username
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user '" + username + "' not found in database."));
    }

    public List<String> getAllDistinctUsernames() {
        return userRepository.findDistinctUsernames();
    }
}
