package staport.finance.gBanque.leasing.lesOrganismsLeasing.mappers;


import org.springframework.stereotype.Component;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingRequestDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingResponseDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.entities.OrganismeLeasing;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component // Mark as a Spring component for dependency injection
public class OrganismeLeasingMapper {

    /**
     * Converts an OrganismeLeasingRequestDTO to an OrganismeLeasing entity for creation.
     * ID, dateCreation, and dateModification are handled by JPA lifecycle callbacks and are not mapped here.
     *
     * @param dto The OrganismeLeasingRequestDTO.
     * @return A new OrganismeLeasing entity.
     */
    public OrganismeLeasing toEntity(OrganismeLeasingRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        OrganismeLeasing entity = new OrganismeLeasing();
        entity.setNom(dto.getNom());
        entity.setPlafond(dto.getPlafond());
        entity.setDateDebut(dto.getDateDebut());
        entity.setDateFin(dto.getDateFin());
        // ID, dateCreation, dateModification are handled by JPA @PrePersist
        return entity;
    }

    /**
     * Converts an OrganismeLeasing entity to an OrganismeLeasingResponseDTO.
     *
     * @param entity The OrganismeLeasing entity.
     * @return An OrganismeLeasingResponseDTO.
     */
    public OrganismeLeasingResponseDTO toDto(OrganismeLeasing entity) {
        if (entity == null) {
            return null;
        }
        OrganismeLeasingResponseDTO dto = new OrganismeLeasingResponseDTO();
        dto.setId(entity.getId());
        dto.setNom(entity.getNom());
        dto.setPlafond(entity.getPlafond());
        dto.setDateDebut(entity.getDateDebut());
        dto.setDateFin(entity.getDateFin());
        // dateCreation and dateModification are not part of OrganismeLeasingResponseDTO based on your provided DTO
        return dto;
    }

    /**
     * Converts a list of OrganismeLeasing entities to a list of OrganismeLeasingResponseDTOs.
     *
     * @param entities The list of OrganismeLeasing entities.
     * @return A list of OrganismeLeasingResponseDTOs.
     */
    public List<OrganismeLeasingResponseDTO> toDtoList(List<OrganismeLeasing> entities) {
        if (entities == null) {
            return new ArrayList<>(); // Return empty list for robustness
        }
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Updates an existing OrganismeLeasing entity from an OrganismeLeasingRequestDTO.
     * Ignores ID, creation/modification dates as they are handled by JPA lifecycle callbacks or cannot be updated.
     *
     * @param dto The OrganismeLeasingRequestDTO with updated data.
     * @param entity The existing OrganismeLeasing entity to be updated.
     */
    public void updateOrganismeLeasingFromDto(OrganismeLeasingRequestDTO dto, OrganismeLeasing entity) {
        if (dto == null || entity == null) {
            return;
        }
        // Update only the fields that are allowed to be updated from the DTO
        if (dto.getNom() != null) {
            entity.setNom(dto.getNom());
        }
        if (dto.getPlafond() != null) {
            entity.setPlafond(dto.getPlafond());
        }
        if (dto.getDateDebut() != null) {
            entity.setDateDebut(dto.getDateDebut());
        }
        if (dto.getDateFin() != null) {
            entity.setDateFin(dto.getDateFin());
        }
        // ID, dateCreation are not updated.
        // dateModification is updated automatically by JPA's @PreUpdate callback.
    }
}