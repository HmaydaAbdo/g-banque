package staport.finance.gBanque.fraisBancaires.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireRequestDto;
import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireResponseDto;
import staport.finance.gBanque.fraisBancaires.entities.FraisBancaire;

@Mapper(componentModel = "spring")
public interface FraisBancaireMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "dateValeur", ignore = true)
    @Mapping(target = "dateOperation", ignore = true)
    @Mapping(target = "banque", source = "banque")
    FraisBancaire toEntity(FraisBancaireRequestDto dto, Banque banque);

    @Mapping(target = "banqueId", source = "banque.id")
    @Mapping(target = "banqueNom", source = "banque.nom")
    FraisBancaireResponseDto toResponseDto(FraisBancaire entity);
}
