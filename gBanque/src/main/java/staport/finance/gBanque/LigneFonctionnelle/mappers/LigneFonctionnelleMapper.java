package staport.finance.gBanque.LigneFonctionnelle.mappers;


import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.dtos.LigneFonctionnelleRequest;
import staport.finance.gBanque.LigneFonctionnelle.dtos.LigneFonctionnelleResponse;
import staport.finance.gBanque.LigneFonctionnelle.entities.LigneFonctionnelle;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class LigneFonctionnelleMapper {

    /**
     * Converts a LigneFonctionnelleRequest DTO to a LigneFonctionnelle entity for creation.
     * The related Banque and NatureLigneFonctionnelle entities must be provided by the service layer.
     *
     * @param request The LigneFonctionnelleRequest DTO.
     * @param banque The associated Banque entity (already fetched).
     * @param natureLigneFonctionnelle The associated NatureLigneFonctionnelle entity (already fetched).
     * @return A new LigneFonctionnelle entity.
     */
    public LigneFonctionnelle toEntity(LigneFonctionnelleRequest request,
                                       Banque banque,
                                       NatureLigneFonctionnelle natureLigneFonctionnelle) {
        if (request == null) {
            return null;
        }

        LigneFonctionnelle ligneFonctionnelle = new LigneFonctionnelle();
        ligneFonctionnelle.setBanque(banque);
        ligneFonctionnelle.setNatureLigneFonctionnelle(natureLigneFonctionnelle);


        ligneFonctionnelle.setPlafond(request.getPlafond());
        ligneFonctionnelle.setTaux(request.getTaux());
        ligneFonctionnelle.setDateDepart(request.getDateDepart());
        ligneFonctionnelle.setDateFin(request.getDateFin());


        // ID, dateCreation, dateModification are handled by JPA @PrePersist callbacks

        return ligneFonctionnelle;
    }

    /**
     * Converts a LigneFonctionnelle entity to a LigneFonctionnelleResponse DTO.
     *
     * @param ligneFonctionnelle The LigneFonctionnelle entity.
     * @return A LigneFonctionnelleResponse DTO.
     */
    public LigneFonctionnelleResponse toDto(LigneFonctionnelle ligneFonctionnelle) {
        if (ligneFonctionnelle == null) {
            return null;
        }

        LigneFonctionnelleResponse response = new LigneFonctionnelleResponse();
        response.setId(ligneFonctionnelle.getId());


        if (ligneFonctionnelle.getBanque() != null) {
            response.setBanqueId(ligneFonctionnelle.getBanque().getId());
            response.setBanqueNom(ligneFonctionnelle.getBanque().getNom());
        }

        if (ligneFonctionnelle.getNatureLigneFonctionnelle() != null) {
            response.setNatureLigneFonctionnelleId(ligneFonctionnelle.getNatureLigneFonctionnelle().getId());
            response.setNatureLigneFonctionnelleLabel(ligneFonctionnelle.getNatureLigneFonctionnelle().getLabel());
            response.setNatureLigneFonctionnelleDescription(ligneFonctionnelle.getNatureLigneFonctionnelle().getDescription());
        }

        // Map direct properties
        response.setPlafond(ligneFonctionnelle.getPlafond());
        response.setTaux(ligneFonctionnelle.getTaux());
        response.setDateDepart(ligneFonctionnelle.getDateDepart());
        response.setDateFin(ligneFonctionnelle.getDateFin());
        response.setDateCreation(ligneFonctionnelle.getDateCreation());
        response.setDateModification(ligneFonctionnelle.getDateModification());
        response.setActif(ligneFonctionnelle.isActif());

        return response;
    }


    /**
     * Updates an existing LigneFonctionnelle entity with data from a LigneFonctionnelleRequest DTO.
     * Related entities (Banque, NatureLigneFonctionnelle) should be passed as separate arguments if they are changing.
     * If these arguments are null, the existing relationships on the entity will be preserved.
     *
     * @param request The LigneFonctionnelleRequest DTO with updated data.
     * @param ligneFonctionnelle The existing LigneFonctionnelle entity to be updated.
     * @param newBanque (Optional) The new Banque entity if the bank association is changing. Pass null otherwise.
     * @param newNature (Optional) The new NatureLigneFonctionnelle entity if the nature association is changing. Pass null otherwise.
     */
    public void updateLigneFonctionnelleFromDto(LigneFonctionnelleRequest request,
                                                LigneFonctionnelle ligneFonctionnelle,
                                                Banque newBanque,
                                                NatureLigneFonctionnelle newNature) {
        if (request == null || ligneFonctionnelle == null) {
            return;
        }

        // Update direct properties only if they are provided in the request
        if (request.getPlafond() != null) {
            ligneFonctionnelle.setPlafond(request.getPlafond());
        }
        if (request.getTaux() != null) {
            ligneFonctionnelle.setTaux(request.getTaux());
        }
        if (request.getDateDepart() != null) {
            ligneFonctionnelle.setDateDepart(request.getDateDepart());
        }
        if (request.getDateFin() != null) {
            ligneFonctionnelle.setDateFin(request.getDateFin());
        }


        // Update relationships if new entities are provided
        if (newBanque != null) {
            ligneFonctionnelle.setBanque(newBanque);
        }

        if (newNature != null) {
            ligneFonctionnelle.setNatureLigneFonctionnelle(newNature);
        }


    }
}