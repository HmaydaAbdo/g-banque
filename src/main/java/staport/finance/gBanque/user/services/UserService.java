package staport.finance.gBanque.user.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.user.dtos.CreateUserRequest;
import staport.finance.gBanque.user.dtos.UserDepartmentRoleResponse;
import staport.finance.gBanque.user.dtos.UserResponse;
import staport.finance.gBanque.user.dtos.UserSearchCriteria;
import staport.finance.gBanque.department.entities.Department;
import staport.finance.gBanque.user.entities.User;
import staport.finance.gBanque.user.entities.UserDepartmentRole;
import staport.finance.gBanque.user.enums.DepartmentRoleType;
import staport.finance.gBanque.user.mappers.UserDepartmentRoleMapper;
import staport.finance.gBanque.user.mappers.UserMapper;
import staport.finance.gBanque.department.services.DepartmentService;
import staport.finance.gBanque.user.repositories.UserDepartmentRoleRepository;
import staport.finance.gBanque.user.repositories.UserRepository;
import staport.finance.gBanque.user.specifications.UserSpecification;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DepartmentService departmentService;
    private final UserDepartmentRoleRepository userDepartmentRoleRepository;
    private final UserMapper userMapper;
    private final UserDepartmentRoleMapper userDepartmentRoleMapper;

    public UserService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        DepartmentService departmentService,
                        UserDepartmentRoleRepository userDepartmentRoleRepository,
                        UserMapper userMapper,
                        UserDepartmentRoleMapper userDepartmentRoleMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.departmentService = departmentService;
        this.userDepartmentRoleRepository = userDepartmentRoleRepository;
        this.userMapper = userMapper;
        this.userDepartmentRoleMapper = userDepartmentRoleMapper;
    }

    // ========================================
    // EXISTING METHODS (KEEP AS IS) — usage interne cross-module, entité requise
    // ========================================

    /**
     * Retrieves the full User entity for the currently authenticated principal.
     * Utilisé par {@code NotificationController} — usage interne backend, jamais
     * sérialisé directement en JSON, donc le retour entité est légitime ici.
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("User not authenticated.");
        }

        String username = authentication.getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user '" + username + "' not found in database."));
    }

    public List<String> getAllDistinctUsernames() {
        return userRepository.findDistinctUsernames();
    }

    // ========================================
    // USER MANAGEMENT — toujours DTO en entrée/sortie côté API
    // ========================================

    /**
     * Crée un utilisateur. {@code request.enabled()} nullable → {@code true} par défaut.
     *
     * @throws DuplicateEntryException si le nom d'utilisateur existe déjà
     */
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = User.class)
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new DuplicateEntryException("Le nom d'utilisateur '" + request.username() + "' existe déjà.");
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(request.enabled() == null || request.enabled());

        return userMapper.toDto(userRepository.save(user));
    }

    /**
     * @throws ResourceNotFoundException si l'utilisateur n'existe pas
     */
    public UserResponse getUserById(Long userId) {
        return userMapper.toDto(findEntityById(userId));
    }

    /**
     * Recherche paginée des utilisateurs — {@link UserSearchCriteria} : tous les
     * champs optionnels (username LIKE, enabled, systemAdmin, departmentId).
     */
    public Page<UserResponse> searchUsers(UserSearchCriteria criteria, Pageable pageable) {
        return userRepository.findAll(UserSpecification.searchByCriteria(criteria), pageable)
                .map(userMapper::toDto);
    }

    /**
     * @throws ResourceNotFoundException si l'utilisateur n'existe pas
     */
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = User.class)
    public void deleteUser(Long userId) {
        userRepository.delete(findEntityById(userId));
    }

    /**
     * @throws ResourceNotFoundException si l'utilisateur n'existe pas
     */
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = User.class)
    public UserResponse setUserEnabled(Long userId, boolean enabled) {
        User user = findEntityById(userId);
        user.setEnabled(enabled);
        return userMapper.toDto(userRepository.save(user));
    }

    /**
     * @throws ResourceNotFoundException si l'utilisateur n'existe pas
     */
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = User.class)
    public void updatePassword(Long userId, String newPassword) {
        User user = findEntityById(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ========================================
    // RBAC — GESTION DÉPARTEMENT / RÔLE (modèle unique)
    // ========================================

    /**
     * Liste les appartenances département+rôle d'un utilisateur.
     *
     * @throws ResourceNotFoundException si l'utilisateur n'existe pas
     */
    public List<UserDepartmentRoleResponse> getDepartmentRoles(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Utilisateur introuvable avec l'ID " + userId);
        }
        return userDepartmentRoleRepository.findByUserId(userId).stream()
                .map(userDepartmentRoleMapper::toDto)
                .toList();
    }

    /**
     * Assigne un utilisateur à un département avec le rôle par défaut {@link DepartmentRoleType#MEMBER}.
     *
     * <p>MEMBER est le seul défaut acceptable (principe du moindre privilège) :
     * DIRECTOR donne accès aux actions sensibles (clôture, annulation, suppression)
     * et doit toujours résulter d'un choix explicite de l'administrateur — jamais
     * d'une valeur implicite. Pour assigner DIRECTOR, utiliser la surcharge à 3 arguments.
     *
     * @throws ResourceNotFoundException si l'utilisateur ou le département n'existe pas
     */
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = UserDepartmentRole.class)
    public UserDepartmentRoleResponse assignDepartmentRole(Long userId, Long departmentId) {
        return assignDepartmentRole(userId, departmentId, DepartmentRoleType.MEMBER);
    }

    /**
     * Assigne un rôle (MEMBER ou DIRECTOR) à un utilisateur pour un département donné.
     *
     * <p>Comportement "upsert" : si l'utilisateur a déjà un rôle dans ce département,
     * il est simplement remplacé par le nouveau plutôt que de lever une erreur de doublon —
     * c'est le comportement attendu par l'écran d'administration (changer le rôle d'un
     * utilisateur dans un département via un seul select, sans retirer puis réassigner).
     *
     * @throws ResourceNotFoundException si l'utilisateur ou le département n'existe pas
     */
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = UserDepartmentRole.class)
    public UserDepartmentRoleResponse assignDepartmentRole(Long userId, Long departmentId, DepartmentRoleType roleInDept) {
        User user = findEntityById(userId);
        Department department = departmentService.getDepartmentEntityById(departmentId);

        UserDepartmentRole existing = userDepartmentRoleRepository
                .findByUserIdAndDepartmentId(userId, departmentId)
                .orElse(null);

        UserDepartmentRole saved;
        if (existing != null) {
            existing.setRoleInDept(roleInDept);
            saved = userDepartmentRoleRepository.save(existing);
        } else {
            saved = userDepartmentRoleRepository.save(new UserDepartmentRole(user, department, roleInDept));
        }
        return userDepartmentRoleMapper.toDto(saved);
    }

    /**
     * Retire un utilisateur d'un département.
     *
     * @return l'ID de la ligne {@code UserDepartmentRole} supprimée — nécessaire à
     * {@code GenericAuditAspect} pour tracer l'audit (le PK n'est pas connu de l'appelant,
     * qui ne fournit que {@code userId}/{@code departmentId}).
     * @throws ResourceNotFoundException si l'utilisateur n'a pas de rôle dans ce département
     */
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = UserDepartmentRole.class)
    public Long removeDepartmentRole(Long userId, Long departmentId) {
        UserDepartmentRole existing = userDepartmentRoleRepository
                .findByUserIdAndDepartmentId(userId, departmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cet utilisateur n'a aucun rôle assigné dans ce département."));
        userDepartmentRoleRepository.delete(existing);
        return existing.getId();
    }

    /**
     * Définit le statut administrateur système d'un utilisateur.
     * Un SYSTEM_ADMIN n'a pas besoin d'appartenir à un département : son accès
     * est transversal (voir authorities générées dans UserDetailsServiceImpl).
     *
     * @throws ResourceNotFoundException si l'utilisateur n'existe pas
     */
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = User.class)
    public UserResponse setSystemAdmin(Long userId, boolean isSystemAdmin) {
        User user = findEntityById(userId);
        user.setSystemAdmin(isSystemAdmin);
        return userMapper.toDto(userRepository.save(user));
    }

    /**
     * Lookup interne — entité brute, jamais exposée hors de ce service.
     */
    private User findEntityById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'ID " + userId));
    }
}
