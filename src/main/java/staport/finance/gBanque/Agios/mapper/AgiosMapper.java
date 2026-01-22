package staport.finance.gBanque.Agios.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Agios.dtos.AgiosRequestDto;
import staport.finance.gBanque.Agios.dtos.AgiosResponseDto;
import staport.finance.gBanque.Agios.entities.Agios;
import staport.finance.gBanque.Bank.entities.Banque;

@Component
public class AgiosMapper {

    private static final Logger log = LoggerFactory.getLogger(AgiosMapper.class);

    /**
     * Convertit un DTO de requête en entité Agios
     *
     * @param dto DTO contenant les données de l'agios
     * @param banque Entité Banque associée
     * @return Entité Agios créée, ou null si le DTO est null
     */
    public Agios toEntity(AgiosRequestDto dto, Banque banque) {
        if (dto == null) {
            log.warn("Tentative de conversion d'un AgiosRequestDto null en entité");
            return null;
        }

        if (banque == null) {
            log.error("Tentative de création d'un agios avec une banque null pour le libellé: '{}'",
                    dto.getLibelle());
            throw new IllegalArgumentException("La banque ne peut pas être null lors de la création d'un agios");
        }

        log.debug("Conversion du DTO vers entité Agios - Libellé: '{}', Banque: '{}', Montant: {}",
                dto.getLibelle(), banque.getNom(), dto.getMontant());

        Agios agios = new Agios();
        agios.setLibelle(dto.getLibelle());
        agios.setObservation(dto.getObservation());
        agios.setMontant(dto.getMontant());
        agios.setBanque(banque);
        agios.setDateEmission(dto.getDateEmission());

        log.debug("Entité Agios créée avec succès pour la banque ID: {} (statut par défaut: PREVU)",
                banque.getId());
        return agios;
    }

    /**
     * Convertit une entité Agios en DTO de réponse
     *
     * @param entity Entité Agios à convertir
     * @return DTO de réponse, ou null si l'entité est null
     */
    public AgiosResponseDto toResponseDto(Agios entity) {
        if (entity == null) {
            log.warn("Tentative de conversion d'une entité Agios null en DTO");
            return null;
        }

        log.debug("Conversion de l'entité Agios ID: {} vers DTO de réponse", entity.getId());

        AgiosResponseDto dto = new AgiosResponseDto();
        dto.setId(entity.getId());
        dto.setLibelle(entity.getLibelle());
        dto.setMontant(entity.getMontant());

        if (entity.getBanque() != null) {
            dto.setBanqueId(entity.getBanque().getId());
            dto.setBanqueNom(entity.getBanque().getNom());
            log.trace("Banque associée à l'agios: ID={}, Nom='{}'",
                    entity.getBanque().getId(), entity.getBanque().getNom());
        } else {
            log.warn("Agios ID: {} n'a pas de banque associée", entity.getId());
        }

        dto.setDateEmission(entity.getDateEmission());
        dto.setStatus(entity.getStatus());
        dto.setDateOperation(entity.getDateOperation() != null ? entity.getDateOperation() : null);
        dto.setDateValeur(entity.getDateValeur() != null ? entity.getDateValeur() : null);
        dto.setObservation(entity.getObservation() != null ? entity.getObservation() : null);

        log.debug("DTO de réponse créé avec succès pour Agios ID: {}, Statut: {}, Montant: {}",
                entity.getId(), entity.getStatus(), entity.getMontant());

        return dto;
    }
}