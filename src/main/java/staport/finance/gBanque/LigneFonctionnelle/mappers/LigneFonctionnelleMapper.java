package staport.finance.gBanque.LigneFonctionnelle.mappers;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.dtos.LigneFonctionnelleRequest;
import staport.finance.gBanque.LigneFonctionnelle.dtos.LigneFonctionnelleResponse;
import staport.finance.gBanque.LigneFonctionnelle.dtos.SousLigneRequestDto;
import staport.finance.gBanque.LigneFonctionnelle.dtos.SousLigneResponseDto;
import staport.finance.gBanque.LigneFonctionnelle.entities.LigneFonctionnelle;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class LigneFonctionnelleMapper {


    private static final Logger logger = LoggerFactory.getLogger(LigneFonctionnelleMapper.class);

    /**
     * Converts a LigneFonctionnelleRequest DTO to a LigneFonctionnelle entity for creation (Mother or Independent).
     * The service is responsible for creating and linking child entities (SousLignes).
     */
    public LigneFonctionnelle toEntity(LigneFonctionnelleRequest request,
                                       Banque banque,
                                       NatureLigneFonctionnelle natureLigneFonctionnelle) {
        if (request == null) {
            logger.warn("Attempted to convert null LigneFonctionnelleRequest to Entity");
            return null;
        }

        logger.debug("Converting LigneFonctionnelleRequest to Entity - request : {}", request);

        LigneFonctionnelle ligneFonctionnelle = new LigneFonctionnelle();

        // Mappage des propriétés directes
        ligneFonctionnelle.setBanque(banque);
        ligneFonctionnelle.setNatureLigneFonctionnelle(natureLigneFonctionnelle);
        ligneFonctionnelle.setPlafond(request.getPlafond());
        ligneFonctionnelle.setTaux(request.getTaux());
        ligneFonctionnelle.setDateDepart(request.getDateDepart());
        ligneFonctionnelle.setDateFin(request.getDateFin());

        // Initialisation de la collection de Sous-lignes pour la cascade
        ligneFonctionnelle.setSousLignes(new ArrayList<>());

        logger.debug("Successfully converted LigneFonctionnelleRequest to Entity - Plafond: {}, Taux: {}", request.getPlafond(), request.getTaux());

        return ligneFonctionnelle;
    }



    /**
     * Helper : Converts a SousLigneRequestDto to a LigneFonctionnelle entity (Sub-line).
     * The Service must provide the LigneMere entity (for inherited properties) and the NatureLigneFonctionnelle entity.
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

        // Propriétés spécifiques du DTO Fille
        sousLigne.setId(request.getId()); // Essentiel pour la modification/suppression dans le Service
        sousLigne.setPlafond(request.getPlafond());
        sousLigne.setNatureLigneFonctionnelle(natureSousLigne);

        // Propriétés héritées (EXTRAITES DIRECTEMENT DE LA MÈRE)
        sousLigne.setBanque(ligneMere.getBanque());
        sousLigne.setTaux(ligneMere.getTaux());
        sousLigne.setDateDepart(ligneMere.getDateDepart());
        sousLigne.setDateFin(ligneMere.getDateFin());

        // Lien Parent
        sousLigne.setLigneMere(ligneMere);

        logger.debug("Successfully converted SousLigneRequestDto to Entity ");

        return sousLigne;
    }




    /**
     * Converts a LigneFonctionnelle entity to a LigneFonctionnelleResponse DTO,
     * incluant les informations de hiérarchie.
     */
    public LigneFonctionnelleResponse toDto(LigneFonctionnelle ligneFonctionnelle) {

        if (ligneFonctionnelle == null) {
            logger.warn("Attempted to convert null LigneFonctionnelle Entity to DTO");
            return null;
        }

        logger.debug("Converting LigneFonctionnelle Entity to DTO - ID: {}, Plafond: {}, Encours: {}",
                ligneFonctionnelle.getId(),
                ligneFonctionnelle.getPlafond(),
                ligneFonctionnelle.getEncours());

        LigneFonctionnelleResponse response = new LigneFonctionnelleResponse();
        response.setId(ligneFonctionnelle.getId());

        if (ligneFonctionnelle.getBanque() != null) {
            response.setBanqueId(ligneFonctionnelle.getBanque().getId());
            response.setBanqueNom(ligneFonctionnelle.getBanque().getNom());
            logger.debug("Mapped banque info - ID: {}, Name: {}",
                    ligneFonctionnelle.getBanque().getId(),
                    ligneFonctionnelle.getBanque().getNom());
        }

        if (ligneFonctionnelle.getNatureLigneFonctionnelle() != null) {
            response.setNatureLigneFonctionnelleId(ligneFonctionnelle.getNatureLigneFonctionnelle().getId());
            response.setNatureLigneFonctionnelleLabel(ligneFonctionnelle.getNatureLigneFonctionnelle().getLabel());
            response.setNatureLigneFonctionnelleDescription(ligneFonctionnelle.getNatureLigneFonctionnelle().getDescription());
            logger.debug("Mapped nature info - ID: {}, Label: {}",
                    ligneFonctionnelle.getNatureLigneFonctionnelle().getId(),
                    ligneFonctionnelle.getNatureLigneFonctionnelle().getLabel());
        }

        response.setPlafond(ligneFonctionnelle.getPlafond());
        response.setEncours(ligneFonctionnelle.getEncours());
        response.setSolde(ligneFonctionnelle.getPlafond().subtract(ligneFonctionnelle.getEncours()));
        response.setTaux(ligneFonctionnelle.getTaux());
        response.setDateDepart(ligneFonctionnelle.getDateDepart());
        response.setDateFin(ligneFonctionnelle.getDateFin());
        response.setDateCreation(ligneFonctionnelle.getDateCreation());
        response.setDateModification(ligneFonctionnelle.getDateModification());
        response.setActif(ligneFonctionnelle.isActif());
        response.setExpired(isExpired(ligneFonctionnelle.getDateFin()));

        // GESTION HIÉRARCHIQUE
        final boolean estSousLigne = ligneFonctionnelle.getLigneMere() != null;
        final boolean estLigneMere = ligneFonctionnelle.getSousLignes() != null && !ligneFonctionnelle.getSousLignes().isEmpty();

        response.setEstSousLigne(estSousLigne);
        response.setEstLigneMere(estLigneMere);

        logger.debug("Ligne fonctionnelle ID {} hierarchy - Is sous-ligne: {}, Is ligne mère: {}",
                ligneFonctionnelle.getId(), estSousLigne, estLigneMere);

        if (estSousLigne) {
            response.setLigneMereId(ligneFonctionnelle.getLigneMere().getId());
            logger.debug("Mapped ligne mère ID: {}", ligneFonctionnelle.getLigneMere().getId());
        }

        if (estLigneMere) {
            List<SousLigneResponseDto> sousLignesDto = ligneFonctionnelle.getSousLignes().stream()
                    .map(this::toSousLigneDto)
                    .collect(Collectors.toList());

            response.setSousLignes(sousLignesDto);
            logger.debug("Mapped {} sous-ligne(s) for ligne mère ID: {}",
                    sousLignesDto.size(), ligneFonctionnelle.getId());
        } else {
            response.setSousLignes(new ArrayList<>());
        }

        logger.debug("Successfully converted LigneFonctionnelle Entity to DTO - ID: {}, Actif: {}, Expired: {}",
                ligneFonctionnelle.getId(),
                ligneFonctionnelle.isActif(),
                response.isExpired());

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

        logger.debug("Converting sous-ligne Entity to DTO - ID: {}, Plafond: {}",
                sousLigne.getId(), sousLigne.getPlafond());

        SousLigneResponseDto dto = new SousLigneResponseDto();
        dto.setId(sousLigne.getId());
        dto.setPlafond(sousLigne.getPlafond());

        if (sousLigne.getNatureLigneFonctionnelle() != null) {
            dto.setNatureLigneFonctionnelleId(sousLigne.getNatureLigneFonctionnelle().getId());
            dto.setNatureLigneFonctionnelleLabel(sousLigne.getNatureLigneFonctionnelle().getLabel());
        }

        dto.setActif(sousLigne.isActif());

        logger.debug("Successfully converted sous-ligne Entity to DTO - ID: {}, Actif: {}",
                sousLigne.getId(), sousLigne.isActif());

        return dto;
    }
    /**
     * Updates an existing LigneFonctionnelle entity (Mother or Independent) with data from the request DTO.
     * The Service handles the update logic for SousLignes.
     */
    public void updateLigneFonctionnelleFromDto(LigneFonctionnelleRequest request,
                                                LigneFonctionnelle ligneFonctionnelle,
                                                Banque newBanque,
                                                NatureLigneFonctionnelle newNature) {
        if (request == null) {
            logger.warn("Attempted to update LigneFonctionnelle Entity with null Request");
            return;
        }

        if (ligneFonctionnelle == null) {
            logger.warn("Attempted to update null LigneFonctionnelle Entity");
            return;
        }

        logger.debug("Updating LigneFonctionnelle Entity ID: {} with request data - New plafond: {}, New taux: {}",
                ligneFonctionnelle.getId(), request.getPlafond(), request.getTaux());

        // 1. Mise à jour des propriétés directes
        ligneFonctionnelle.setPlafond(request.getPlafond());

        if (request.getTaux() != null) {
            ligneFonctionnelle.setTaux(request.getTaux());
            logger.debug("Updated taux to: {}", request.getTaux());
        }

        if (request.getDateDepart() != null) {
            ligneFonctionnelle.setDateDepart(request.getDateDepart());
            logger.debug("Updated dateDepart to: {}", request.getDateDepart());
        }

        if (request.getDateFin() != null) {
            ligneFonctionnelle.setDateFin(request.getDateFin());
            logger.debug("Updated dateFin to: {}", request.getDateFin());
        }

        // 2. Mise à jour des relations
        if (newBanque != null) {
            ligneFonctionnelle.setBanque(newBanque);
            logger.debug("Updated banque to ID: {}, Name: {}", newBanque.getId(), newBanque.getNom());
        }

        if (newNature != null) {
            ligneFonctionnelle.setNatureLigneFonctionnelle(newNature);
            logger.debug("Updated nature to ID: {}, Label: {}", newNature.getId(), newNature.getLabel());
        }

        logger.debug("Successfully updated LigneFonctionnelle Entity ID: {}", ligneFonctionnelle.getId());
    }


    private boolean isExpired(OffsetDateTime dateFin) {
        if (dateFin == null) {
            logger.debug("DateFin is null, ligne is not expired");
            return false;
        }

        OffsetDateTime now = OffsetDateTime.now();
        boolean expired = now.isAfter(dateFin);

        logger.debug("Checked expiration - DateFin: {}, Now: {}, Expired: {}",
                dateFin, now, expired);

        return expired;
    }
}