package staport.finance.gBanque.chargesFixes.config.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.chargesFixes.config.dtos.request.ChargeFixeConfigRequestDTO;
import staport.finance.gBanque.chargesFixes.config.dtos.response.ChargeFixeConfigResponseDTO;
import staport.finance.gBanque.chargesFixes.config.entities.ChargeFixeConfig;

@Mapper(componentModel = "spring")
public interface ChargeFixeConfigMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "banque", source = "banque")
    ChargeFixeConfig toEntity(ChargeFixeConfigRequestDTO dto, Banque banque);

    @Mapping(target = "banqueId", source = "banque.id")
    @Mapping(target = "banqueNom", source = "banque.nom")
    ChargeFixeConfigResponseDTO toResponseDTO(ChargeFixeConfig entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "banque", ignore = true)
    void updateEntity(ChargeFixeConfigRequestDTO dto, @MappingTarget ChargeFixeConfig entity);
}
