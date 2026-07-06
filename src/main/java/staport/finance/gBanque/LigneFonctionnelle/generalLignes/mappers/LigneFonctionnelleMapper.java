package staport.finance.gBanque.LigneFonctionnelle.generalLignes.mappers;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.dtos.LigneFonctionnelleRequest;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.dtos.LigneFonctionnelleResponse;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.dtos.MarcheRefDto;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.dtos.SousLigneRequestDto;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.dtos.SousLigneResponseDto;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.services.EncoursCalculatorService;
import staport.finance.gBanque.marches.entities.Marche;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper MapStruct pour LigneFonctionnelle. Classe abstraite nécessaire pour injecter
 * {@link EncoursCalculatorService} (calcul encours/solde, service centralisé partagé avec les
 * lignes spécifiques) et exprimer la logique de hiérarchie (ligne mère / sous-ligne) via des
 * méthodes protégées, mirroring {@code EcheanceLeasingMapper}/{@code ContratLeasingMapper}
 * (architecture.md — "Mapper : MapStruct abstract class").
 * <p>
 * {@code encours}/{@code solde} sont calculés une seule fois par {@link #toDto} puis transmis
 * en paramètres à {@link #buildDto} — jamais recalculés deux fois pour la même conversion
 * (performance.md, "ne jamais recalculer une valeur identique plusieurs fois").
 */
@Mapper(componentModel = "spring")
public abstract class LigneFonctionnelleMapper {

    @Autowired
    protected EncoursCalculatorService encoursCalculatorService;

    // ── toEntity ─────────────────────────────────────────────────────────────

    /**
     * Converts a LigneFonctionnelleRequest DTO to a LigneFonctionnelle entity for creation (Mother or Independent).
     */
    @Mapping(source = "banque",                    target = "banque")
    @Mapping(source = "natureLigneFonctionnelle",  target = "natureLigneFonctionnelle")
    @Mapping(source = "request.plafond",           target = "plafond")
    @Mapping(source = "request.taux",              target = "taux")
    @Mapping(source = "request.dateDepart",        target = "dateDepart")
    @Mapping(source = "request.dateFin",           target = "dateFin")
    @Mapping(target = "marches",           ignore = true)
    @Mapping(target = "sousLignes",        ignore = true)
    @Mapping(target = "id",                ignore = true)
    @Mapping(target = "dateCreation",      ignore = true)
    @Mapping(target = "dateModification",  ignore = true)
    @Mapping(target = "actif",             ignore = true)
    @Mapping(target = "ligneMere",         ignore = true)
    @Mapping(target = "lcns",              ignore = true)
    public abstract LigneFonctionnelle toEntity(LigneFonctionnelleRequest request,
                                                Banque banque,
                                                NatureLigneFonctionnelle natureLigneFonctionnelle,
                                                List<Marche> marches);

    @AfterMapping
    protected void afterToEntity(List<Marche> marches, @MappingTarget LigneFonctionnelle ligneFonctionnelle) {
        ligneFonctionnelle.setMarches(marches != null ? new ArrayList<>(marches) : new ArrayList<>());
        ligneFonctionnelle.setSousLignes(new ArrayList<>());
    }

    // ── toSousLigneEntity ────────────────────────────────────────────────────

    /**
     * Helper : Converts a SousLigneRequestDto to a LigneFonctionnelle entity (Sub-line).
     * Hérite banque/taux/dateDepart/dateFin de la ligne mère.
     */
    @Mapping(source = "request.id",            target = "id")
    @Mapping(source = "request.plafond",       target = "plafond")
    @Mapping(source = "natureSousLigne",       target = "natureLigneFonctionnelle")
    @Mapping(source = "ligneMere.banque",      target = "banque")
    @Mapping(source = "ligneMere.taux",        target = "taux")
    @Mapping(source = "ligneMere.dateDepart",  target = "dateDepart")
    @Mapping(source = "ligneMere.dateFin",     target = "dateFin")
    @Mapping(source = "ligneMere",             target = "ligneMere")
    @Mapping(target = "marches",           ignore = true)
    @Mapping(target = "sousLignes",        ignore = true)
    @Mapping(target = "dateCreation",      ignore = true)
    @Mapping(target = "dateModification",  ignore = true)
    @Mapping(target = "actif",             ignore = true)
    @Mapping(target = "lcns",              ignore = true)
    public abstract LigneFonctionnelle toSousLigneEntity(SousLigneRequestDto request,
                                                          LigneFonctionnelle ligneMere,
                                                          NatureLigneFonctionnelle natureSousLigne);

    @AfterMapping
    protected void afterToSousLigneEntity(LigneFonctionnelle ligneMere, @MappingTarget LigneFonctionnelle sousLigne) {
        sousLigne.setMarches(new ArrayList<>(ligneMere.getMarches()));
    }

    // ── toDto ────────────────────────────────────────────────────────────────

    /**
     * Converts a LigneFonctionnelle entity to a LigneFonctionnelleResponse DTO.
     * Calcule encours/solde une seule fois puis délègue l'assemblage du DTO (record immuable)
     * à {@link #buildDto}.
     */
    public LigneFonctionnelleResponse toDto(LigneFonctionnelle ligneFonctionnelle) {
        if (ligneFonctionnelle == null) {
            return null;
        }
        BigDecimal[] encoursSolde = computeEncoursAndSolde(ligneFonctionnelle);
        return buildDto(ligneFonctionnelle, encoursSolde[0], encoursSolde[1]);
    }

    @Mapping(source = "entity.banque.id",                                 target = "banqueId")
    @Mapping(source = "entity.banque.nom",                                target = "banqueNom")
    @Mapping(source = "entity.natureLigneFonctionnelle.id",               target = "natureLigneFonctionnelleId")
    @Mapping(source = "entity.natureLigneFonctionnelle.label",            target = "natureLigneFonctionnelleLabel")
    @Mapping(source = "entity.natureLigneFonctionnelle.description",      target = "natureLigneFonctionnelleDescription")
    @Mapping(target = "marches",       expression = "java(computeMarcheRefs(entity))")
    @Mapping(source = "encours",       target = "encours")
    @Mapping(source = "solde",         target = "solde")
    @Mapping(source = "entity.taux",           target = "taux")
    @Mapping(source = "entity.dateDepart",     target = "dateDepart")
    @Mapping(source = "entity.dateFin",        target = "dateFin")
    @Mapping(source = "entity.dateCreation",     target = "dateCreation")
    @Mapping(source = "entity.dateModification", target = "dateModification")
    @Mapping(source = "entity.actif",          target = "actif")
    @Mapping(target = "expired",       expression = "java(isExpired(entity.getDateFin()))")
    @Mapping(target = "ligneMereId",   expression = "java(computeLigneMereId(entity))")
    @Mapping(target = "sousLignes",    expression = "java(computeSousLignesDto(entity))")
    @Mapping(target = "estSousLigne",  expression = "java(entity.getLigneMere() != null)")
    @Mapping(target = "estLigneMere",  expression = "java(entity.getSousLignes() != null && !entity.getSousLignes().isEmpty())")
    protected abstract LigneFonctionnelleResponse buildDto(LigneFonctionnelle entity, BigDecimal encours, BigDecimal solde);

    /**
     * Private helper method to convert a LigneFonctionnelle entity into the lightweight SousLigneResponseDto.
     */
    @Mapping(source = "natureLigneFonctionnelle.id",    target = "natureLigneFonctionnelleId")
    @Mapping(source = "natureLigneFonctionnelle.label", target = "natureLigneFonctionnelleLabel")
    protected abstract SousLigneResponseDto toSousLigneDto(LigneFonctionnelle sousLigne);

    protected List<MarcheRefDto> computeMarcheRefs(LigneFonctionnelle ligneFonctionnelle) {
        if (ligneFonctionnelle.getMarches() == null || ligneFonctionnelle.getMarches().isEmpty()) {
            return new ArrayList<>();
        }
        return ligneFonctionnelle.getMarches().stream()
                .map(m -> new MarcheRefDto(m.getId(), m.getNumero(), m.getNom()))
                .collect(Collectors.toList());
    }

    protected Long computeLigneMereId(LigneFonctionnelle ligneFonctionnelle) {
        return ligneFonctionnelle.getLigneMere() != null ? ligneFonctionnelle.getLigneMere().getId() : null;
    }

    protected List<SousLigneResponseDto> computeSousLignesDto(LigneFonctionnelle ligneFonctionnelle) {
        boolean estLigneMere = ligneFonctionnelle.getSousLignes() != null && !ligneFonctionnelle.getSousLignes().isEmpty();
        if (!estLigneMere) {
            return new ArrayList<>();
        }
        return ligneFonctionnelle.getSousLignes().stream()
                .map(this::toSousLigneDto)
                .collect(Collectors.toList());
    }

    protected boolean isExpired(LocalDate dateFin) {
        if (dateFin == null) {
            return false;
        }
        return LocalDate.now().isAfter(dateFin);
    }

    // ── updateEntityFromDto ──────────────────────────────────────────────────

    /**
     * Updates an existing LigneFonctionnelle entity. {@code taux}/{@code dateDepart}/{@code dateFin}
     * ne sont mis à jour que si non-null dans la requête (mise à jour partielle) ; {@code banque}/
     * {@code natureLigneFonctionnelle} ne sont mis à jour que si les entités résolues sont non-null.
     * {@code plafond} est toujours écrasé (comportement d'origine).
     */
    @Mapping(source = "request.plafond",  target = "plafond")
    @Mapping(source = "request.taux",             target = "taux",
             nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "request.dateDepart",       target = "dateDepart",
             nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "request.dateFin",          target = "dateFin",
             nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "newBanque",                target = "banque",
             nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "newNature",                target = "natureLigneFonctionnelle",
             nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "marches",           ignore = true)
    @Mapping(target = "id",                ignore = true)
    @Mapping(target = "dateCreation",      ignore = true)
    @Mapping(target = "dateModification",  ignore = true)
    @Mapping(target = "actif",             ignore = true)
    @Mapping(target = "ligneMere",         ignore = true)
    @Mapping(target = "sousLignes",        ignore = true)
    @Mapping(target = "lcns",              ignore = true)
    public abstract void updateLigneFonctionnelleFromDto(LigneFonctionnelleRequest request,
                                                          @MappingTarget LigneFonctionnelle ligneFonctionnelle,
                                                          Banque newBanque,
                                                          NatureLigneFonctionnelle newNature,
                                                          List<Marche> newMarches);

    @AfterMapping
    protected void afterUpdateEntityFromDto(List<Marche> newMarches, @MappingTarget LigneFonctionnelle ligneFonctionnelle) {
        ligneFonctionnelle.setMarches(newMarches != null ? new ArrayList<>(newMarches) : new ArrayList<>());
    }

    /**
     * Utilise EncoursCalculatorService de manière centralisée.
     *
     * @return un tableau à deux éléments {@code [encours, solde]} (peuvent être {@code null}).
     */
    private BigDecimal[] computeEncoursAndSolde(LigneFonctionnelle ligneFonctionnelle) {
        if (ligneFonctionnelle.getNatureLigneFonctionnelle() == null) {
            return new BigDecimal[] { null, null };
        }

        String natureCode = ligneFonctionnelle.getNatureLigneFonctionnelle().getCode();
        Long ligneId = ligneFonctionnelle.getId();
        List<Marche> marches = ligneFonctionnelle.getMarches();
        BigDecimal plafond = ligneFonctionnelle.getPlafond();

        BigDecimal encours = encoursCalculatorService.calculateEncours(
                ligneId,
                natureCode,
                marches,
                ligneFonctionnelle.getBanque(),
                plafond
        );

        BigDecimal solde = encoursCalculatorService.calculateSolde(
                ligneId,
                natureCode,
                marches,
                plafond,
                encours
        );

        return new BigDecimal[] { encours, solde };
    }
}
