package staport.finance.gBanque.LigneFonctionnelle.generalLignes.mappers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Component
public class LigneFonctionnelleMapper {

    private static final Logger logger = LoggerFactory.getLogger(LigneFonctionnelleMapper.class);

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
            logger.warn("Attempted to convert null LigneFonctionnelleRequest to Entity");
            return null;
        }

        logger.debug("Converting LigneFonctionnelleRequest to Entity - request : {}", request);

        LigneFonctionnelle ligneFonctionnelle = new LigneFonctionnelle();

        ligneFonctionnelle.setBanque(banque);
        ligneFonctionnelle.setNatureLigneFonctionnelle(natureLigneFonctionnelle);
        ligneFonctionnelle.setMarches(marches != null ? new ArrayList<>(marches) : new ArrayList<>());
        ligneFonctionnelle.setPlafond(request.getPlafond());
        ligneFonctionnelle.setTaux(request.getTaux());
        ligneFonctionnelle.setDateDepart(request.getDateDepart());
        ligneFonctionnelle.setDateFin(request.getDateFin());
        ligneFonctionnelle.setSousLignes(new ArrayList<>());

        logger.debug("Successfully converted LigneFonctionnelleRequest to Entity");

        return ligneFonctionnelle;
    }

    /**
     * Helper : Converts a SousLigneRequestDto to a LigneFonctionnelle entity (Sub-line).
     */
    public LigneFonctionnelle toSousLigneEntity(SousLigneRequestDto request,
                                                LigneFonctionnelle ligneMere,
                                                NatureLigneFonctionnelle natureSousLigne) {

        if (request == null) {
            logger.warn("Attempted to convert null SousLigneRequestDto to Entity");
            return null;
        }

        if (ligneMere == null) {
            logger.error("Cannot convert SousLigneRequestDto to Entity - ligne mère is null");
            return null;
        }

        logger.debug("Converting SousLigneRequestDto to Entity - request : {}", request);

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

        logger.debug("Successfully converted SousLigneRequestDto to Entity");

        return sousLigne;
    }

    /**
     * Converts a LigneFonctionnelle entity to a LigneFonctionnelleResponse DTO.
     */
    public LigneFonctionnelleResponse toDto(LigneFonctionnelle ligneFonctionnelle) {

        if (ligneFonctionnelle == null) {
            logger.warn("Attempted to convert null LigneFonctionnelle Entity to DTO");
            return null;
        }

        logger.debug("Converting LigneFonctionnelle Entity to DTO - ID: {}", ligneFonctionnelle.getId());

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

        // ✅ UTILISATION DU SERVICE CENTRALISÉ
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

        logger.debug("Successfully converted LigneFonctionnelle Entity to DTO - ID: {}", ligneFonctionnelle.getId());

        return response;
    }

    /**
     * Private helper method to convert a LigneFonctionnelle entity into the lightweight SousLigneResponseDto.
     */
    private SousLigneResponseDto toSousLigneDto(LigneFonctionnelle sousLigne) {
        if (sousLigne == null) {
            logger.warn("Attempted to convert null sous-ligne Entity to DTO");
            return null;
        }

        logger.debug("Converting sous-ligne Entity to DTO - ID: {}", sousLigne.getId());

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
            logger.warn("Attempted to update with null request or entity");
            return;
        }

        logger.debug("Updating LigneFonctionnelle Entity ID: {}", ligneFonctionnelle.getId());

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

        logger.debug("Successfully updated LigneFonctionnelle Entity ID: {}", ligneFonctionnelle.getId());
    }

    private boolean isExpired(LocalDate dateFin) {
        if (dateFin == null) {
            return false;
        }
        LocalDate now = LocalDate.now();
        return now.isAfter(dateFin);
    }

    /**
     *Utilise EncoursCalculatorService de manière centralisée
     */
    private void mapEncoursAndSolde(LigneFonctionnelle ligneFonctionnelle, LigneFonctionnelleResponse response) {
        if (ligneFonctionnelle.getNatureLigneFonctionnelle() == null) {
            logger.warn("No nature found for ligne ID: {} - Encours and Solde set to null", ligneFonctionnelle.getId());
            response.setEncours(null);
            response.setSolde(null);
            return;
        }

        Long natureId = ligneFonctionnelle.getNatureLigneFonctionnelle().getId();
        Long ligneId = ligneFonctionnelle.getId();
        List<Marche> marches = ligneFonctionnelle.getMarches();
        BigDecimal plafond = ligneFonctionnelle.getPlafond();

        logger.debug("Calculating encours and solde for ligne ID: {}, nature ID: {}", ligneId, natureId);

        // Calcul de l'encours via le service centralisé
        BigDecimal encours = encoursCalculatorService.calculateEncours(
                ligneId,
                natureId,
                marches,
                ligneFonctionnelle.getBanque(),
                plafond
        );

        // Calcul du solde via le service centralisé
        BigDecimal solde = encoursCalculatorService.calculateSolde(
                ligneId,
                natureId,
                marches,
                plafond,
                encours
        );

        response.setEncours(encours);
        response.setSolde(solde);

        logger.debug("Mapped encours: {}, solde: {} for ligne ID: {}", encours, solde, ligneId);
    }
}