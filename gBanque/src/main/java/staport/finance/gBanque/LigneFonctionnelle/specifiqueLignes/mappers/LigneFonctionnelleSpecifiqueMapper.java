package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.mappers;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.LigneFonctionnelleSpecifiqueRequest;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.LigneFonctionnelleSpecifiqueResponse;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.SpecificLigneResposneDto;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

import java.math.BigDecimal;
import java.util.List;


@Component
public class LigneFonctionnelleSpecifiqueMapper {

    /**
     * Converts a LigneFonctionnelleSpecifiqueRequest DTO to a LigneFonctionnelleSpecifique entity.
     *
     * @param request The LigneFonctionnelleSpecifiqueRequest DTO.
     * @param natureLigneFonctionnelle The associated NatureLigneFonctionnelle entity.
     * @param marche The associated Marche entity.
     * @return A new LigneFonctionnelleSpecifique entity.
     */
    public LigneFonctionnelleSpecifique toEntity(
            LigneFonctionnelleSpecifiqueRequest request,
            NatureLigneFonctionnelle natureLigneFonctionnelle,
            Marche marche) {
        if (request == null) {
            return null;
        }

        LigneFonctionnelleSpecifique entity = new LigneFonctionnelleSpecifique();

        // Map all fields directly
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
            response.setBanqueId(entity.getMarche().getBanque().getId());
            response.setBanqueNom(entity.getMarche().getBanque().getNom());
        }

        return response;
    }


    /**
     * Converts a LigneFonctionnelleSpecifique entity to a SpecificLigneResposneDto DTO.
     *
     * @param entity The LigneFonctionnelleSpecifique entity.
     * @return A PrefinancementResponseDto DTO.
     */
    public SpecificLigneResposneDto toPrefinancementResponseDto(LigneFonctionnelleSpecifique entity) {
        if (entity == null) {
            return null;
        }

        SpecificLigneResposneDto response = new SpecificLigneResposneDto();

        // Map all fields directly
        response.setId(entity.getId());
        // Map the specific fields
        if (entity.getMarche() != null) {
            response.setMarcheNumero(entity.getMarche().getNumero());
            response.setBanqueNom( entity.getMarche().getBanque().getNom());
        }

        // montant
        response.setMontant(entity.getPlafond());

        // encours
        List<Prefinancement> prefinancements= entity.getMarche().getPrefinancements().stream().filter(prefinancement -> prefinancement.getStatus()== Status.PAYE && prefinancement.getType()== Type.REMBOURSEMENT).toList();
        BigDecimal encours = prefinancements.stream().map(Prefinancement::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setEncours(encours);

        //solde
        response.setSolde(entity.getPlafond().subtract(encours));

        response.setTaux(entity.getTaux());
        response.setDateDepart(entity.getDateDepart());
        response.setDateFin(entity.getDateFin());
        response.setNatureLigneFonctionnelleLabel(entity.getNatureLigneFonctionnelle() != null ? entity.getNatureLigneFonctionnelle().getLabel() : null);
        response.setActif(entity.isActif());

        return response;
    }

    /**
     * Converts a LigneFonctionnelleSpecifique entity to a SpecificLigneResposneDto DTO.
     *
     * @param entity The LigneFonctionnelleSpecifique entity.
     * @return A PrefinancementResponseDto DTO.
     */
    public SpecificLigneResposneDto toDCResponseDto(LigneFonctionnelleSpecifique entity) {
        if (entity == null) {
            return null;
        }

        SpecificLigneResposneDto response = new SpecificLigneResposneDto();

        // Map all fields directly
        response.setId(entity.getId());
        // Map the specific fields
        if (entity.getMarche() != null) {
            response.setMarcheNumero(entity.getMarche().getNumero());
            response.setBanqueNom( entity.getMarche().getBanque().getNom());
        }

        // montant
        response.setMontant(entity.getPlafond());

        // encours
        List<DroitConstate> droitConstates= entity.getMarche().getDroitConstates().stream().filter(droitConstate -> droitConstate.getStatus()== Status.PAYE && droitConstate.getType()== Type.REMBOURSEMENT).toList();
        BigDecimal encours = droitConstates.stream().map(DroitConstate::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setEncours(encours);

        //solde
        response.setSolde(entity.getPlafond().subtract(encours));

        response.setTaux(entity.getTaux());
        response.setDateDepart(entity.getDateDepart());
        response.setDateFin(entity.getDateFin());
        response.setNatureLigneFonctionnelleLabel(entity.getNatureLigneFonctionnelle() != null ? entity.getNatureLigneFonctionnelle().getLabel() : null);
        response.setActif(entity.isActif());

        return response;
    }


}