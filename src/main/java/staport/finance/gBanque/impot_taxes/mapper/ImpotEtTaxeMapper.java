package staport.finance.gBanque.impot_taxes.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.impot_taxes.dtos.ImpotEtTaxeRequestDto;
import staport.finance.gBanque.impot_taxes.dtos.ImpotEtTaxeResponseDto;
import staport.finance.gBanque.impot_taxes.entities.ImpotEtTaxe;

@Component
public class ImpotEtTaxeMapper {

    private static final Logger log = LoggerFactory.getLogger(ImpotEtTaxeMapper.class);

    /**
     * Convertit un DTO de requête en entité ImpotEtTaxe
     */
    public ImpotEtTaxe toEntity(ImpotEtTaxeRequestDto dto, Banque banque) {
        if (dto == null) {
            log.warn("Tentative de conversion d'un ImpotEtTaxeRequestDto null en entité");
            return null;
        }

        if (banque == null) {
            log.error("Tentative de création d'un impôt/taxe avec une banque null pour le libellé: '{}'",
                    dto.getLibelle());
            throw new IllegalArgumentException("La banque ne peut pas être null lors de la création d'un impôt/taxe");
        }

        log.debug("Conversion du DTO vers entité ImpotEtTaxe - Libellé: '{}', Banque: '{}', Montant: {}",
                dto.getLibelle(), banque.getNom(), dto.getMontant());

        ImpotEtTaxe impotEtTaxe = new ImpotEtTaxe();
        impotEtTaxe.setLibelle(dto.getLibelle());
        impotEtTaxe.setObservation(dto.getObservation());
        impotEtTaxe.setMontant(dto.getMontant());
        impotEtTaxe.setBanque(banque);
        impotEtTaxe.setMotif(dto.getMotif());
        impotEtTaxe.setDateEmission(dto.getDateEmission());

        // Note: Le statut est généralement géré par défaut dans l'entité ou la DB (ex: PREVU)
        log.debug("Entité ImpotEtTaxe créée avec succès pour la banque ID: {}", banque.getId());

        return impotEtTaxe;
    }

    /**
     * Convertit une entité ImpotEtTaxe en DTO de réponse
     */
    public ImpotEtTaxeResponseDto toResponseDto(ImpotEtTaxe entity) {
        if (entity == null) {
            log.warn("Tentative de conversion d'une entité ImpotEtTaxe null en DTO");
            return null;
        }

        log.debug("Conversion de l'entité ImpotEtTaxe ID: {} vers DTO de réponse", entity.getId());

        ImpotEtTaxeResponseDto dto = new ImpotEtTaxeResponseDto();
        dto.setId(entity.getId());
        dto.setLibelle(entity.getLibelle());
        dto.setObservation(entity.getObservation());
        dto.setMotif(entity.getMotif());
        dto.setMontant(entity.getMontant());
        dto.setDateEmission(entity.getDateEmission());
        dto.setStatus(entity.getStatus());

        if (entity.getBanque() != null) {
            dto.setBanqueId(entity.getBanque().getId());
            dto.setBanqueNom(entity.getBanque().getNom());
            log.trace("Banque associée à l'impôt/taxe: ID={}, Nom='{}'",
                    entity.getBanque().getId(), entity.getBanque().getNom());
        } else {
            log.warn("L'impôt/taxe ID: {} n'a pas de banque associée", entity.getId());
        }

        // Mapping des dates d'opération et valeur
        dto.setDateOperation(entity.getDateOperation());
        dto.setDateValeur(entity.getDateValeur());

        log.debug("DTO de réponse créé avec succès pour ImpotEtTaxe ID: {}, Statut: {}",
                entity.getId(), entity.getStatus());

        return dto;
    }
}