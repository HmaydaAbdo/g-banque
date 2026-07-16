package staport.finance.gBanque.commision.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.commision.dtos.CommissionRequestDto;
import staport.finance.gBanque.commision.dtos.CommissionResponseDto;
import staport.finance.gBanque.commision.entities.Commission;

import java.math.BigDecimal;

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
                    dto.libelle());
            throw new IllegalArgumentException("La banque ne peut pas être null");
        }

        log.debug("Conversion du DTO vers entité Commission - Libellé: '{}', Banque: '{}', Montant: {}",
                dto.libelle(), banque.getNom(), dto.montant());

        Commission commission = new Commission();
        commission.setLibelle(dto.libelle());
        commission.setObservation(dto.observation());
        commission.setMontant(dto.montant());
        commission.setBanque(banque);
        commission.setOperation(dto.operation());
        commission.setDateEmission(dto.dateEmission());

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

        Long banqueId = null;
        String banqueNom = null;
        if (entity.getBanque() != null) {
            banqueId = entity.getBanque().getId();
            banqueNom = entity.getBanque().getNom();
            log.trace("Banque associée: ID={}, Nom='{}'",
                    entity.getBanque().getId(), entity.getBanque().getNom());
        } else {
            log.warn("Commission ID: {} n'a pas de banque associée", entity.getId());
        }

        // Caution d'origine — nullable (absente pour les commissions saisies manuellement
        // ou générées hors caution, ex : virements)
        Long cautionId = null;
        BigDecimal cautionMontant = null;
        Long natureId = null;
        String natureLabel = null;
        if (entity.getCaution() != null) {
            cautionId = entity.getCaution().getId();
            cautionMontant = entity.getCaution().getMontant();
            if (entity.getCaution().getNature() != null) {
                natureId = entity.getCaution().getNature().getId();
                natureLabel = entity.getCaution().getNature().getLabel();
            }
        }

        CommissionResponseDto dto = new CommissionResponseDto(
                entity.getId(),
                entity.getLibelle(),
                entity.getObservation() != null ? entity.getObservation() : null,
                entity.getMontant(),
                banqueId,
                banqueNom,
                entity.getOperation(),
                entity.getDateEmission(),
                entity.getDateValeur() != null ? entity.getDateValeur() : null,
                entity.getDateOperation() != null ? entity.getDateOperation() : null,
                entity.getStatus(),
                cautionId,
                cautionMontant,
                natureId,
                natureLabel
        );

        log.debug("DTO de réponse créé avec succès pour Commission ID: {}, Statut: {}",
                entity.getId(), entity.getStatus());

        return dto;
    }
}
