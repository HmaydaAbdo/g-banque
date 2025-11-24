package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.mappers;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.LigneFonctionnelleSpecifiqueRequest;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.LigneFonctionnelleSpecifiqueResponse;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.SpecificLigneResposneDto;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
        response.setExpired(isExpired(entity.getDateFin()));

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
        List<Prefinancement> prefinancements= entity.getMarche().getPrefinancements().stream().toList();

        BigDecimal totalDeblocages= prefinancements.stream().filter(prefinancement -> prefinancement.getType()== Type.DEBLOCAGE).map(Prefinancement::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRemboursements=prefinancements.stream().filter(prefinancement -> prefinancement.getType()== Type.REMBOURSEMENT).map(Prefinancement::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal encours = totalDeblocages.subtract(totalRemboursements);
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
        List<DroitConstate> droitConstates= entity.getMarche().getDroitConstates().stream().toList();
        BigDecimal totalDeblocages=droitConstates.stream().filter(droitConstate -> droitConstate.getType()== Type.DEBLOCAGE).map(DroitConstate::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRembourssement=droitConstates.stream().filter(droitConstate -> droitConstate.getType()== Type.REMBOURSEMENT).map(DroitConstate::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal encours = totalDeblocages.subtract(totalRembourssement);
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
     */
    public SpecificLigneResposneDto toFactoringResponseDto(LigneFonctionnelleSpecifique entity) {
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
        List<Factoring> factorings= entity.getMarche().getFactorings().stream().toList();
        BigDecimal totalDeblocage=factorings.stream().filter(factoring -> factoring.getType()== Type.DEBLOCAGE).map(Factoring::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRemboursement=factorings.stream().filter(factoring -> factoring.getType()== Type.REMBOURSEMENT).map(Factoring::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal encours = totalDeblocage.subtract(totalRemboursement);
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


    private boolean isExpired(OffsetDateTime dateFin) {
        // Si pas de date de fin, elle ne peut pas être expirée
        if (dateFin == null) {
            return false;
        }

        // 1. On prend l'instant présent exact
        OffsetDateTime now = OffsetDateTime.now();

        // 2. On retourne true si l'instant "now" est après l'instant "dateFin"
        return now.isAfter(dateFin);
    }

}