package staport.finance.gBanque.cmt.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.cmt.dtos.CreditMoyenTermeRequestDto;
import staport.finance.gBanque.cmt.dtos.CreditMoyenTermeResponseDto;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;

/**
 * Mapper MapStruct pour la conversion entre l'entité CreditMoyenTerme et ses DTOs.
 * La résolution de la banque est effectuée par le Service ; ce mapper se contente
 * de l'assigner à l'entité (conversion uniquement, aucune logique métier).
 */
@Mapper(componentModel = "spring")
public interface CreditMoyenTermeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "banque", source = "banque")
    @Mapping(target = "echeanceCMTS", ignore = true)
    @Mapping(target = "completed", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    CreditMoyenTerme toEntity(CreditMoyenTermeRequestDto requestDto, Banque banque);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "banque", source = "banque")
    @Mapping(target = "echeanceCMTS", ignore = true)
    @Mapping(target = "completed", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    void updateEntity(CreditMoyenTermeRequestDto requestDto, @MappingTarget CreditMoyenTerme entity, Banque banque);

    CreditMoyenTermeResponseDto toResponseDto(CreditMoyenTerme entity);

    CreditMoyenTermeResponseDto.BanqueDto toBanqueDto(Banque banque);
}
