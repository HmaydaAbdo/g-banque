package staport.finance.gBanque.Agios.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import staport.finance.gBanque.Agios.dtos.AgiosResponseDto;
import staport.finance.gBanque.Agios.entities.Agios;
import java.math.BigDecimal;

/**
 * Mapper MapStruct pour le module Agios.
 * Conversion uniquement — aucune logique métier.
 */
@Mapper(componentModel = "spring")
public interface AgiosMapper {

    @Mapping(target = "banqueId",  source = "banque.id")
    @Mapping(target = "banqueNom", source = "banque.nom")
    @Mapping(target = "ecart", expression = "java(ecart(entity))")
    @Mapping(target = "natureEcart", expression = "java(natureEcart(entity))")
    AgiosResponseDto toResponseDto(Agios entity);

    default BigDecimal ecart(Agios entity) {
        if (entity.getMontantFacture() == null || entity.getMontantCalcule() == null) return null;
        return entity.getMontantFacture().subtract(entity.getMontantCalcule());
    }

    default String natureEcart(Agios entity) {
        BigDecimal ecart = ecart(entity);
        if (ecart == null) return null;
        int signe = ecart.compareTo(BigDecimal.ZERO);
        return signe > 0 ? "RECLAMATION" : signe < 0 ? "BENEFIQUE" : "AUCUN_ECART";
    }
}
