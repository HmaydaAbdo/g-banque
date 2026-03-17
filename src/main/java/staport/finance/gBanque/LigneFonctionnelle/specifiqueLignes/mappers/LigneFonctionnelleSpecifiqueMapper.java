package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.mappers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.services.EncoursCalculatorService;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.LigneFonctionnelleSpecifiqueRequest;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.LigneFonctionnelleSpecifiqueResponse;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.marches.entities.Marche;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class LigneFonctionnelleSpecifiqueMapper {

    private static final Logger logger = LoggerFactory.getLogger(LigneFonctionnelleSpecifiqueMapper.class);

    private final EncoursCalculatorService encoursCalculatorService;

    public LigneFonctionnelleSpecifiqueMapper(EncoursCalculatorService encoursCalculatorService) {
        this.encoursCalculatorService = encoursCalculatorService;
    }

    /**
     * Converts a LigneFonctionnelleSpecifiqueRequest DTO to a LigneFonctionnelleSpecifique entity.
     */
    public LigneFonctionnelleSpecifique toEntity(
            LigneFonctionnelleSpecifiqueRequest request,
            NatureLigneFonctionnelle natureLigneFonctionnelle,
            Marche marche) {

        logger.debug("Début du mapping Request -> Entity");

        if (request == null) {
            logger.warn("Tentative de mapping avec request null");
            return null;
        }

        LigneFonctionnelleSpecifique entity = new LigneFonctionnelleSpecifique();

        entity.setNatureLigneFonctionnelle(natureLigneFonctionnelle);
        entity.setPlafond(request.getPlafond());
        entity.setTaux(request.getTaux());
        entity.setDateDepart(request.getDateDepart());
        entity.setDateFin(request.getDateFin());
        entity.setActif(request.getActif() != null ? request.getActif() : true);
        entity.setMarche(marche);

        logger.debug("Entity créée avec succès");

        return entity;
    }

    /**
     * Calcule automatiquement encours et solde
     */
    public LigneFonctionnelleSpecifiqueResponse toDto(LigneFonctionnelleSpecifique entity) {
        logger.debug("Début du mapping Entity -> Response DTO avec calcul encours/solde");

        if (entity == null) {
            logger.warn("Tentative de mapping avec entity null");
            return null;
        }

        logger.debug("Mapping entity ID: {}", entity.getId());

        LigneFonctionnelleSpecifiqueResponse response = new LigneFonctionnelleSpecifiqueResponse();

        // Mapping des champs de base
        response.setId(entity.getId());

        response.setNatureLigneFonctionnelleId(entity.getNatureLigneFonctionnelle() != null ?
                entity.getNatureLigneFonctionnelle().getId() : null);

        response.setNatureLigneFonctionnelleLabel(entity.getNatureLigneFonctionnelle() != null ?
                entity.getNatureLigneFonctionnelle().getLabel() : null);

        response.setNatureLigneFonctionnelleDescription(entity.getNatureLigneFonctionnelle() != null ?
                entity.getNatureLigneFonctionnelle().getDescription() : null);

        response.setPlafond(entity.getPlafond());

        response.setTaux(entity.getTaux());

        response.setDateDepart(entity.getDateDepart());

        response.setDateFin(entity.getDateFin());

        response.setDateCreation(entity.getDateCreation());

        response.setDateModification(entity.getDateModification());

        response.setActif(entity.isActif());

        response.setExpired(isExpired(entity.getDateFin()));

        if (entity.getMarche() != null) {
            response.setMarcheId(entity.getMarche().getId());
            response.setMarcheNom(entity.getMarche().getNom());
            response.setMarcheNumero(entity.getMarche().getNumero());
            response.setBanqueId(entity.getMarche().getBanque().getId());
            response.setBanqueNom(entity.getMarche().getBanque().getNom());
        }

        // CALCUL AUTOMATIQUE de l'encours et du solde
        if (entity.getNatureLigneFonctionnelle() != null) {
            Long natureId = entity.getNatureLigneFonctionnelle().getId();

            BigDecimal encours = encoursCalculatorService.calculateEncours(
                    entity.getId(),
                    natureId,
                    entity.getMarche(),
                    entity.getMarche().getBanque(),
                    entity.getPlafond()
            );

            BigDecimal solde = encoursCalculatorService.calculateSolde(
                    entity.getId(),
                    natureId,
                    entity.getMarche(),
                    entity.getPlafond(),
                    encours
            );

            response.setEncours(encours);
            response.setSolde(solde);

            logger.debug("Encours et solde calculés - Encours: {}, Solde: {}", encours, solde);
        }

        logger.debug("Response DTO créé avec succès");

        return response;
    }




    private boolean isExpired(LocalDate dateFin) {
        if (dateFin == null) {
            return false;
        }
        LocalDate now = LocalDate.now();
        return now.isAfter(dateFin);
    }
}