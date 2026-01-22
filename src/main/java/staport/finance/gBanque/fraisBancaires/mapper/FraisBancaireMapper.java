package staport.finance.gBanque.fraisBancaires.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireRequestDto;
import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireResposneDto;
import staport.finance.gBanque.fraisBancaires.entities.FraisBancaire;

@Component
public class FraisBancaireMapper {

    private static final Logger log = LoggerFactory.getLogger(FraisBancaireMapper.class);

    /**
     * Convertit un DTO de requête en entité FraisBancaire
     *
     * @param dto DTO contenant les données du frais bancaire
     * @param banque Entité Banque associée
     * @return Entité FraisBancaire créée, ou null si le DTO est null
     */
    public FraisBancaire toEntity(FraisBancaireRequestDto dto, Banque banque) {
        if (dto == null) {
            log.warn("Tentative de conversion d'un FraisBancaireRequestDto null en entité");
            return null;
        }

        if (banque == null) {
            log.error("Tentative de création d'un frais bancaire avec une banque null pour le libellé: '{}'",
                    dto.getLibelle());
            throw new IllegalArgumentException("La banque ne peut pas être null lors de la création d'un frais bancaire");
        }

        log.debug("Conversion du DTO vers entité FraisBancaire - Libellé: '{}', Banque: '{}', Montant: {}",
                dto.getLibelle(), banque.getNom(), dto.getMontant());

        FraisBancaire fraisBancaire = new FraisBancaire();
        fraisBancaire.setLibelle(dto.getLibelle());
        fraisBancaire.setObservation(dto.getObservation());
        fraisBancaire.setMontant(dto.getMontant());
        fraisBancaire.setBanque(banque);
        fraisBancaire.setDateEmission(dto.getDateEmission());

        log.debug("Entité FraisBancaire créée avec succès pour la banque ID: {} (statut par défaut: PREVU)",
                banque.getId());
        return fraisBancaire;
    }

    /**
     * Convertit une entité FraisBancaire en DTO de réponse
     *
     * @param entity Entité FraisBancaire à convertir
     * @return DTO de réponse, ou null si l'entité est null
     */
    public FraisBancaireResposneDto toResponseDto(FraisBancaire entity) {
        if (entity == null) {
            log.warn("Tentative de conversion d'une entité FraisBancaire null en DTO");
            return null;
        }

        log.debug("Conversion de l'entité FraisBancaire ID: {} vers DTO de réponse", entity.getId());

        FraisBancaireResposneDto dto = new FraisBancaireResposneDto();
        dto.setId(entity.getId());
        dto.setLibelle(entity.getLibelle());
        dto.setMontant(entity.getMontant());

        if (entity.getBanque() != null) {
            dto.setBanqueId(entity.getBanque().getId());
            dto.setBanqueNom(entity.getBanque().getNom());
            log.trace("Banque associée au frais bancaire: ID={}, Nom='{}'",
                    entity.getBanque().getId(), entity.getBanque().getNom());
        } else {
            log.warn("FraisBancaire ID: {} n'a pas de banque associée", entity.getId());
        }

        dto.setDateEmission(entity.getDateEmission());
        dto.setStatus(entity.getStatus());
        dto.setDateOperation(entity.getDateOperation() != null ? entity.getDateOperation() : null);
        dto.setDateValeur(entity.getDateValeur() != null ? entity.getDateValeur() : null);
        dto.setObservation(entity.getObservation() != null ? entity.getObservation() : null);

        log.debug("DTO de réponse créé avec succès pour FraisBancaire ID: {}, Statut: {}, Montant: {}",
                entity.getId(), entity.getStatus(), entity.getMontant());

        return dto;
    }
}