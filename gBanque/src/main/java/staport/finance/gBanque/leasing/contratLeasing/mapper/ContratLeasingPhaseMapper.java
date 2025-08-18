package staport.finance.gBanque.leasing.contratLeasing.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingPhaseRequestDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingPhaseResponseDTO;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasingPhase;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ContratLeasingPhaseMapper { // Keep the original class name

    /**
     * Converts a ContratLeasingPhaseRequestDTO to a ContratLeasingPhase entity for creation.
     * ID, contratLeasing, dateCreation, and dateModification are handled externally (JPA callbacks or parent setting).
     *
     * @param dto The ContratLeasingPhaseRequestDTO.
     * @return A new ContratLeasingPhase entity.
     */
    public ContratLeasingPhase toEntity(ContratLeasingPhaseRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        ContratLeasingPhase entity = new ContratLeasingPhase();
        entity.setNumeroPhase(dto.getNumeroPhase());
        entity.setDureeMois(dto.getDureeMois());
        entity.setMensualiteTTC(dto.getMensualiteTTC());
        // ID, contratLeasing, dateCreation, dateModification are handled by parent or JPA
        return entity;
    }

    /**
     * Converts a ContratLeasingPhase entity to a ContratLeasingPhaseResponseDTO.
     *
     * @param entity The ContratLeasingPhase entity.
     * @return A ContratLeasingPhaseResponseDTO.
     */
    public ContratLeasingPhaseResponseDTO toDto(ContratLeasingPhase entity) {
        if (entity == null) {
            return null;
        }
        ContratLeasingPhaseResponseDTO dto = new ContratLeasingPhaseResponseDTO();
        dto.setId(entity.getId());
        dto.setNumeroPhase(entity.getNumeroPhase());
        dto.setDureeMois(entity.getDureeMois());
        dto.setMensualiteTTC(entity.getMensualiteTTC());
        dto.setDateCreation(entity.getDateCreation());
        dto.setDateModification(entity.getDateModification());
        return dto;
    }

    /**
     * Converts a list of ContratLeasingPhase entities to a list of ContratLeasingPhaseResponseDTOs.
     *
     * @param entities The list of ContratLeasingPhase entities.
     * @return A list of ContratLeasingPhaseResponseDTOs.
     */
    public List<ContratLeasingPhaseResponseDTO> toDtoList(List<ContratLeasingPhase> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Updates an existing ContratLeasingPhase entity with data from a ContratLeasingPhaseRequestDTO.
     * ID, contratLeasing, dateCreation, and dateModification are ignored.
     *
     * @param dto The ContratLeasingPhaseRequestDTO with updated data.
     * @param entity The existing ContratLeasingPhase entity to be updated.
     */
    public void updateEntityFromDto(ContratLeasingPhaseRequestDTO dto, ContratLeasingPhase entity) {
        if (dto == null || entity == null) {
            return;
        }
        entity.setNumeroPhase(dto.getNumeroPhase());
        entity.setDureeMois(dto.getDureeMois());
        entity.setMensualiteTTC(dto.getMensualiteTTC());
        // ID, contratLeasing, dateCreation, dateModification are ignored
    }
}