package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.mappers;


import org.mapstruct.MappingTarget;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleRequest;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleResponse;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import java.util.List;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;

@Component // Mark this as a Spring component for dependency injection
public class NatureLigneFonctionnelleMapper {

    /**
     * Converts a NatureLigneFonctionnelleRequest DTO to a NatureLigneFonctionnelle entity for creation.
     * ID, dateCreation, and dateModification are ignored as they are handled by JPA lifecycle callbacks.
     *
     * @param request The NatureLigneFonctionnelleRequest DTO.
     * @return A new NatureLigneFonctionnelle entity.
     */
    public NatureLigneFonctionnelle toEntity(NatureLigneFonctionnelleRequest request) {
        if (request == null) {
            return null;
        }
        NatureLigneFonctionnelle entity = new NatureLigneFonctionnelle();
        entity.setLabel(request.getLabel());
        entity.setDescription(request.getDescription());
        // ID, dateCreation, and dateModification are managed by JPA @PrePersist/@PreUpdate
        return entity;
    }

    /**
     * Converts a NatureLigneFonctionnelle entity to a NatureLigneFonctionnelleResponse DTO.
     *
     * @param entity The NatureLigneFonctionnelle entity.
     * @return A NatureLigneFonctionnelleResponse DTO.
     */
    public NatureLigneFonctionnelleResponse toDto(NatureLigneFonctionnelle entity) {
        if (entity == null) {
            return null;
        }
        NatureLigneFonctionnelleResponse dto = new NatureLigneFonctionnelleResponse();
        dto.setId(entity.getId());
        dto.setLabel(entity.getLabel());
        dto.setDescription(entity.getDescription());
        dto.setDateCreation(entity.getDateCreation());
        dto.setDateModification(entity.getDateModification());
        return dto;
    }

    /**
     * Converts a list of NatureLigneFonctionnelle entities to a list of NatureLigneFonctionnelleResponse DTOs.
     *
     * @param entities The list of NatureLigneFonctionnelle entities.
     * @return A list of NatureLigneFonctionnelleResponse DTOs.
     */
    public List<NatureLigneFonctionnelleResponse> toDtoList(List<NatureLigneFonctionnelle> entities) {
        if (entities == null) {
            return List.of(); // Return an empty list instead of null for robustness
        }
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Updates an existing NatureLigneFonctionnelle entity with data from a NatureLigneFonctionnelleRequest DTO.
     * Fields like ID, dateCreation, and dateModification are explicitly ignored as they shouldn't be updated via DTO.
     * The dateModification will be handled by the @PreUpdate JPA callback on the entity.
     *
     * @param request The NatureLigneFonctionnelleRequest DTO with updated data.
     * @param entity The existing NatureLigneFonctionnelle entity to be updated.
     */
    public void updateEntityFromDto(NatureLigneFonctionnelleRequest request, @MappingTarget NatureLigneFonctionnelle entity) {
        // Ensure neither request nor entity is null before attempting to update
        if (request == null || entity == null) {
            return;
        }
        // Update only the fields that are expected to be updated from the DTO
        // @NotBlank validation on the request should ensure these are not null if the DTO is valid
        entity.setLabel(request.getLabel());
        entity.setDescription(request.getDescription());
        // ID and dateCreation are not updated.
        // dateModification is updated automatically by JPA's @PreUpdate callback.
    }
}
