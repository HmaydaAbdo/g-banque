package staport.finance.gBanque.security.services;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.security.entities.Role;
import staport.finance.gBanque.security.entities.User;
import staport.finance.gBanque.security.enums.RoleName;
import staport.finance.gBanque.security.repositories.RoleRepository;
import staport.finance.gBanque.security.repositories.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ========================================
    // EXISTING METHODS (KEEP AS IS)
    // ========================================

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

    // ========================================
    // NEW USER MANAGEMENT METHODS
    // ========================================

    /**
     * Creates a new user with the given username and password.
     * The password will be automatically encoded.
     *
     * @param username the username
     * @param password the raw password (will be encoded)
     * @param enabled whether the user is enabled
     * @return the created user
     * @throws IllegalArgumentException if username already exists
     */
    @Transactional
    public User createUser(String username, String password, boolean enabled) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username '" + username + "' already exists.");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(enabled);

        return userRepository.save(user);
    }

    /**
     * Deletes a user by ID.
     *
     * @param userId the user ID
     * @throws IllegalArgumentException if user not found
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + userId + " not found."));

        userRepository.delete(user);
    }

    /**
     * Deletes a user by username.
     *
     * @param username the username
     * @throws IllegalArgumentException if user not found
     */
    @Transactional
    public void deleteUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User '" + username + "' not found."));

        userRepository.delete(user);
    }

    /**
     * Adds a role to a user by role ID.
     *
     * @param userId the user ID
     * @param roleId the role ID
     * @return the updated user
     * @throws IllegalArgumentException if user or role not found, or role already assigned
     */
    @Transactional
    public User addRoleToUser(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + userId + " not found."));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role with ID " + roleId + " not found."));

        if (user.getRoles().contains(role)) {
            throw new IllegalArgumentException("User already has this role.");
        }

        user.getRoles().add(role);
        return userRepository.save(user);
    }

    /**
     * Adds a role to a user by RoleName enum.
     *
     * @param userId the user ID
     * @param roleName the RoleName enum (e.g., RoleName.ADMIN)
     * @return the updated user
     * @throws IllegalArgumentException if user or role not found, or role already assigned
     */
    @Transactional
    public User addRoleToUser(Long userId, RoleName roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + userId + " not found."));

        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role '" + roleName + "' not found."));

        if (user.getRoles().contains(role)) {
            throw new IllegalArgumentException("User already has this role.");
        }

        user.getRoles().add(role);
        return userRepository.save(user);
    }

    /**
     * Removes a role from a user by role ID.
     *
     * @param userId the user ID
     * @param roleId the role ID
     * @return the updated user
     * @throws IllegalArgumentException if user or role not found, or user doesn't have the role
     */
    @Transactional
    public User removeRoleFromUser(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + userId + " not found."));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role with ID " + roleId + " not found."));

        if (!user.getRoles().contains(role)) {
            throw new IllegalArgumentException("User doesn't have this role.");
        }

        user.getRoles().remove(role);
        return userRepository.save(user);
    }

    /**
     * Removes a role from a user by RoleName enum.
     *
     * @param userId the user ID
     * @param roleName the RoleName enum (e.g., RoleName.ADMIN)
     * @return the updated user
     * @throws IllegalArgumentException if user or role not found, or user doesn't have the role
     */
    @Transactional
    public User removeRoleFromUser(Long userId, RoleName roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + userId + " not found."));

        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role '" + roleName + "' not found."));

        if (!user.getRoles().contains(role)) {
            throw new IllegalArgumentException("User doesn't have this role.");
        }

        user.getRoles().remove(role);
        return userRepository.save(user);
    }

    /**
     * Enables or disables a user account.
     *
     * @param userId the user ID
     * @param enabled the enabled status
     * @return the updated user
     * @throws IllegalArgumentException if user not found
     */
    @Transactional
    public User setUserEnabled(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + userId + " not found."));

        user.setEnabled(enabled);
        return userRepository.save(user);
    }

    /**
     * Updates a user's password.
     *
     * @param userId the user ID
     * @param newPassword the new raw password (will be encoded)
     * @return the updated user
     * @throws IllegalArgumentException if user not found
     */
    @Transactional
    public User updatePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + userId + " not found."));

        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    /**
     * Retrieves all users.
     *
     * @return list of all users
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Finds a user by ID.
     *
     * @param userId the user ID
     * @return Optional containing the user if found
     */
    public Optional<User> findUserById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * Finds a user by username.
     *
     * @param username the username
     * @return Optional containing the user if found
     */
    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}