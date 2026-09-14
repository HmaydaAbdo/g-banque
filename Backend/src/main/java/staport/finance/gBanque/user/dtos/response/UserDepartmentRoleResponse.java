package staport.finance.gBanque.user.dtos.response;

/**
 * DTO de sortie pour une appartenance département+rôle — jamais l'entité
 * {@code UserDepartmentRole} brute (qui porte une référence cyclique vers User).
 */
public record UserDepartmentRoleResponse(
        Long id,
        Long departmentId,
        String departmentName,
        String roleInDept
) {}
