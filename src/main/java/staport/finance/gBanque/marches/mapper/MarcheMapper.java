package staport.finance.gBanque.marches.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.marches.dtos.MarcheRequestDTO;
import staport.finance.gBanque.marches.dtos.MarcheResponseDTO;
import staport.finance.gBanque.marches.entities.Marche;

@Component
public class MarcheMapper {

    private static final Logger log = LoggerFactory.getLogger(MarcheMapper.class);

    /**
     * Converts a MarcheRequestDTO to a Marche entity.
     * This is used when creating a new Marche from client data.
     */
    public Marche toEntity(MarcheRequestDTO dto, Banque banque) {
        log.debug("Début de la conversion MarcheRequestDTO vers Marche entity - Numéro: {}",
                dto != null ? dto.getNumero() : "null");

        if (dto == null) {
            log.warn("MarcheRequestDTO est null, retour d'une entité null");
            return null;
        }

        Marche marche = new Marche();

        marche.setNumero(dto.getNumero());
        marche.setNom(dto.getNom());
        marche.setMontant(dto.getMontant());
        marche.setMaitreDouvrage(dto.getMaitreDouvrage());
        marche.setBanque(banque);
        marche.setNanti(dto.getIsNanti() != null ? dto.getIsNanti() : false);

        // Null-safety for optional dates
        if (dto.getDateDebutDesTravaux() != null) {
            marche.setDateDebutDesTravaux(dto.getDateDebutDesTravaux());
        }

        if (dto.getDateDAchievementDesTravaux() != null) {
            marche.setDateDAchievementDesTravaux(dto.getDateDAchievementDesTravaux());
        }

        log.debug("Conversion MarcheRequestDTO vers Marche entity réussie - Numéro: {}, Nom: {}",
                marche.getNumero(), marche.getNom());

        return marche;
    }

    /**
     * Converts a Marche entity to a MarcheResponseDTO.
     * This is used when sending Marche data to the client.
     */
    public MarcheResponseDTO toResponseDTO(Marche entity) {
        log.debug("Début de la conversion Marche entity vers MarcheResponseDTO - ID: {}",
                entity != null ? entity.getId() : "null");

        if (entity == null) {
            log.warn("Marche entity est null, retour d'un DTO null");
            return null;
        }

        MarcheResponseDTO dto = new MarcheResponseDTO();
        dto.setId(entity.getId());
        dto.setNumero(entity.getNumero());
        dto.setNom(entity.getNom());
        dto.setMontant(entity.getMontant());
        dto.setMaitreDouvrage(entity.getMaitreDouvrage());
        dto.setDateDebutDesTravaux(entity.getDateDebutDesTravaux() != null ? entity.getDateDebutDesTravaux() : null);
        dto.setDateDAchievementDesTravaux(entity.getDateDAchievementDesTravaux() != null ? entity.getDateDAchievementDesTravaux() : null);
        dto.setNanti(entity.isNanti());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setModifiedAt(entity.getModifiedAt());

        if (entity.getBanque() != null) {
            dto.setBanqueNom(entity.getBanque().getNom());
            dto.setBanqueId(entity.getBanque().getId());
        }

        log.debug("Conversion Marche entity vers MarcheResponseDTO réussie - ID: {}, Numéro: {}",
                dto.getId(), dto.getNumero());

        return dto;
    }
}