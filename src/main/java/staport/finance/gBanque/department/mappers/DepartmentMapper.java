package staport.finance.gBanque.department.mappers;

import org.mapstruct.Mapper;
import staport.finance.gBanque.department.dtos.request.DepartmentRequest;
import staport.finance.gBanque.department.dtos.response.DepartmentResponse;
import staport.finance.gBanque.department.entities.Department;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {

    Department toEntity(DepartmentRequest request);

    DepartmentResponse toDto(Department department);
}
