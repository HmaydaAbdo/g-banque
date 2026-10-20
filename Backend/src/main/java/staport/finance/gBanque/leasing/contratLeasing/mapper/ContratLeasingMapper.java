package staport.finance.gBanque.leasing.contratLeasing.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingPhaseRequestDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingRequestDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingResponseDTO;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasingPhase;

import java.util.ArrayList;

/**
 * Mapper MapStruct pour ContratLeasing.
 * Classe abstraite nécessaire pour injecter ContratLeasingPhaseMapper
 * et gérer la logique custom des phases via @AfterMapping.
 */
@Mapper(componentModel = "spring", uses = {ContratLeasingPhaseMapper.class})
public abstract class ContratLeasingMapper {

    @Autowired
    protected ContratLeasingPhaseMapper phaseMapper;

    // ── toEntity ─────────────────────────────────────────────────────────────

    @Mapping(target = "banqueDePrelevement", source = "banque")
    @Mapping(target = "dateDebut",           source = "requestDTO.dateDebut")
    @Mapping(target = "id",                  ignore = true)
    @Mapping(target = "completed",           ignore = true)
    @Mapping(target = "echeances",           ignore = true)
    @Mapping(target = "phases",              ignore = true)
    @Mapping(target = "contratCadre",        ignore = true)
    @Mapping(target = "avanceTTC",           ignore = true) // montant du moyen de paiement choisi, posé par le service
    @Mapping(target = "dateCreation",        ignore = true)
    @Mapping(target = "dateModification",    ignore = true)
    public abstract ContratLeasing toEntity(ContratLeasingRequestDTO requestDTO, Banque banque);

    // ── toDto ─────────────────────────────────────────────────────────────────

    @Mapping(target = "banqueDePrelevementId",  source = "banqueDePrelevement.id")
    @Mapping(target = "nomBanqueDePrelevement", source = "banqueDePrelevement.nom")
    @Mapping(target = "organismeLeasingId",     source = "contratCadre.organisme.id")
    @Mapping(target = "nomOrganismeLeasing",    source = "contratCadre.organisme.nom")
    @Mapping(target = "contratCadreId",         source = "contratCadre.id")
    @Mapping(target = "plafond",                ignore = true) // plafond en cours : renseigné par le service (ContratLeasingServiceImpl)
    @Mapping(target = "avanceMoyenPaiement",    ignore = true) // renseigné par le service à la consultation d'un contrat
    @Mapping(target = "dateFin",
             expression = "java(entity.getDateDebut().plusMonths(entity.getPeriodeEnMois()))")
    public abstract ContratLeasingResponseDTO toDto(ContratLeasing entity);

    // ── updateEntityFromDto ────────────────────────────────────────────────────

    @Mapping(target = "banqueDePrelevement", source = "banque")
    @Mapping(target = "dateDebut",           source = "requestDTO.dateDebut")
    @Mapping(target = "id",                  ignore = true)
    @Mapping(target = "completed",           ignore = true)
    @Mapping(target = "echeances",           ignore = true)
    @Mapping(target = "phases",              ignore = true)
    @Mapping(target = "contratCadre",        ignore = true)
    @Mapping(target = "avanceTTC",           ignore = true) // montant du moyen de paiement choisi, posé par le service
    @Mapping(target = "dateCreation",        ignore = true)
    @Mapping(target = "dateModification",    ignore = true)
    public abstract void updateEntityFromDto(ContratLeasingRequestDTO requestDTO,
                                             @MappingTarget ContratLeasing existingEntity,
                                             Banque banque);

    // ── @AfterMapping — synchronisation des phases ────────────────────────────

    /**
     * Appelé après toEntity ET updateEntityFromDto.
     * Synchronise la collection de phases :
     *  - vide la collection existante si présente
     *  - mappe et rattache chaque phase du DTO au contrat
     */
    @AfterMapping
    protected void syncPhases(ContratLeasingRequestDTO requestDTO,
                               @MappingTarget ContratLeasing entity) {
        if (entity.getPhases() != null) {
            entity.getPhases().clear();
        } else {
            entity.setPhases(new ArrayList<>());
        }
        if (requestDTO.phases() != null) {
            for (ContratLeasingPhaseRequestDTO phaseDto : requestDTO.phases()) {
                ContratLeasingPhase phase = phaseMapper.toEntity(phaseDto);
                phase.setContratLeasing(entity);
                entity.getPhases().add(phase);
            }
        }
    }
}
