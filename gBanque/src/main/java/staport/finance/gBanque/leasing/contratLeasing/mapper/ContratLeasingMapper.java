// src/main/java/staport/finance/gBanque/leasing/contratLeasing/mapper/ContratLeasingMapper.java

package staport.finance.gBanque.leasing.contratLeasing.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingPhaseRequestDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingResponseDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingRequestDTO;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasingPhase;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.entities.OrganismeLeasing;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ContratLeasingMapper {

    private final ContratLeasingPhaseMapper phaseMapper;

    @Autowired
    public ContratLeasingMapper(ContratLeasingPhaseMapper phaseMapper) {
        this.phaseMapper = phaseMapper;
    }

    /**
     * Converts ContratLeasingRequestDTO to ContratLeasing entity.
     */
    public ContratLeasing toEntity(ContratLeasingRequestDTO requestDTO, Banque banque, OrganismeLeasing organismeLeasing) {
        if (requestDTO == null) {
            return null;
        }
        ContratLeasing entity = new ContratLeasing();
        entity.setNumeroContrat(requestDTO.getNumeroContrat());
        entity.setMontantAchatTTC(requestDTO.getMontantAchatTTC());
        entity.setImmobilisation(requestDTO.getImmobilisation());
        entity.setDateDebut(requestDTO.getDateDebut());
        entity.setPeriodeEnMois(requestDTO.getPeriodeEnMois());
        entity.setTypeContrat(requestDTO.getTypeContrat());
        entity.setValeurResiduelle(requestDTO.getValeurResiduelle());

        entity.setBanqueDePrelevement(banque);
        entity.setOrganismeLeasing(organismeLeasing);


        List<ContratLeasingPhase> phases = new ArrayList<>();

        for (ContratLeasingPhaseRequestDTO phaseDto : requestDTO.getPhases()) {
                ContratLeasingPhase phase = phaseMapper.toEntity(phaseDto);
                phase.setContratLeasing(entity);
                phases.add(phase);
        }

        entity.setPhases(phases);

        return entity;
    }

    /**
     * Converts a ContratLeasing entity to a ContratLeasingResponseDTO.
     */
    public ContratLeasingResponseDTO toDto(ContratLeasing entity) {
        if (entity == null) {
            return null;
        }
        ContratLeasingResponseDTO dto = new ContratLeasingResponseDTO();
        dto.setId(entity.getId());
        dto.setNumeroContrat(entity.getNumeroContrat());
        dto.setImmobilisation(entity.getImmobilisation());
        dto.setMontantAchatTTC(entity.getMontantAchatTTC());
        dto.setDateDebut(entity.getDateDebut());
        dto.setPeriodeEnMois(entity.getPeriodeEnMois());
        dto.setDateFin(entity.getDateDebut().plusMonths(entity.getPeriodeEnMois()));
        dto.setTypeContrat(entity.getTypeContrat());
        dto.setValeurResiduelle(entity.getValeurResiduelle());
        dto.setDateCreation(entity.getDateCreation());
        dto.setDateModification(entity.getDateModification());
        dto.setCompleted(entity.isCompleted());
        dto.setHasFichierPdf(entity.hasFichierPdf());


        if (entity.getBanqueDePrelevement() != null) {
            dto.setBanqueDePrelevementId(entity.getBanqueDePrelevement().getId());
            dto.setNomBanqueDePrelevement(entity.getBanqueDePrelevement().getNom());
        }
        if (entity.getOrganismeLeasing() != null) {
            dto.setOrganismeLeasingId(entity.getOrganismeLeasing().getId());
            dto.setNomOrganismeLeasing(entity.getOrganismeLeasing().getNom());
        }

        dto.setPhases(entity.getPhases() != null ? phaseMapper.toDtoList(entity.getPhases()) : new ArrayList<>());

        return dto;
    }



    /**
     * Updates an existing ContratLeasing entity from a ContratLeasingRequestDTO.
     */
    public void updateEntityFromDto(ContratLeasingRequestDTO requestDTO, ContratLeasing existingEntity, Banque banque, OrganismeLeasing organismeLeasing) {
        if (requestDTO == null || existingEntity == null) {
            return;
        }

        if (requestDTO.getNumeroContrat() != null) {
            existingEntity.setNumeroContrat(requestDTO.getNumeroContrat());
        }
        if (requestDTO.getMontantAchatTTC() != null) {
            existingEntity.setMontantAchatTTC(requestDTO.getMontantAchatTTC());
        }
        if (requestDTO.getImmobilisation() != null) {
            existingEntity.setImmobilisation(requestDTO.getImmobilisation());
        }
        if (requestDTO.getDateDebut() != null) {
            existingEntity.setDateDebut(requestDTO.getDateDebut());
        }
        if (requestDTO.getPeriodeEnMois() != null) {
            existingEntity.setPeriodeEnMois(requestDTO.getPeriodeEnMois());
        }
        if (requestDTO.getTypeContrat() != null) {
            existingEntity.setTypeContrat(requestDTO.getTypeContrat());
        }

        existingEntity.setBanqueDePrelevement(banque);
        existingEntity.setOrganismeLeasing(organismeLeasing);

        if (existingEntity.getPhases() != null) {
            existingEntity.getPhases().clear();
        } else {
            existingEntity.setPhases(new ArrayList<>());
        }

        if (requestDTO.getPhases() != null && !requestDTO.getPhases().isEmpty()) {
            for (ContratLeasingPhaseRequestDTO phaseDto : requestDTO.getPhases()) {
                ContratLeasingPhase newPhase = phaseMapper.toEntity(phaseDto);
                newPhase.setContratLeasing(existingEntity);
                existingEntity.getPhases().add(newPhase);
            }
        }
    }
}