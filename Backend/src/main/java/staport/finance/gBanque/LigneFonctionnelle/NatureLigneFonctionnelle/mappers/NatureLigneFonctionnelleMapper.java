package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleRequest;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleResponse;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;

import java.util.List;

/**
 * Mapper MapStruct pour la conversion entre l'entité NatureLigneFonctionnelle et ses DTOs.
 * Conversion uniquement — aucune logique métier (unicité, immutabilité du code, etc. sont
 * portées par le Service).
 */
@Mapper(componentModel = "spring")
public interface NatureLigneFonctionnelleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    NatureLigneFonctionnelle toEntity(NatureLigneFonctionnelleRequest request);

    NatureLigneFonctionnelleResponse toDto(NatureLigneFonctionnelle entity);

    List<NatureLigneFonctionnelleResponse> toDtoList(List<NatureLigneFonctionnelle> entities);
}
