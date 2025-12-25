package staport.finance.gBanque.leasing.contratLeasing.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingPhaseRequestDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingPhaseResponseDTO;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasingPhase;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ContratLeasingPhaseMapper {

    private static final Logger log = LoggerFactory.getLogger(ContratLeasingPhaseMapper.class);

    /**
     * Converts a ContratLeasingPhaseRequestDTO to a ContratLeasingPhase entity for creation.
     * ID, contratLeasing, dateCreation, and dateModification are handled externally (JPA callbacks or parent setting).
     *
     * @param dto The ContratLeasingPhaseRequestDTO.
     * @return A new ContratLeasingPhase entity.
     */
    public ContratLeasingPhase toEntity(ContratLeasingPhaseRequestDTO dto) {
        log.debug("Début de la conversion du DTO vers l'entité ContratLeasingPhase");

        if (dto == null) {
            log.warn("Le DTO de phase reçu pour la conversion est null");
            return null;
        }

        log.debug("Conversion de la phase numéro : {}, Durée : {} mois, Mensualité TTC : {}",
                dto.getNumeroPhase(), dto.getDureeMois(), dto.getMensualiteTTC());

        ContratLeasingPhase entity = new ContratLeasingPhase();
        entity.setNumeroPhase(dto.getNumeroPhase());
        entity.setDureeMois(dto.getDureeMois());
        entity.setMensualiteTTC(dto.getMensualiteTTC());

        log.debug("Conversion du DTO vers l'entité ContratLeasingPhase réussie pour la phase numéro : {}", dto.getNumeroPhase());

        return entity;
    }

    /**
     * Converts a ContratLeasingPhase entity to a ContratLeasingPhaseResponseDTO.
     *
     * @param entity The ContratLeasingPhase entity.
     * @return A ContratLeasingPhaseResponseDTO.
     */
    public ContratLeasingPhaseResponseDTO toDto(ContratLeasingPhase entity) {
        log.debug("Début de la conversion de l'entité ContratLeasingPhase vers le DTO");

        if (entity == null) {
            log.warn("L'entité de phase reçue pour la conversion est null");
            return null;
        }

        log.debug("Conversion de la phase ID : {}, Numéro : {}", entity.getId(), entity.getNumeroPhase());

        ContratLeasingPhaseResponseDTO dto = new ContratLeasingPhaseResponseDTO();
        dto.setId(entity.getId());
        dto.setNumeroPhase(entity.getNumeroPhase());
        dto.setDureeMois(entity.getDureeMois());
        dto.setMensualiteTTC(entity.getMensualiteTTC());
        dto.setDateCreation(entity.getDateCreation());
        dto.setDateModification(entity.getDateModification());

        log.debug("Conversion de l'entité vers le DTO réussie pour la phase ID : {}", entity.getId());

        return dto;
    }

    /**
     * Converts a list of ContratLeasingPhase entities to a list of ContratLeasingPhaseResponseDTOs.
     *
     * @param entities The list of ContratLeasingPhase entities.
     * @return A list of ContratLeasingPhaseResponseDTOs.
     */
    public List<ContratLeasingPhaseResponseDTO> toDtoList(List<ContratLeasingPhase> entities) {
        log.debug("Début de la conversion d'une liste de phases vers une liste de DTOs");

        if (entities == null) {
            log.warn("La liste de phases reçue pour la conversion est null");
            return new ArrayList<>();
        }

        log.debug("Nombre de phases à convertir : {}", entities.size());

        List<ContratLeasingPhaseResponseDTO> dtos = entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        log.debug("Conversion de la liste de phases terminée. Nombre de DTOs générés : {}", dtos.size());

        return dtos;
    }

}