package staport.finance.gBanque.operationErrone.mappers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.operationErrone.dtos.OperationErroneRequestDto;
import staport.finance.gBanque.operationErrone.dtos.OperationErroneResponseDto;
import staport.finance.gBanque.operationErrone.entities.OperationErrone;

@Component
public class OperationErroneMapper {

    private static final Logger log = LoggerFactory.getLogger(OperationErroneMapper.class);

    /**
     * Convertit un DTO de requête en entité OperationErrone
     */
    public OperationErrone toEntity(OperationErroneRequestDto dto, Banque banque) {
        if (dto == null) {
            log.warn("Tentative de conversion d'un OperationErroneRequestDto null en entité");
            return null;
        }

        if (banque == null) {
            log.error("Tentative de création d'une opération erronée avec une banque null pour le libellé: '{}'",
                    dto.getLibelle());
            throw new IllegalArgumentException("La banque ne peut pas être null lors de la création d'une opération erronée");
        }

        log.debug("Conversion du DTO vers entité OperationErrone - Libellé: '{}', Banque: '{}', Montant: {}, Motif: {}",
                dto.getLibelle(), banque.getNom(), dto.getMontant(), dto.getMotif());

        OperationErrone operationErrone = new OperationErrone();
        operationErrone.setLibelle(dto.getLibelle());
        operationErrone.setObservation(dto.getObservation());
        operationErrone.setMontant(dto.getMontant());
        operationErrone.setBanque(banque);
        operationErrone.setDateEmission(dto.getDateEmission());
        operationErrone.setMotif(dto.getMotif());

        log.debug("Entité OperationErrone créée avec succès pour la banque ID: {} (statut par défaut: PREVU, motif: {})",
                banque.getId(), dto.getMotif());
        return operationErrone;
    }

    /**
     * Convertit une entité OperationErrone en DTO de réponse
     */
    public OperationErroneResponseDto toResponseDto(OperationErrone entity) {
        if (entity == null) {
            log.warn("Tentative de conversion d'une entité OperationErrone null en DTO");
            return null;
        }

        log.debug("Conversion de l'entité OperationErrone ID: {} vers DTO de réponse", entity.getId());

        OperationErroneResponseDto dto = new OperationErroneResponseDto();
        dto.setId(entity.getId());
        dto.setLibelle(entity.getLibelle());
        dto.setMontant(entity.getMontant());
        dto.setMotif(entity.getMotif());

        if (entity.getBanque() != null) {
            dto.setBanqueId(entity.getBanque().getId());
            dto.setBanqueNom(entity.getBanque().getNom());
            log.trace("Banque associée à l'opération erronée: ID={}, Nom='{}'",
                    entity.getBanque().getId(), entity.getBanque().getNom());
        } else {
            log.warn("OperationErrone ID: {} n'a pas de banque associée", entity.getId());
        }

        dto.setDateEmission(entity.getDateEmission());
        dto.setStatus(entity.getStatus());
        dto.setDateOperation(entity.getDateOperation() != null ? entity.getDateOperation() : null);
        dto.setDateValeur(entity.getDateValeur() != null ? entity.getDateValeur() : null);
        dto.setObservation(entity.getObservation() != null ? entity.getObservation() : null);

        log.debug("DTO de réponse créé avec succès pour OperationErrone ID: {}, Statut: {}, Montant: {}, Motif: {}",
                entity.getId(), entity.getStatus(), entity.getMontant(), entity.getMotif());

        return dto;
    }
}