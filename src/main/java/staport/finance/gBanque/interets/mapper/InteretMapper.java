package staport.finance.gBanque.interets.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.interets.dtos.InteretRequestDto;
import staport.finance.gBanque.interets.dtos.InteretResponseDto;
import staport.finance.gBanque.interets.entities.Interet;

@Component
public class InteretMapper {

    private static final Logger log = LoggerFactory.getLogger(InteretMapper.class);

    /**
     * Convertit un DTO de requête en entité Interet
     */
    public Interet toEntity(InteretRequestDto dto, Banque banque) {
        if (dto == null) {
            log.warn("Tentative de conversion d'un InteretRequestDto null en entité");
            return null;
        }

        if (banque == null) {
            log.error("Tentative de création d'un intérêt avec une banque null pour le libellé: '{}'",
                    dto.getLibelle());
            throw new IllegalArgumentException("La banque ne peut pas être null lors de la création d'un intérêt");
        }

        log.debug("Conversion du DTO vers entité Interet - Libellé: '{}', Banque: '{}', Montant: {}",
                dto.getLibelle(), banque.getNom(), dto.getMontant());

        Interet interet = new Interet();
        interet.setLibelle(dto.getLibelle());
        interet.setObservation(dto.getObservation());
        interet.setMontant(dto.getMontant());
        interet.setBanque(banque);
        interet.setDateEmission(dto.getDateEmission());

        log.debug("Entité Interet créée avec succès pour la banque ID: {} (statut par défaut: PREVU)",
                banque.getId());
        return interet;
    }

    /**
     * Convertit une entité Interet en DTO de réponse
     */
    public InteretResponseDto toResponseDto(Interet entity) {
        if (entity == null) {
            log.warn("Tentative de conversion d'une entité Interet null en DTO");
            return null;
        }

        log.debug("Conversion de l'entité Interet ID: {} vers DTO de réponse", entity.getId());

        InteretResponseDto dto = new InteretResponseDto();
        dto.setId(entity.getId());
        dto.setLibelle(entity.getLibelle());
        dto.setMontant(entity.getMontant());

        if (entity.getBanque() != null) {
            dto.setBanqueId(entity.getBanque().getId());
            dto.setBanqueNom(entity.getBanque().getNom());
            log.trace("Banque associée à l'intérêt: ID={}, Nom='{}'",
                    entity.getBanque().getId(), entity.getBanque().getNom());
        } else {
            log.warn("Interet ID: {} n'a pas de banque associée", entity.getId());
        }

        dto.setDateEmission(entity.getDateEmission());
        dto.setStatus(entity.getStatus());
        dto.setDateOperation(entity.getDateOperation() != null ? entity.getDateOperation() : null);
        dto.setDateValeur(entity.getDateValeur() != null ? entity.getDateValeur() : null);
        dto.setObservation(entity.getObservation() != null ? entity.getObservation() : null);

        log.debug("DTO de réponse créé avec succès pour Interet ID: {}, Statut: {}, Montant: {}",
                entity.getId(), entity.getStatus(), entity.getMontant());

        return dto;
    }
}
