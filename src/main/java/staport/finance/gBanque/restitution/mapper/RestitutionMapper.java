package staport.finance.gBanque.restitution.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.restitution.dtos.RestitutionRequestDto;
import staport.finance.gBanque.restitution.dtos.RestitutionResponseDto;
import staport.finance.gBanque.restitution.entities.Restitution;

@Component
public class RestitutionMapper {

    private static final Logger log = LoggerFactory.getLogger(RestitutionMapper.class);

    /**
     * Convertit un DTO de requête en entité Restitution
     */
    public Restitution toEntity(RestitutionRequestDto dto, Banque banque) {

        if (dto == null) {
            log.warn("Tentative de conversion d'un RestitutionRequestDto null en entité");
            return null;
        }

        if (banque == null) {
            log.error("Tentative de création d'une restitution avec une banque null pour le libellé: '{}'",
                    dto.getLibelle());
            throw new IllegalArgumentException("La banque ne peut pas être null lors de la création d'une restitution");
        }

        log.debug(
                "Conversion du DTO vers entité Restitution - Libellé: '{}', Banque: '{}', Montant: {}",
                dto.getLibelle(),
                banque.getNom(),
                dto.getMontant()
        );

        Restitution restitution = new Restitution();
        restitution.setLibelle(dto.getLibelle());
        restitution.setObservation(dto.getObservation());
        restitution.setMontant(dto.getMontant());
        restitution.setBanque(banque);
        restitution.setDateEmission(dto.getDateEmission());

        log.debug(
                "Entité Restitution créée avec succès pour la banque ID: {} (statut par défaut: PREVU)",
                banque.getId()
        );

        return restitution;
    }

    /**
     * Convertit une entité Restitution en DTO de réponse
     */
    public RestitutionResponseDto toResponseDto(Restitution entity) {

        if (entity == null) {
            log.warn("Tentative de conversion d'une entité Restitution null en DTO");
            return null;
        }

        log.debug(
                "Conversion de l'entité Restitution ID: {} vers DTO de réponse",
                entity.getId()
        );

        RestitutionResponseDto dto = new RestitutionResponseDto();
        dto.setId(entity.getId());
        dto.setLibelle(entity.getLibelle());
        dto.setMontant(entity.getMontant());

        if (entity.getBanque() != null) {
            dto.setBanqueId(entity.getBanque().getId());
            dto.setBanqueNom(entity.getBanque().getNom());

            log.trace(
                    "Banque associée à la restitution: ID={}, Nom='{}'",
                    entity.getBanque().getId(),
                    entity.getBanque().getNom()
            );
        } else {
            log.warn("Restitution ID: {} n'a pas de banque associée", entity.getId());
        }

        dto.setDateEmission(entity.getDateEmission());
        dto.setStatus(entity.getStatus());
        dto.setDateOperation(entity.getDateOperation());
        dto.setDateValeur(entity.getDateValeur());
        dto.setObservation(entity.getObservation());

        log.debug(
                "DTO de réponse créé avec succès pour Restitution ID: {}, Statut: {}, Montant: {}",
                entity.getId(),
                entity.getStatus(),
                entity.getMontant()
        );

        return dto;
    }
}
