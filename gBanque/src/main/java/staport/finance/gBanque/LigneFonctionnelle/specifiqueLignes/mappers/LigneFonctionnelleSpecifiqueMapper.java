package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.mappers;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.LigneFonctionnelleSpecifiqueRequest;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.LigneFonctionnelleSpecifiqueResponse;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.marches.entities.Marche;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class LigneFonctionnelleSpecifiqueMapper {

    /**
     * Converts a LigneFonctionnelleSpecifiqueRequest DTO to a LigneFonctionnelleSpecifique entity.
     *
     * @param request The LigneFonctionnelleSpecifiqueRequest DTO.
     * @param banque The associated Banque entity.
     * @param natureLigneFonctionnelle The associated NatureLigneFonctionnelle entity.
     * @param marche The associated Marche entity.
     * @return A new LigneFonctionnelleSpecifique entity.
     */
    public LigneFonctionnelleSpecifique toEntity(
            LigneFonctionnelleSpecifiqueRequest request,
            Banque banque,
            NatureLigneFonctionnelle natureLigneFonctionnelle,
            Marche marche) {
        if (request == null) {
            return null;
        }

        LigneFonctionnelleSpecifique entity = new LigneFonctionnelleSpecifique();

        // Map all fields directly
        entity.setBanque(banque);
        entity.setNatureLigneFonctionnelle(natureLigneFonctionnelle);
        entity.setPlafond(request.getPlafond());
        entity.setTaux(request.getTaux());
        entity.setDateDepart(request.getDateDepart());
        entity.setDateFin(request.getDateFin());
        entity.setActif(request.getActif() != null ? request.getActif() : true);
        entity.setMarche(marche);

        return entity;
    }

    /**
     * Converts a LigneFonctionnelleSpecifique entity to a LigneFonctionnelleSpecifiqueResponse DTO.
     *
     * @param entity The LigneFonctionnelleSpecifique entity.
     * @return A LigneFonctionnelleSpecifiqueResponse DTO.
     */
    public LigneFonctionnelleSpecifiqueResponse toDto(LigneFonctionnelleSpecifique entity) {
        if (entity == null) {
            return null;
        }

        LigneFonctionnelleSpecifiqueResponse response = new LigneFonctionnelleSpecifiqueResponse();

        // Map all fields directly
        response.setId(entity.getId());
        response.setBanqueId(entity.getBanque() != null ? entity.getBanque().getId() : null);
        response.setBanqueNom(entity.getBanque() != null ? entity.getBanque().getNom() : null);
        response.setNatureLigneFonctionnelleId(entity.getNatureLigneFonctionnelle() != null ? entity.getNatureLigneFonctionnelle().getId() : null);
        response.setNatureLigneFonctionnelleLabel(entity.getNatureLigneFonctionnelle() != null ? entity.getNatureLigneFonctionnelle().getLabel() : null);
        response.setNatureLigneFonctionnelleDescription(entity.getNatureLigneFonctionnelle() != null ? entity.getNatureLigneFonctionnelle().getDescription() : null);
        response.setPlafond(entity.getPlafond());
        response.setTaux(entity.getTaux());
        response.setDateDepart(entity.getDateDepart());
        response.setDateFin(entity.getDateFin());
        response.setDateCreation(entity.getDateCreation());
        response.setDateModification(entity.getDateModification());
        response.setActif(entity.isActif());

        // Map the specific fields
        if (entity.getMarche() != null) {
            response.setMarcheId(entity.getMarche().getId());
            response.setMarcheNom(entity.getMarche().getNom());
            response.setMarcheNumero(entity.getMarche().getNumero());
        }

        return response;
    }

    /**
     * Converts a list of LigneFonctionnelleSpecifique entities to a list of DTOs.
     *
     * @param entities The list of LigneFonctionnelleSpecifique entities.
     * @return A list of LigneFonctionnelleSpecifiqueResponse DTOs.
     */
    public List<LigneFonctionnelleSpecifiqueResponse> toDtoList(List<LigneFonctionnelleSpecifique> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}