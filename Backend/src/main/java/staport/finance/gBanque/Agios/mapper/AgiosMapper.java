package staport.finance.gBanque.Agios.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import staport.finance.gBanque.Agios.dtos.AgiosResponseDto;
import staport.finance.gBanque.Agios.entities.Agios;

/**
 * Mapper MapStruct pour le module Agios.
 * Conversion uniquement — aucune logique métier.
 */
@Mapper(componentModel = "spring")
public interface AgiosMapper {

    @Mapping(target = "banqueId",  source = "banque.id")
    @Mapping(target = "banqueNom", source = "banque.nom")
    AgiosResponseDto toResponseDto(Agios entity);
}
