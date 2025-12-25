
package staport.finance.gBanque.leasing.contratLeasing.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ContratLeasingMapper.class);
    private final ContratLeasingPhaseMapper phaseMapper;

    @Autowired
    public ContratLeasingMapper(ContratLeasingPhaseMapper phaseMapper) {
        this.phaseMapper = phaseMapper;
    }

    /**
     * Converts ContratLeasingRequestDTO to ContratLeasing entity.
     */
    public ContratLeasing toEntity(ContratLeasingRequestDTO requestDTO, Banque banque, OrganismeLeasing organismeLeasing) {
        log.debug("Début de la conversion du DTO vers l'entité ContratLeasing");

        if (requestDTO == null) {
            log.warn("Le DTO reçu pour la conversion est null");
            return null;
        }

        log.debug("Conversion du contrat numéro : {}, Banque : {}, Organisme : {}",
                requestDTO.getNumeroContrat(),
                banque != null ? banque.getNom() : "null",
                organismeLeasing != null ? organismeLeasing.getNom() : "null");

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

        log.debug("Nombre de phases à convertir : {}", requestDTO.getPhases() != null ? requestDTO.getPhases().size() : 0);

        for (ContratLeasingPhaseRequestDTO phaseDto : requestDTO.getPhases()) {
            ContratLeasingPhase phase = phaseMapper.toEntity(phaseDto);
            phase.setContratLeasing(entity);
            phases.add(phase);
        }

        entity.setPhases(phases);

        log.debug("Conversion du DTO vers l'entité ContratLeasing réussie pour le contrat : {}", requestDTO.getNumeroContrat());

        return entity;
    }

    /**
     * Converts a ContratLeasing entity to a ContratLeasingResponseDTO.
     */
    public ContratLeasingResponseDTO toDto(ContratLeasing entity) {
        log.debug("Début de la conversion de l'entité ContratLeasing vers le DTO");

        if (entity == null) {
            log.warn("L'entité ContratLeasing reçue pour la conversion est null");
            return null;
        }

        log.debug("Conversion du contrat ID : {}, Numéro : {}", entity.getId(), entity.getNumeroContrat());

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
            log.debug("Banque de prélèvement associée : {}", entity.getBanqueDePrelevement().getNom());
        }
        if (entity.getOrganismeLeasing() != null) {
            dto.setOrganismeLeasingId(entity.getOrganismeLeasing().getId());
            dto.setNomOrganismeLeasing(entity.getOrganismeLeasing().getNom());
            log.debug("Organisme de leasing associé : {}", entity.getOrganismeLeasing().getNom());
        }

        dto.setPhases(entity.getPhases() != null ? phaseMapper.toDtoList(entity.getPhases()) : new ArrayList<>());

        log.debug("Conversion de l'entité vers le DTO réussie pour le contrat ID : {}", entity.getId());

        return dto;
    }



    /**
     * Updates an existing ContratLeasing entity from a ContratLeasingRequestDTO.
     */
    public void updateEntityFromDto(ContratLeasingRequestDTO requestDTO, ContratLeasing existingEntity, Banque banque, OrganismeLeasing organismeLeasing) {
        log.debug("Début de la mise à jour de l'entité ContratLeasing à partir du DTO");

        if (requestDTO == null || existingEntity == null) {
            log.warn("Le DTO ou l'entité est null, mise à jour annulée");
            return;
        }

        log.debug("Mise à jour du contrat ID : {} avec les nouvelles données", existingEntity.getId());

        if (requestDTO.getNumeroContrat() != null) {
            log.debug("Mise à jour du numéro de contrat : {} -> {}",
                    existingEntity.getNumeroContrat(), requestDTO.getNumeroContrat());
            existingEntity.setNumeroContrat(requestDTO.getNumeroContrat());
        }
        if (requestDTO.getMontantAchatTTC() != null) {
            log.debug("Mise à jour du montant d'achat TTC : {} -> {}",
                    existingEntity.getMontantAchatTTC(), requestDTO.getMontantAchatTTC());
            existingEntity.setMontantAchatTTC(requestDTO.getMontantAchatTTC());
        }
        if (requestDTO.getImmobilisation() != null) {
            log.debug("Mise à jour de l'immobilisation : {} -> {}",
                    existingEntity.getImmobilisation(), requestDTO.getImmobilisation());
            existingEntity.setImmobilisation(requestDTO.getImmobilisation());
        }
        if (requestDTO.getDateDebut() != null) {
            log.debug("Mise à jour de la date de début : {} -> {}",
                    existingEntity.getDateDebut(), requestDTO.getDateDebut());
            existingEntity.setDateDebut(requestDTO.getDateDebut());
        }
        if (requestDTO.getPeriodeEnMois() != null) {
            log.debug("Mise à jour de la période en mois : {} -> {}",
                    existingEntity.getPeriodeEnMois(), requestDTO.getPeriodeEnMois());
            existingEntity.setPeriodeEnMois(requestDTO.getPeriodeEnMois());
        }
        if (requestDTO.getTypeContrat() != null) {
            log.debug("Mise à jour du type de contrat : {} -> {}",
                    existingEntity.getTypeContrat(), requestDTO.getTypeContrat());
            existingEntity.setTypeContrat(requestDTO.getTypeContrat());
        }

        log.debug("Mise à jour de la banque : {}", banque != null ? banque.getNom() : "null");
        existingEntity.setBanqueDePrelevement(banque);

        log.debug("Mise à jour de l'organisme de leasing : {}", organismeLeasing != null ? organismeLeasing.getNom() : "null");
        existingEntity.setOrganismeLeasing(organismeLeasing);

        if (existingEntity.getPhases() != null) {
            log.debug("Suppression des {} phases existantes", existingEntity.getPhases().size());
            existingEntity.getPhases().clear();
        } else {
            existingEntity.setPhases(new ArrayList<>());
        }

        if (requestDTO.getPhases() != null && !requestDTO.getPhases().isEmpty()) {
            log.debug("Ajout de {} nouvelles phases", requestDTO.getPhases().size());
            for (ContratLeasingPhaseRequestDTO phaseDto : requestDTO.getPhases()) {
                ContratLeasingPhase newPhase = phaseMapper.toEntity(phaseDto);
                newPhase.setContratLeasing(existingEntity);
                existingEntity.getPhases().add(newPhase);
            }
        }

        log.debug("Mise à jour de l'entité ContratLeasing terminée pour le contrat ID : {}", existingEntity.getId());
    }
}