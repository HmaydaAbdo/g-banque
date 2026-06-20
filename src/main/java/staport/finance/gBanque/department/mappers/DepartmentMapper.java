package staport.finance.gBanque.department.mappers;

import org.mapstruct.Mapper;
import staport.finance.gBanque.department.dtos.DepartmentRequest;
import staport.finance.gBanque.department.dtos.DepartmentResponse;
import staport.finance.gBanque.department.entities.Department;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {

    Department toEntity(DepartmentRequest request);

    DepartmentResponse toDto(Department department);
}
