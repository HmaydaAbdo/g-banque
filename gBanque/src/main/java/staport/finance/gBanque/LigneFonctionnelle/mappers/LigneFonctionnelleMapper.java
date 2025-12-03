package staport.finance.gBanque.LigneFonctionnelle.mappers;


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


    /**
     * Converts a LigneFonctionnelleRequest DTO to a LigneFonctionnelle entity for creation (Mother or Independent).
     * The service is responsible for creating and linking child entities (SousLignes).
     */
    public LigneFonctionnelle toEntity(LigneFonctionnelleRequest request,
                                       Banque banque,
                                       NatureLigneFonctionnelle natureLigneFonctionnelle) {
        if (request == null) {
            return null;
        }

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

        return ligneFonctionnelle;
    }



    /**
     * Helper : Converts a SousLigneRequestDto to a LigneFonctionnelle entity (Sub-line).
     * The Service must provide the LigneMere entity (for inherited properties) and the NatureLigneFonctionnelle entity.
     */
    public LigneFonctionnelle toSousLigneEntity(SousLigneRequestDto request,
                                                LigneFonctionnelle ligneMere,
                                                NatureLigneFonctionnelle natureSousLigne) {

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

        return sousLigne;
    }




    /**
     * Converts a LigneFonctionnelle entity to a LigneFonctionnelleResponse DTO,
     * incluant les informations de hiérarchie.
     */
    public LigneFonctionnelleResponse toDto(LigneFonctionnelle ligneFonctionnelle) {
        if (ligneFonctionnelle == null) {
            return null;
        }

        LigneFonctionnelleResponse response = new LigneFonctionnelleResponse();
        response.setId(ligneFonctionnelle.getId());

        // ... Mappage des propriétés de la Banque, Nature, Plafond, Taux, Dates ...
        if (ligneFonctionnelle.getBanque() != null) {
            response.setBanqueId(ligneFonctionnelle.getBanque().getId());
            response.setBanqueNom(ligneFonctionnelle.getBanque().getNom());
        }

        if (ligneFonctionnelle.getNatureLigneFonctionnelle() != null) {
            response.setNatureLigneFonctionnelleId(ligneFonctionnelle.getNatureLigneFonctionnelle().getId());
            response.setNatureLigneFonctionnelleLabel(ligneFonctionnelle.getNatureLigneFonctionnelle().getLabel());
            response.setNatureLigneFonctionnelleDescription(ligneFonctionnelle.getNatureLigneFonctionnelle().getDescription());
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

        // GESTION HIÉRARCHIQUE: Rôles et Relations
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
     * Updates an existing LigneFonctionnelle entity (Mother or Independent) with data from the request DTO.
     * The Service handles the update logic for SousLignes.
     */
    public void updateLigneFonctionnelleFromDto(LigneFonctionnelleRequest request,
                                                LigneFonctionnelle ligneFonctionnelle,
                                                Banque newBanque,
                                                NatureLigneFonctionnelle newNature) {
        if (request == null || ligneFonctionnelle == null) {
            return;
        }

        // 1. Mise à jour des propriétés directes de la Ligne Mère
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

        // 2. Mise à jour des relations (Banque et Nature)
        if (newBanque != null) {
            ligneFonctionnelle.setBanque(newBanque);
        }

        if (newNature != null) {
            ligneFonctionnelle.setNatureLigneFonctionnelle(newNature);
        }

        // La mise à jour du lien ligneMere n'est pas gérée ici (immuabilité gérée par le Service si nécessaire).
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