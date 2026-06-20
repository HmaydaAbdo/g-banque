package staport.finance.gBanque.user.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import staport.finance.gBanque.user.dtos.UserDepartmentRoleResponse;
import staport.finance.gBanque.user.entities.UserDepartmentRole;

@Mapper(componentModel = "spring")
public interface UserDepartmentRoleMapper {

    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "department.name", target = "departmentName")
    UserDepartmentRoleResponse toDto(UserDepartmentRole entity);
}
