package staport.finance.gBanque.user.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * DTO pour l'assignation d'un utilisateur à un département avec un rôle
 * (MEMBER ou DIRECTOR). {@code roleInDept} est optionnel : omis, le service
 * applique MEMBER par défaut (principe du moindre privilège — voir
 * {@code UserService.assignDepartmentRole(userId, departmentId)}).
 */
public record DepartmentRoleRequest(

        @NotNull(message = "departmentId est obligatoire.")
        Long departmentId,

        @Pattern(regexp = "(?i)MEMBER|DIRECTOR", message = "roleInDept doit être MEMBER ou DIRECTOR.")
        String roleInDept

) {}
