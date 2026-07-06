package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.mappers;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.services.EncoursCalculatorService;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.LigneFonctionnelleSpecifiqueRequest;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.LigneFonctionnelleSpecifiqueResponse;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.marches.entities.Marche;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Conversion Entity ↔ DTO pour LigneFonctionnelleSpecifique. Aucune logique métier hormis le calcul
 * d'encours/solde, délégué à {@link EncoursCalculatorService} (service centralisé partagé avec les
 * lignes générales).
 *
 * Conservé en {@code @Component} manuel plutôt que converti en interface MapStruct : la
 * construction du DTO dépend d'un appel de service (encours/solde), comme pour
 * {@code LigneFonctionnelleMapper} (générale) — design-patterns.md, préférer la solution la plus
 * simple et la plus lisible.
 */
@Component
public class LigneFonctionnelleSpecifiqueMapper {

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

        if (request == null) {
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

        return entity;
    }

    /**
     * Calcule automatiquement encours et solde
     */
    public LigneFonctionnelleSpecifiqueResponse toDto(LigneFonctionnelleSpecifique entity) {
        if (entity == null) {
            return null;
        }

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

            List<Marche> marcheList = entity.getMarche() != null
                    ? List.of(entity.getMarche())
                    : Collections.emptyList();

            BigDecimal encours = encoursCalculatorService.calculateEncours(
                    entity.getId(),
                    natureId,
                    marcheList,
                    entity.getMarche() != null ? entity.getMarche().getBanque() : null,
                    entity.getPlafond()
            );

            BigDecimal solde = encoursCalculatorService.calculateSolde(
                    entity.getId(),
                    natureId,
                    marcheList,
                    entity.getPlafond(),
                    encours
            );

            response.setEncours(encours);
            response.setSolde(solde);
        }

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