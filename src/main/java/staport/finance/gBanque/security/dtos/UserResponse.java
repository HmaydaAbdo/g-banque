package staport.finance.gBanque.security.dtos;

import java.util.List;

/**
 * DTO de sortie pour un utilisateur — jamais l'entité {@code User} brute
 * (mot de passe haché, relations JPA lazy non désirées dans l'API).
 */
public record UserResponse(
        Long id,
        String username,
        boolean enabled,
        boolean systemAdmin,
        List<UserDepartmentRoleResponse> departmentRoles
) {}
