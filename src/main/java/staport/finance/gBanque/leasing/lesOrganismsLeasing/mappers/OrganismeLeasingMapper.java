package staport.finance.gBanque.leasing.lesOrganismsLeasing.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingRequestDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingResponseDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.entities.OrganismeLeasing;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrganismeLeasingMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    OrganismeLeasing toEntity(OrganismeLeasingRequestDTO dto);

    OrganismeLeasingResponseDTO toDto(OrganismeLeasing entity);

    List<OrganismeLeasingResponseDTO> toDtoList(List<OrganismeLeasing> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    void updateOrganismeLeasingFromDto(OrganismeLeasingRequestDTO dto, @MappingTarget OrganismeLeasing entity);
}
