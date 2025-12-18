package staport.finance.gBanque.leasing.lesOrganismsLeasing.mappers;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingRequestDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingResponseDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.entities.OrganismeLeasing;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component // Mark as a Spring component for dependency injection
public class OrganismeLeasingMapper {


    private static final Logger log = LoggerFactory.getLogger(OrganismeLeasingMapper.class);

    /**
     * Converts an OrganismeLeasingRequestDTO to an OrganismeLeasing entity for creation.
     * ID, dateCreation, and dateModification are handled by JPA lifecycle callbacks and are not mapped here.
     *
     * @param dto The OrganismeLeasingRequestDTO.
     * @return A new OrganismeLeasing entity.
     */
    public OrganismeLeasing toEntity(OrganismeLeasingRequestDTO dto) {
        log.debug("Début de la conversion du DTO vers l'entité OrganismeLeasing");

        if (dto == null) {
            log.warn("Le DTO reçu pour la conversion est null");
            return null;
        }

        OrganismeLeasing entity = new OrganismeLeasing();
        entity.setNom(dto.getNom());
        entity.setPlafond(dto.getPlafond());
        entity.setDateDebut(dto.getDateDebut());
        entity.setDateFin(dto.getDateFin());

        log.debug("Conversion du DTO vers l'entité réussie pour l'organisme : {}", dto.getNom());
        return entity;
    }

    /**
     * Converts an OrganismeLeasing entity to an OrganismeLeasingResponseDTO.
     *
     * @param entity The OrganismeLeasing entity.
     * @return An OrganismeLeasingResponseDTO.
     */
    public OrganismeLeasingResponseDTO toDto(OrganismeLeasing entity) {
        log.debug("Début de la conversion de l'entité OrganismeLeasing vers le DTO");

        if (entity == null) {
            log.warn("L'entité reçue pour la conversion est null");
            return null;
        }

        OrganismeLeasingResponseDTO dto = new OrganismeLeasingResponseDTO();
        dto.setId(entity.getId());
        dto.setNom(entity.getNom());
        dto.setPlafond(entity.getPlafond());
        dto.setDateDebut(entity.getDateDebut());
        dto.setDateFin(entity.getDateFin());

        log.debug("Conversion de l'entité vers le DTO réussie pour l'organisme ID : {}", entity.getId());
        return dto;
    }

    /**
     * Converts a list of OrganismeLeasing entities to a list of OrganismeLeasingResponseDTOs.
     *
     * @param entities The list of OrganismeLeasing entities.
     * @return A list of OrganismeLeasingResponseDTOs.
     */
    public List<OrganismeLeasingResponseDTO> toDtoList(List<OrganismeLeasing> entities) {
        log.debug("Début de la conversion d'une liste d'entités OrganismeLeasing vers une liste de DTOs");

        if (entities == null) {
            log.warn("La liste d'entités reçue pour la conversion est null");
            return new ArrayList<>();
        }

        List<OrganismeLeasingResponseDTO> dtos = entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        log.debug("Conversion de la liste terminée. Nombre d'éléments convertis : {}", dtos.size());
        return dtos;
    }

    /**
     * Updates an existing OrganismeLeasing entity from an OrganismeLeasingRequestDTO.
     * Ignores ID, creation/modification dates as they are handled by JPA lifecycle callbacks or cannot be updated.
     *
     * @param dto The OrganismeLeasingRequestDTO with updated data.
     * @param entity The existing OrganismeLeasing entity to be updated.
     */
    public void updateOrganismeLeasingFromDto(OrganismeLeasingRequestDTO dto, OrganismeLeasing entity) {
        log.debug("Début de la mise à jour de l'entité OrganismeLeasing à partir du DTO");

        if (dto == null || entity == null) {
            log.warn("Le DTO ou l'entité est null, mise à jour annulée");
            return;
        }

        log.debug("Mise à jour de l'organisme ID : {} avec les nouvelles données", entity.getId());

        if (dto.getNom() != null) {
            entity.setNom(dto.getNom());
            log.debug("Nom mis à jour : {}", dto.getNom());
        }
        if (dto.getPlafond() != null) {
            entity.setPlafond(dto.getPlafond());
            log.debug("Plafond mis à jour : {}", dto.getPlafond());
        }
        if (dto.getDateDebut() != null) {
            entity.setDateDebut(dto.getDateDebut());
            log.debug("Date début mise à jour : {}", dto.getDateDebut());
        }
        if (dto.getDateFin() != null) {
            entity.setDateFin(dto.getDateFin());
            log.debug("Date fin mise à jour : {}", dto.getDateFin());
        }

        log.debug("Mise à jour de l'entité OrganismeLeasing terminée pour l'ID : {}", entity.getId());
    }
}