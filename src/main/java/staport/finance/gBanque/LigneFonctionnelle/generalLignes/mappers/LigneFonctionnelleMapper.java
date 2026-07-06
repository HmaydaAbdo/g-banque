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
        ligneFonctionnelle.setPlafond(request.getPlafond());
        ligneFonctionnelle.setTaux(request.getTaux());
        ligneFonctionnelle.setDateDepart(request.getDateDepart());
        ligneFonctionnelle.setDateFin(request.getDateFin());
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

        sousLigne.setId(request.getId());
        sousLigne.setPlafond(request.getPlafond());
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

        LigneFonctionnelleResponse response = new LigneFonctionnelleResponse();
        response.setId(ligneFonctionnelle.getId());

        if (ligneFonctionnelle.getBanque() != null) {
            response.setBanqueId(ligneFonctionnelle.getBanque().getId());
            response.setBanqueNom(ligneFonctionnelle.getBanque().getNom());
        }

        if (ligneFonctionnelle.getMarches() != null && !ligneFonctionnelle.getMarches().isEmpty()) {
            List<MarcheRefDto> marcheRefs = ligneFonctionnelle.getMarches().stream()
                    .map(m -> new MarcheRefDto(m.getId(), m.getNumero(), m.getNom()))
                    .collect(Collectors.toList());
            response.setMarches(marcheRefs);
        }

        if (ligneFonctionnelle.getNatureLigneFonctionnelle() != null) {
            response.setNatureLigneFonctionnelleId(ligneFonctionnelle.getNatureLigneFonctionnelle().getId());
            response.setNatureLigneFonctionnelleLabel(ligneFonctionnelle.getNatureLigneFonctionnelle().getLabel());
            response.setNatureLigneFonctionnelleDescription(ligneFonctionnelle.getNatureLigneFonctionnelle().getDescription());
        }

        response.setPlafond(ligneFonctionnelle.getPlafond());
        response.setTaux(ligneFonctionnelle.getTaux());
        response.setDateDepart(ligneFonctionnelle.getDateDepart());
        response.setDateFin(ligneFonctionnelle.getDateFin());
        response.setDateCreation(ligneFonctionnelle.getDateCreation());
        response.setDateModification(ligneFonctionnelle.getDateModification());
        response.setActif(ligneFonctionnelle.isActif());
        response.setExpired(isExpired(ligneFonctionnelle.getDateFin()));

        mapEncoursAndSolde(ligneFonctionnelle, response);

        // GESTION HIÉRARCHIQUE
        final boolean estSousLigne = ligneFonctionnelle.getLigneMere() != null;
        final boolean estLigneMere = ligneFonctionnelle.getSousLignes() != null && !ligneFonctionnelle.getSousLignes().isEmpty();

        response.setEstSousLigne(estSousLigne);
        response.setEstLigneMere(estLigneMere);

        if (estSousLigne) {
            response.setLigneMereId(ligneFonctionnelle.getLigneMere().getId());
        }

        if (estLigneMere) {
            List<SousLigneResponseDto> sousLignesDto = ligneFonctionnelle.getSousLignes().stream()
                    .map(this::toSousLigneDto)
                    .collect(Collectors.toList());
            response.setSousLignes(sousLignesDto);
        } else {
            response.setSousLignes(new ArrayList<>());
        }

        return response;
    }

    /**
     * Private helper method to convert a LigneFonctionnelle entity into the lightweight SousLigneResponseDto.
     */
    private SousLigneResponseDto toSousLigneDto(LigneFonctionnelle sousLigne) {
        if (sousLigne == null) {
            return null;
        }

        SousLigneResponseDto dto = new SousLigneResponseDto();
        dto.setId(sousLigne.getId());
        dto.setPlafond(sousLigne.getPlafond());

        if (sousLigne.getNatureLigneFonctionnelle() != null) {
            dto.setNatureLigneFonctionnelleId(sousLigne.getNatureLigneFonctionnelle().getId());
            dto.setNatureLigneFonctionnelleLabel(sousLigne.getNatureLigneFonctionnelle().getLabel());
        }

        dto.setActif(sousLigne.isActif());

        return dto;
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

        ligneFonctionnelle.setPlafond(request.getPlafond());

        if (request.getTaux() != null) {
            ligneFonctionnelle.setTaux(request.getTaux());
        }

        if (request.getDateDepart() != null) {
            ligneFonctionnelle.setDateDepart(request.getDateDepart());
        }

        if (request.getDateFin() != null) {
            ligneFonctionnelle.setDateFin(request.getDateFin());
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
     */
    private void mapEncoursAndSolde(LigneFonctionnelle ligneFonctionnelle, LigneFonctionnelleResponse response) {
        if (ligneFonctionnelle.getNatureLigneFonctionnelle() == null) {
            response.setEncours(null);
            response.setSolde(null);
            return;
        }

        Long natureId = ligneFonctionnelle.getNatureLigneFonctionnelle().getId();
        Long ligneId = ligneFonctionnelle.getId();
        List<Marche> marches = ligneFonctionnelle.getMarches();
        BigDecimal plafond = ligneFonctionnelle.getPlafond();

        BigDecimal encours = encoursCalculatorService.calculateEncours(
                ligneId,
                natureId,
                marches,
                ligneFonctionnelle.getBanque(),
                plafond
        );

        BigDecimal solde = encoursCalculatorService.calculateSolde(
                ligneId,
                natureId,
                marches,
                plafond,
                encours
        );

        response.setEncours(encours);
        response.setSolde(solde);
    }
}
