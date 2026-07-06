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
        entity.setPlafond(request.plafond());
        entity.setTaux(request.taux());
        entity.setDateDepart(request.dateDepart());
        entity.setDateFin(request.dateFin());
        entity.setActif(request.actif() != null ? request.actif() : true);
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

        Long natureId = null;
        String natureCode = null;
        String natureLabel = null;
        String natureDescription = null;
        if (entity.getNatureLigneFonctionnelle() != null) {
            natureId = entity.getNatureLigneFonctionnelle().getId();
            natureCode = entity.getNatureLigneFonctionnelle().getCode();
            natureLabel = entity.getNatureLigneFonctionnelle().getLabel();
            natureDescription = entity.getNatureLigneFonctionnelle().getDescription();
        }

        Long marcheId = null;
        String marcheNom = null;
        String marcheNumero = null;
        Long banqueId = null;
        String banqueNom = null;
        if (entity.getMarche() != null) {
            marcheId = entity.getMarche().getId();
            marcheNom = entity.getMarche().getNom();
            marcheNumero = entity.getMarche().getNumero();
            banqueId = entity.getMarche().getBanque().getId();
            banqueNom = entity.getMarche().getBanque().getNom();
        }

        // CALCUL AUTOMATIQUE de l'encours et du solde
        BigDecimal encours = null;
        BigDecimal solde = null;
        if (entity.getNatureLigneFonctionnelle() != null) {
            List<Marche> marcheList = entity.getMarche() != null
                    ? List.of(entity.getMarche())
                    : Collections.emptyList();

            encours = encoursCalculatorService.calculateEncours(
                    entity.getId(),
                    natureCode,
                    marcheList,
                    entity.getMarche() != null ? entity.getMarche().getBanque() : null,
                    entity.getPlafond()
            );

            solde = encoursCalculatorService.calculateSolde(
                    entity.getId(),
                    natureCode,
                    marcheList,
                    entity.getPlafond(),
                    encours
            );
        }

        return new LigneFonctionnelleSpecifiqueResponse(
                entity.getId(),
                banqueId,
                banqueNom,
                natureId,
                natureLabel,
                natureDescription,
                entity.getPlafond(),
                encours,
                solde,
                entity.getTaux(),
                entity.getDateDepart(),
                entity.getDateFin(),
                entity.getDateCreation(),
                entity.getDateModification(),
                entity.isActif(),
                isExpired(entity.getDateFin()),
                marcheId,
                marcheNom,
                marcheNumero
        );
    }




    private boolean isExpired(LocalDate dateFin) {
        if (dateFin == null) {
            return false;
        }
        LocalDate now = LocalDate.now();
        return now.isAfter(dateFin);
    }
}