package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleRequest;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleResponse;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NatureLigneFonctionnelleMapper {

    // Request to Entity for creation
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    NatureLigneFonctionnelle toEntity(NatureLigneFonctionnelleRequest request);

    // Entity to Response for reading
    NatureLigneFonctionnelleResponse toDto(NatureLigneFonctionnelle entity);

    List<NatureLigneFonctionnelleResponse> toDtoList(List<NatureLigneFonctionnelle> entities);

    // Request to Entity for update
    @Mapping(target = "id", ignore = true) // ID cannot be updated from DTO
    @Mapping(target = "dateCreation", ignore = true) // Creation date cannot be updated
    @Mapping(target = "dateModification", ignore = true) // Handled by @PreUpdate
    void updateEntityFromDto(NatureLigneFonctionnelleRequest request, @MappingTarget NatureLigneFonctionnelle entity);
}
