package staport.finance.gBanque.leasing.config.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import staport.finance.gBanque.leasing.config.dtos.LeasingConfigRequestDTO;
import staport.finance.gBanque.leasing.config.dtos.LeasingConfigResponseDTO;
import staport.finance.gBanque.leasing.config.entities.LeasingConfig;

@Mapper(componentModel = "spring")
public interface LeasingConfigMapper {

    LeasingConfigResponseDTO toDto(LeasingConfig entity);

    @Mapping(target = "id",               ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    void updateEntity(LeasingConfigRequestDTO dto, @MappingTarget LeasingConfig entity);
}
