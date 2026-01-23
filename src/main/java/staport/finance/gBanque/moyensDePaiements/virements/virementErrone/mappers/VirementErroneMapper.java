package staport.finance.gBanque.moyensDePaiements.virements.virementErrone.mappers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.moyensDePaiements.virements.virementErrone.dtos.VirementErroneRequestDto;
import staport.finance.gBanque.moyensDePaiements.virements.virementErrone.dtos.VirementErroneResponseDto;
import staport.finance.gBanque.moyensDePaiements.virements.virementErrone.entities.VirementErrone;

@Component
public class VirementErroneMapper {

    private static final Logger log = LoggerFactory.getLogger(VirementErroneMapper.class);

    /**
     * Convertit un DTO de requête en entité VirementErrone
     */
    public VirementErrone toEntity(VirementErroneRequestDto dto, Banque banque) {
        if (dto == null) {
            log.warn("Tentative de conversion d'un VirementErroneRequestDto null en entité");
            return null;
        }

        if (banque == null) {
            log.error("Tentative de création d'un virement erroné avec une banque null pour le libellé: '{}'",
                    dto.getLibelle());
            throw new IllegalArgumentException("La banque ne peut pas être null lors de la création d'un virement erroné");
        }

        log.debug("Conversion du DTO vers entité VirementErrone - Libellé: '{}', Banque: '{}', Montant: {}",
                dto.getLibelle(), banque.getNom(), dto.getMontant());

        VirementErrone virementErrone = new VirementErrone();
        virementErrone.setLibelle(dto.getLibelle());
        virementErrone.setObservation(dto.getObservation());
        virementErrone.setMontant(dto.getMontant());
        virementErrone.setBanque(banque);
        virementErrone.setDateEmission(dto.getDateEmission());

        log.debug("Entité VirementErrone créée avec succès pour la banque ID: {} (statut par défaut: PREVU)",
                banque.getId());
        return virementErrone;
    }

    /**
     * Convertit une entité VirementErrone en DTO de réponse
     */
    public VirementErroneResponseDto toResponseDto(VirementErrone entity) {
        if (entity == null) {
            log.warn("Tentative de conversion d'une entité VirementErrone null en DTO");
            return null;
        }

        log.debug("Conversion de l'entité VirementErrone ID: {} vers DTO de réponse", entity.getId());

        VirementErroneResponseDto dto = new VirementErroneResponseDto();
        dto.setId(entity.getId());
        dto.setLibelle(entity.getLibelle());
        dto.setMontant(entity.getMontant());

        if (entity.getBanque() != null) {
            dto.setBanqueId(entity.getBanque().getId());
            dto.setBanqueNom(entity.getBanque().getNom());
            log.trace("Banque associée au virement erroné: ID={}, Nom='{}'",
                    entity.getBanque().getId(), entity.getBanque().getNom());
        } else {
            log.warn("VirementErrone ID: {} n'a pas de banque associée", entity.getId());
        }

        dto.setDateEmission(entity.getDateEmission());
        dto.setStatus(entity.getStatus());
        dto.setDateOperation(entity.getDateOperation() != null ? entity.getDateOperation() : null);
        dto.setDateValeur(entity.getDateValeur() != null ? entity.getDateValeur() : null);
        dto.setObservation(entity.getObservation() != null ? entity.getObservation() : null);

        log.debug("DTO de réponse créé avec succès pour VirementErrone ID: {}, Statut: {}, Montant: {}",
                entity.getId(), entity.getStatus(), entity.getMontant());

        return dto;
    }
}