package staport.finance.gBanque.leasing.contratLeasing.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingPhaseRequestDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingPhaseResponseDTO;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasingPhase;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ContratLeasingPhaseMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "contratLeasing", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    ContratLeasingPhase toEntity(ContratLeasingPhaseRequestDTO dto);

    ContratLeasingPhaseResponseDTO toDto(ContratLeasingPhase entity);

    List<ContratLeasingPhaseResponseDTO> toDtoList(List<ContratLeasingPhase> entities);
}
