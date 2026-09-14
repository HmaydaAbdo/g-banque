package staport.finance.gBanque.user.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import staport.finance.gBanque.user.dtos.request.CreateUserRequest;
import staport.finance.gBanque.user.dtos.response.UserResponse;
import staport.finance.gBanque.user.entities.User;

/**
 * {@code uses = UserDepartmentRoleMapper.class} permet à MapStruct de mapper
 * automatiquement {@code List<UserDepartmentRole>} → {@code List<UserDepartmentRoleResponse>}
 * en réutilisant {@link UserDepartmentRoleMapper#toDto}.
 */
@Mapper(componentModel = "spring", uses = UserDepartmentRoleMapper.class)
public interface UserMapper {

    UserResponse toDto(User user);

    /**
     * Construit l'entité à partir de la requête de création — mappe uniquement
     * {@code username}. Le mot de passe (hachage) et {@code enabled} (défaut si
     * absent) restent gérés explicitement dans {@code UserService.createUser}
     * plutôt que dans le mapper, pour éviter un NPE sur unboxing {@code Boolean}
     * null et pour ne jamais laisser un mot de passe en clair transiter sans
     * passer par le {@code PasswordEncoder}.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "systemAdmin", ignore = true)
    @Mapping(target = "departmentRoles", ignore = true)
    User toEntity(CreateUserRequest request);
}
