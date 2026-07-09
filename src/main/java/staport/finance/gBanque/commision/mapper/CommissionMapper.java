package staport.finance.gBanque.commision.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.commision.dtos.CommissionRequestDto;
import staport.finance.gBanque.commision.dtos.CommissionResponseDto;
import staport.finance.gBanque.commision.entities.Commission;

@Component
public class CommissionMapper {

    private static final Logger log = LoggerFactory.getLogger(CommissionMapper.class);

    /**
     * Convertit un DTO de requête en entité Commission
     *
     * @param dto DTO contenant les données de la commission
     * @param banque Entité Banque associée
     * @return Entité Commission créée, ou null si le DTO est null
     */
    public Commission toEntity(CommissionRequestDto dto, Banque banque) {
        if (dto == null) {
            log.warn("Tentative de conversion d'un CommissionRequestDto null en entité");
            return null;
        }

        if (banque == null) {
            log.error("Tentative de création d'une commission avec une banque null pour le libellé: '{}'",
                    dto.getLibelle());
            throw new IllegalArgumentException("La banque ne peut pas être null");
        }

        log.debug("Conversion du DTO vers entité Commission - Libellé: '{}', Banque: '{}', Montant: {}",
                dto.getLibelle(), banque.getNom(), dto.getMontant());

        Commission commission = new Commission();
        commission.setLibelle(dto.getLibelle());
        commission.setObservation(dto.getObservation());
        commission.setMontant(dto.getMontant());
        commission.setBanque(banque);
        commission.setOperation(dto.getOperation());
        commission.setDateEmission(dto.getDateEmission());

        log.debug("Entité Commission créée avec succès pour la banque ID: {}", banque.getId());
        return commission;
    }

    /**
     * Convertit une entité Commission en DTO de réponse
     *
     * @param entity Entité Commission à convertir
     * @return DTO de réponse, ou null si l'entité est null
     */
    public CommissionResponseDto toResponseDto(Commission entity) {
        if (entity == null) {
            log.warn("Tentative de conversion d'une entité Commission null en DTO");
            return null;
        }

        log.debug("Conversion de l'entité Commission ID: {} vers DTO de réponse", entity.getId());

        CommissionResponseDto dto = new CommissionResponseDto();
        dto.setId(entity.getId());
        dto.setLibelle(entity.getLibelle());
        dto.setOperation(entity.getOperation());
        dto.setMontant(entity.getMontant());

        if (entity.getBanque() != null) {
            dto.setBanqueId(entity.getBanque().getId());
            dto.setBanqueNom(entity.getBanque().getNom());
            log.trace("Banque associée: ID={}, Nom='{}'",
                    entity.getBanque().getId(), entity.getBanque().getNom());
        } else {
            log.warn("Commission ID: {} n'a pas de banque associée", entity.getId());
        }

        dto.setDateEmission(entity.getDateEmission());
        dto.setStatus(entity.getStatus());
        dto.setDateOperation(entity.getDateOperation() != null ? entity.getDateOperation() : null);
        dto.setDateValeur(entity.getDateValeur() != null ? entity.getDateValeur() : null);
        dto.setObservation(entity.getObservation() != null ? entity.getObservation() : null);

        // Caution d'origine — nullable (absente pour les commissions saisies manuellement
        // ou générées hors caution, ex : virements)
        if (entity.getCaution() != null) {
            dto.setCautionId(entity.getCaution().getId());
            dto.setCautionMontant(entity.getCaution().getMontant());
            if (entity.getCaution().getNature() != null) {
                dto.setNatureId(entity.getCaution().getNature().getId());
                dto.setNatureLabel(entity.getCaution().getNature().getLabel());
            }
        }

        log.debug("DTO de réponse créé avec succès pour Commission ID: {}, Statut: {}",
                entity.getId(), entity.getStatus());

        return dto;
    }
}
