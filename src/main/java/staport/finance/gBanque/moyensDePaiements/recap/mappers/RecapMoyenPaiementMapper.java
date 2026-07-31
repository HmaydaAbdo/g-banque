package staport.finance.gBanque.moyensDePaiements.recap.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import staport.finance.gBanque.moyensDePaiements.recap.dtos.response.RecapMoyenPaiementItem;
import staport.finance.gBanque.moyensDePaiements.recap.entities.RecapMoyenPaiementView;

@Mapper(componentModel = "spring")
public interface RecapMoyenPaiementMapper {

    @Mapping(target = "id", source = "entityId")
    @Mapping(target = "source", source = "sourceType")
    RecapMoyenPaiementItem toDto(RecapMoyenPaiementView view);
}
