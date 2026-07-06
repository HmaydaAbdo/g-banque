package staport.finance.gBanque.LigneFonctionnelle.config.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import staport.finance.gBanque.LigneFonctionnelle.config.dtos.LigneFonctionnelleConfigRequestDTO;
import staport.finance.gBanque.LigneFonctionnelle.config.dtos.LigneFonctionnelleConfigResponseDTO;
import staport.finance.gBanque.LigneFonctionnelle.config.entities.LigneFonctionnelleConfig;

@Mapper(componentModel = "spring")
public interface LigneFonctionnelleConfigMapper {

    LigneFonctionnelleConfigResponseDTO toDto(LigneFonctionnelleConfig entity);

    @Mapping(target = "id",               ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    void updateEntity(LigneFonctionnelleConfigRequestDTO dto, @MappingTarget LigneFonctionnelleConfig entity);
}
