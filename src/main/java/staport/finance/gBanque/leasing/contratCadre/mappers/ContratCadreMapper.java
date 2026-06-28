package staport.finance.gBanque.leasing.contratCadre.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import staport.finance.gBanque.leasing.contratCadre.dtos.ContratCadreRequestDTO;
import staport.finance.gBanque.leasing.contratCadre.dtos.ContratCadreResponseDTO;
import staport.finance.gBanque.leasing.contratCadre.entities.ContratCadre;

@Mapper(componentModel = "spring")
public interface ContratCadreMapper {

    @Mapping(target = "id",               ignore = true)
    @Mapping(target = "organisme",        ignore = true)
    @Mapping(target = "statut",           ignore = true)
    @Mapping(target = "dateCreation",     ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    ContratCadre toEntity(ContratCadreRequestDTO dto);

    @Mapping(source = "organisme.id",  target = "organismeId")
    @Mapping(source = "organisme.nom", target = "organismeNom")
    ContratCadreResponseDTO toDto(ContratCadre entity);

    @Mapping(target = "id",               ignore = true)
    @Mapping(target = "organisme",        ignore = true)
    @Mapping(target = "statut",           ignore = true)
    @Mapping(target = "dateCreation",     ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    void updateEntity(ContratCadreRequestDTO dto, @MappingTarget ContratCadre entity);
}
