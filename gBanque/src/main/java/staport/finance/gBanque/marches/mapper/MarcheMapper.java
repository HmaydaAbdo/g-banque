package staport.finance.gBanque.marches.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.marches.dtos.MarcheRequestDTO;
import staport.finance.gBanque.marches.dtos.MarcheResponseDTO;
import staport.finance.gBanque.marches.entities.Marche;

@Component
public class MarcheMapper {

    /**
     * Converts a MarcheRequestDTO to a Marche entity.
     * This is used when creating a new Marche from client data.
     */
    public Marche toEntity(MarcheRequestDTO dto, Banque banque) {
        if (dto == null) {
            return null;
        }

        Marche marche = new Marche();

        marche.setNumero(dto.getNumero());
        marche.setNom(dto.getNom());
        marche.setMontant(dto.getMontant());
        marche.setMaitreDouvrage(dto.getMaitreDouvrage());
        marche.setBanque(banque);

        // Null-safety for optional dates
        if (dto.getDateDebutDesTravaux() != null) {
            marche.setDateDebutDesTravaux(dto.getDateDebutDesTravaux());
        }

        if (dto.getDateDAchievementDesTravaux() != null) {
            marche.setDateDAchievementDesTravaux(dto.getDateDAchievementDesTravaux());
        }

        return marche;
    }

    /**
     * Converts a Marche entity to a MarcheResponseDTO.
     * This is used when sending Marche data to the client.
     */
    public MarcheResponseDTO toResponseDTO(Marche entity) {
        if (entity == null) {
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
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setModifiedAt(entity.getModifiedAt());

        if (entity.getBanque() != null) {
            dto.setBanqueNom(entity.getBanque().getNom());
            dto.setBanqueId(entity.getBanque().getId());
        }

        return dto;
    }
}