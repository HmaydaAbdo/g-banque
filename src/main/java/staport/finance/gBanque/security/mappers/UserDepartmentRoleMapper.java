package staport.finance.gBanque.security.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import staport.finance.gBanque.security.dtos.UserDepartmentRoleResponse;
import staport.finance.gBanque.security.entities.UserDepartmentRole;

@Mapper(componentModel = "spring")
public interface UserDepartmentRoleMapper {

    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "department.name", target = "departmentName")
    UserDepartmentRoleResponse toDto(UserDepartmentRole entity);
}
