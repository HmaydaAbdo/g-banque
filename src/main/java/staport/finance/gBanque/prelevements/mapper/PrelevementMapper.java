package staport.finance.gBanque.prelevements.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.prelevements.dtos.PrelevementRequestDto;
import staport.finance.gBanque.prelevements.dtos.PrelevementResponseDto;
import staport.finance.gBanque.prelevements.entitie.Prelevement;

@Component
public class PrelevementMapper {

    private static final Logger log = LoggerFactory.getLogger(PrelevementMapper.class);

    /**
     * Convertit un DTO de requête en entité Prelevement
     *
     * @param dto DTO contenant les données du prélèvement
     * @param banque Entité Banque associée
     * @return Entité Prelevement créée, ou null si le DTO est null
     */
    public Prelevement toEntity(PrelevementRequestDto dto, Banque banque) {
        if (dto == null) {
            log.warn("Tentative de conversion d'un PrelevementRequestDto null en entité");
            return null;
        }

        if (banque == null) {
            log.error("Tentative de création d'un prélèvement avec une banque null pour le libellé: '{}'", dto.getLibelle());
            throw new IllegalArgumentException("La banque ne peut pas être null lors de la création d'un prélèvement");
        }

        log.debug("Conversion du DTO vers entité Prelevement - Libellé: '{}', Banque: '{}', Montant: {}", dto.getLibelle(), banque.getNom(), dto.getMontant());

        Prelevement prelevement = new Prelevement();
        prelevement.setLibelle(dto.getLibelle());
        prelevement.setObservation(dto.getObservation());
        prelevement.setMontant(dto.getMontant());
        prelevement.setBanque(banque);
        prelevement.setDateEmission(dto.getDateEmission());

        log.debug("Entité Prelevement créée avec succès pour la banque ID: {} (statut par défaut: PREVU)", banque.getId());
        return prelevement;
    }

    /**
     * Convertit une entité Prelevement en DTO de réponse
     *
     * @param entity Entité Prelevement à convertir
     * @return DTO de réponse, ou null si l'entité est null
     */
    public PrelevementResponseDto toResponseDto(Prelevement entity) {
        if (entity == null) {
            log.warn("Tentative de conversion d'une entité Prelevement null en DTO");
            return null;
        }

        log.debug("Conversion de l'entité Prelevement ID: {} vers DTO de réponse", entity.getId());

        PrelevementResponseDto dto = new PrelevementResponseDto();
        dto.setId(entity.getId());
        dto.setLibelle(entity.getLibelle());
        dto.setMontant(entity.getMontant());

        if (entity.getBanque() != null) {
            dto.setBanqueId(entity.getBanque().getId());
            dto.setBanqueNom(entity.getBanque().getNom());
            log.trace("Banque associée au prélèvement: ID={}, Nom='{}'",
                    entity.getBanque().getId(), entity.getBanque().getNom());
        } else {
            log.warn("Prelevement ID: {} n'a pas de banque associée", entity.getId());
        }

        dto.setDateEmission(entity.getDateEmission());
        dto.setStatus(entity.getStatus());
        dto.setDateOperation(entity.getDateOperation() != null ? entity.getDateOperation() : null);
        dto.setDateValeur(entity.getDateValeur() != null ? entity.getDateValeur() : null);
        dto.setObservation(entity.getObservation() != null ? entity.getObservation() : null);

        log.debug("DTO de réponse créé avec succès pour Prelevement ID: {}, Statut: {}, Montant: {}",
                entity.getId(), entity.getStatus(), entity.getMontant());

        return dto;
    }
}