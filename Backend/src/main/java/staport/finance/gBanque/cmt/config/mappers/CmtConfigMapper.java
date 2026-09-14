package staport.finance.gBanque.cmt.config.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import staport.finance.gBanque.cmt.config.dtos.CmtConfigRequestDTO;
import staport.finance.gBanque.cmt.config.dtos.CmtConfigResponseDTO;
import staport.finance.gBanque.cmt.config.entities.CmtConfig;

@Mapper(componentModel = "spring")
public interface CmtConfigMapper {

    CmtConfigResponseDTO toDto(CmtConfig entity);

    @Mapping(target = "id",               ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    void updateEntity(CmtConfigRequestDTO dto, @MappingTarget CmtConfig entity);
}
