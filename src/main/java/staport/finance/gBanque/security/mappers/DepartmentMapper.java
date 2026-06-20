package staport.finance.gBanque.security.mappers;

import org.mapstruct.Mapper;
import staport.finance.gBanque.security.dtos.DepartmentRequest;
import staport.finance.gBanque.security.dtos.DepartmentResponse;
import staport.finance.gBanque.security.entities.Department;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {

    Department toEntity(DepartmentRequest request);

    DepartmentResponse toDto(Department department);
}
