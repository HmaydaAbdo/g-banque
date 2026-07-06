package staport.finance.gBanque.LigneFonctionnelle.generalLignes.mappers;

import org.springframework.stereotype.Component;
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
 * Conversion Entity ↔ DTO pour LigneFonctionnelle. Aucune logique métier hormis le calcul
 * d'encours/solde, délégué à {@link EncoursCalculatorService} (service centralisé partagé
 * avec les lignes spécifiques).
 *
 * Conservé en {@code @Component} manuel plutôt que converti en interface MapStruct : la
 * construction du DTO dépend d'un appel de service (encours/solde) et d'une logique
 * conditionnelle de hiérarchie (ligne mère / sous-ligne) que MapStruct exprimerait moins
 * clairement qu'une méthode Java explicite (design-patterns.md — préférer la solution la plus
 * simple et la plus lisible).
 */
@Component
public class LigneFonctionnelleMapper {

    private final EncoursCalculatorService encoursCalculatorService;

    public LigneFonctionnelleMapper(EncoursCalculatorService encoursCalculatorService) {
        this.encoursCalculatorService = encoursCalculatorService;
    }

    /**
     * Converts a LigneFonctionnelleRequest DTO to a LigneFonctionnelle entity for creation (Mother or Independent).
     */
    public LigneFonctionnelle toEntity(LigneFonctionnelleRequest request,
                                       Banque banque,
                                       NatureLigneFonctionnelle natureLigneFonctionnelle,
                                       List<Marche> marches) {
        if (request == null) {
            return null;
        }

        LigneFonctionnelle ligneFonctionnelle = new LigneFonctionnelle();

        ligneFonctionnelle.setBanque(banque);
        ligneFonctionnelle.setNatureLigneFonctionnelle(natureLigneFonctionnelle);
        ligneFonctionnelle.setMarches(marches != null ? new ArrayList<>(marches) : new ArrayList<>());
        ligneFonctionnelle.setPlafond(request.plafond());
        ligneFonctionnelle.setTaux(request.taux());
        ligneFonctionnelle.setDateDepart(request.dateDepart());
        ligneFonctionnelle.setDateFin(request.dateFin());
        ligneFonctionnelle.setSousLignes(new ArrayList<>());

        return ligneFonctionnelle;
    }

    /**
     * Helper : Converts a SousLigneRequestDto to a LigneFonctionnelle entity (Sub-line).
     */
    public LigneFonctionnelle toSousLigneEntity(SousLigneRequestDto request,
                                                LigneFonctionnelle ligneMere,
                                                NatureLigneFonctionnelle natureSousLigne) {

        if (request == null || ligneMere == null) {
            return null;
        }

        LigneFonctionnelle sousLigne = new LigneFonctionnelle();

        sousLigne.setId(request.id());
        sousLigne.setPlafond(request.plafond());
        sousLigne.setNatureLigneFonctionnelle(natureSousLigne);
        sousLigne.setBanque(ligneMere.getBanque());
        sousLigne.setMarches(new ArrayList<>(ligneMere.getMarches()));
        sousLigne.setTaux(ligneMere.getTaux());
        sousLigne.setDateDepart(ligneMere.getDateDepart());
        sousLigne.setDateFin(ligneMere.getDateFin());
        sousLigne.setLigneMere(ligneMere);

        return sousLigne;
    }

    /**
     * Converts a LigneFonctionnelle entity to a LigneFonctionnelleResponse DTO.
     */
    public LigneFonctionnelleResponse toDto(LigneFonctionnelle ligneFonctionnelle) {

        if (ligneFonctionnelle == null) {
            return null;
        }

        Long banqueId = null;
        String banqueNom = null;
        if (ligneFonctionnelle.getBanque() != null) {
            banqueId = ligneFonctionnelle.getBanque().getId();
            banqueNom = ligneFonctionnelle.getBanque().getNom();
        }

        List<MarcheRefDto> marcheRefs = new ArrayList<>();
        if (ligneFonctionnelle.getMarches() != null && !ligneFonctionnelle.getMarches().isEmpty()) {
            marcheRefs = ligneFonctionnelle.getMarches().stream()
                    .map(m -> new MarcheRefDto(m.getId(), m.getNumero(), m.getNom()))
                    .collect(Collectors.toList());
        }

        Long natureId = null;
        String natureLabel = null;
        String natureDescription = null;
        if (ligneFonctionnelle.getNatureLigneFonctionnelle() != null) {
            natureId = ligneFonctionnelle.getNatureLigneFonctionnelle().getId();
            natureLabel = ligneFonctionnelle.getNatureLigneFonctionnelle().getLabel();
            natureDescription = ligneFonctionnelle.getNatureLigneFonctionnelle().getDescription();
        }

        BigDecimal[] encoursSolde = computeEncoursAndSolde(ligneFonctionnelle);

        // GESTION HIÉRARCHIQUE
        final boolean estSousLigne = ligneFonctionnelle.getLigneMere() != null;
        final boolean estLigneMere = ligneFonctionnelle.getSousLignes() != null && !ligneFonctionnelle.getSousLignes().isEmpty();

        Long ligneMereId = estSousLigne ? ligneFonctionnelle.getLigneMere().getId() : null;

        List<SousLigneResponseDto> sousLignesDto = new ArrayList<>();
        if (estLigneMere) {
            sousLignesDto = ligneFonctionnelle.getSousLignes().stream()
                    .map(this::toSousLigneDto)
                    .collect(Collectors.toList());
        }

        return new LigneFonctionnelleResponse(
                ligneFonctionnelle.getId(),
                banqueId,
                banqueNom,
                natureId,
                natureLabel,
                natureDescription,
                marcheRefs,
                ligneFonctionnelle.getPlafond(),
                encoursSolde[0],
                encoursSolde[1],
                ligneFonctionnelle.getTaux(),
                ligneFonctionnelle.getDateDepart(),
                ligneFonctionnelle.getDateFin(),
                ligneFonctionnelle.getDateCreation(),
                ligneFonctionnelle.getDateModification(),
                ligneFonctionnelle.isActif(),
                isExpired(ligneFonctionnelle.getDateFin()),
                ligneMereId,
                sousLignesDto,
                estSousLigne,
                estLigneMere
        );
    }

    /**
     * Private helper method to convert a LigneFonctionnelle entity into the lightweight SousLigneResponseDto.
     */
    private SousLigneResponseDto toSousLigneDto(LigneFonctionnelle sousLigne) {
        if (sousLigne == null) {
            return null;
        }

        Long natureId = null;
        String natureLabel = null;
        if (sousLigne.getNatureLigneFonctionnelle() != null) {
            natureId = sousLigne.getNatureLigneFonctionnelle().getId();
            natureLabel = sousLigne.getNatureLigneFonctionnelle().getLabel();
        }

        return new SousLigneResponseDto(
                sousLigne.getId(),
                sousLigne.getPlafond(),
                natureId,
                natureLabel,
                sousLigne.isActif()
        );
    }

    /**
     * Updates an existing LigneFonctionnelle entity.
     */
    public void updateLigneFonctionnelleFromDto(LigneFonctionnelleRequest request,
                                                LigneFonctionnelle ligneFonctionnelle,
                                                Banque newBanque,
                                                NatureLigneFonctionnelle newNature,
                                                List<Marche> newMarches) {
        if (request == null || ligneFonctionnelle == null) {
            return;
        }

        ligneFonctionnelle.setPlafond(request.plafond());

        if (request.taux() != null) {
            ligneFonctionnelle.setTaux(request.taux());
        }

        if (request.dateDepart() != null) {
            ligneFonctionnelle.setDateDepart(request.dateDepart());
        }

        if (request.dateFin() != null) {
            ligneFonctionnelle.setDateFin(request.dateFin());
        }

        if (newBanque != null) {
            ligneFonctionnelle.setBanque(newBanque);
        }

        if (newNature != null) {
            ligneFonctionnelle.setNatureLigneFonctionnelle(newNature);
        }

        ligneFonctionnelle.setMarches(newMarches != null ? new ArrayList<>(newMarches) : new ArrayList<>());
    }

    private boolean isExpired(LocalDate dateFin) {
        if (dateFin == null) {
            return false;
        }
        LocalDate now = LocalDate.now();
        return now.isAfter(dateFin);
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
