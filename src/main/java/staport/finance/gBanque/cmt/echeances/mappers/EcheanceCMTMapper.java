package staport.finance.gBanque.cmt.echeances.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import staport.finance.gBanque.cmt.echeances.dtos.EcheanceCMTResponseDto;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;

/**
 * Mapper MapStruct pour la conversion entre l'entité EcheanceCMT et ses DTOs.
 */
@Mapper(componentModel = "spring")
public interface EcheanceCMTMapper {

    @Mapping(source = "creditMoyenTerme", target = "credit")
    @Mapping(source = "montant", target = "montantTtc")
    @Mapping(source = "dateValeur", target = "dateValuer")
    @Mapping(target = "observation", constant = "Prélèvement CMT")
    @Mapping(target = "libelle", constant = "échéance CMT")
    EcheanceCMTResponseDto toResponseDto(EcheanceCMT entity);

    EcheanceCMTResponseDto.CreditMoyenTermeDto toCreditDto(CreditMoyenTerme credit);
}
