package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Mapper MapStruct pour LigneFonctionnelleSpecifique. Classe abstraite nécessaire pour injecter
 * {@link EncoursCalculatorService} (calcul encours/solde, service centralisé partagé avec les
 * lignes générales), mirroring {@code LigneFonctionnelleMapper} (générale) et
 * {@code EcheanceLeasingMapper}/{@code ContratLeasingMapper} (architecture.md — "Mapper : MapStruct
 * abstract class").
 * <p>
 * {@code encours}/{@code solde} sont calculés une seule fois par {@link #toDto} puis transmis en
 * paramètres à {@link #buildDto} — jamais recalculés deux fois pour la même conversion
 * (performance.md, "ne jamais recalculer une valeur identique plusieurs fois").
 */
@Mapper(componentModel = "spring")
public abstract class LigneFonctionnelleSpecifiqueMapper {

    @Autowired
    protected EncoursCalculatorService encoursCalculatorService;

    // ── toEntity ─────────────────────────────────────────────────────────────

    /**
     * Converts a LigneFonctionnelleSpecifiqueRequest DTO to a LigneFonctionnelleSpecifique entity.
     */
    @Mapping(source = "nature",             target = "natureLigneFonctionnelle")
    @Mapping(source = "request.plafond",    target = "plafond")
    @Mapping(source = "request.taux",       target = "taux")
    @Mapping(source = "request.dateDepart", target = "dateDepart")
    @Mapping(source = "request.dateFin",    target = "dateFin")
    @Mapping(target = "actif", expression = "java(request.actif() != null ? request.actif() : true)")
    @Mapping(source = "marche",             target = "marche")
    @Mapping(target = "id",                 ignore = true)
    @Mapping(target = "dateCreation",       ignore = true)
    @Mapping(target = "dateModification",   ignore = true)
    public abstract LigneFonctionnelleSpecifique toEntity(
            LigneFonctionnelleSpecifiqueRequest request,
            NatureLigneFonctionnelle nature,
            Marche marche);

    // ── toDto ────────────────────────────────────────────────────────────────

    /**
     * Calcule automatiquement encours et solde.
     */
    public LigneFonctionnelleSpecifiqueResponse toDto(LigneFonctionnelleSpecifique entity) {
        if (entity == null) {
            return null;
        }
        BigDecimal[] encoursSolde = computeEncoursAndSolde(entity);
        return buildDto(entity, encoursSolde[0], encoursSolde[1]);
    }

    @Mapping(source = "entity.natureLigneFonctionnelle.id",          target = "natureLigneFonctionnelleId")
    @Mapping(source = "entity.natureLigneFonctionnelle.label",       target = "natureLigneFonctionnelleLabel")
    @Mapping(source = "entity.natureLigneFonctionnelle.description", target = "natureLigneFonctionnelleDescription")
    @Mapping(source = "entity.marche.banque.id",  target = "banqueId")
    @Mapping(source = "entity.marche.banque.nom", target = "banqueNom")
    @Mapping(source = "entity.marche.id",         target = "marcheId")
    @Mapping(source = "entity.marche.nom",        target = "marcheNom")
    @Mapping(source = "entity.marche.numero",     target = "marcheNumero")
    @Mapping(source = "encours", target = "encours")
    @Mapping(source = "solde",   target = "solde")
    @Mapping(target = "expired", expression = "java(isExpired(entity.getDateFin()))")
    protected abstract LigneFonctionnelleSpecifiqueResponse buildDto(
            LigneFonctionnelleSpecifique entity, BigDecimal encours, BigDecimal solde);

    protected boolean isExpired(LocalDate dateFin) {
        if (dateFin == null) {
            return false;
        }
        return LocalDate.now().isAfter(dateFin);
    }

    /**
     * Utilise EncoursCalculatorService de manière centralisée.
     *
     * @return un tableau à deux éléments {@code [encours, solde]} (peuvent être {@code null}).
     */
    private BigDecimal[] computeEncoursAndSolde(LigneFonctionnelleSpecifique entity) {
        if (entity.getNatureLigneFonctionnelle() == null) {
            return new BigDecimal[] { null, null };
        }

        String natureCode = entity.getNatureLigneFonctionnelle().getCode();
        // Un marché archivé est exclu définitivement du calcul d'encours (cf. Marche.actif).
        List<Marche> marcheList = (entity.getMarche() != null && entity.getMarche().isActif())
                ? List.of(entity.getMarche())
                : Collections.emptyList();

        BigDecimal encours = encoursCalculatorService.calculateEncours(
                entity.getId(),
                natureCode,
                marcheList,
                entity.getMarche() != null ? entity.getMarche().getBanque() : null,
                entity.getPlafond()
        );

        BigDecimal solde = encoursCalculatorService.calculateSolde(
                entity.getId(),
                natureCode,
                marcheList,
                entity.getPlafond(),
                encours
        );

        return new BigDecimal[] { encours, solde };
    }
}
