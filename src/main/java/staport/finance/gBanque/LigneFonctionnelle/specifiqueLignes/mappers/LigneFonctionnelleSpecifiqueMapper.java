package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.mappers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.LigneFonctionnelleSpecifiqueRequest;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.LigneFonctionnelleSpecifiqueResponse;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.SpecificLigneResposneDto;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;


@Component
public class LigneFonctionnelleSpecifiqueMapper {


    private static final Logger logger = LoggerFactory.getLogger(LigneFonctionnelleSpecifiqueMapper.class);

    /**
     * Converts a LigneFonctionnelleSpecifiqueRequest DTO to a LigneFonctionnelleSpecifique entity.
     *
     * @param request The LigneFonctionnelleSpecifiqueRequest DTO.
     * @param natureLigneFonctionnelle The associated NatureLigneFonctionnelle entity.
     * @param marche The associated Marche entity.
     * @return A new LigneFonctionnelleSpecifique entity.
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

        logger.debug("Mapping request avec Nature ID: {}, Marche ID: {}",
                natureLigneFonctionnelle != null ? natureLigneFonctionnelle.getId() : "null",
                marche != null ? marche.getId() : "null");

        LigneFonctionnelleSpecifique entity = new LigneFonctionnelleSpecifique();

        // Map all fields directly
        entity.setNatureLigneFonctionnelle(natureLigneFonctionnelle);
        entity.setPlafond(request.getPlafond());
        entity.setTaux(request.getTaux());
        entity.setDateDepart(request.getDateDepart());
        entity.setDateFin(request.getDateFin());
        entity.setActif(request.getActif() != null ? request.getActif() : true);
        entity.setMarche(marche);

        logger.debug("Entity créée avec succès - Plafond: {}, Taux: {}, Actif: {}",
                entity.getPlafond(), entity.getTaux(), entity.isActif());

        return entity;
    }

    /**
     * Converts a LigneFonctionnelleSpecifique entity to a LigneFonctionnelleSpecifiqueResponse DTO.
     *
     * @param entity The LigneFonctionnelleSpecifique entity.
     * @return A LigneFonctionnelleSpecifiqueResponse DTO.
     */
    public LigneFonctionnelleSpecifiqueResponse toDto(LigneFonctionnelleSpecifique entity) {
        logger.debug("Début du mapping Entity -> Response DTO");

        if (entity == null) {
            logger.warn("Tentative de mapping avec entity null");
            return null;
        }

        logger.debug("Mapping entity ID: {}", entity.getId());

        LigneFonctionnelleSpecifiqueResponse response = new LigneFonctionnelleSpecifiqueResponse();

        // Map all fields directly
        response.setId(entity.getId());
        response.setNatureLigneFonctionnelleId(entity.getNatureLigneFonctionnelle() != null ? entity.getNatureLigneFonctionnelle().getId() : null);
        response.setNatureLigneFonctionnelleLabel(entity.getNatureLigneFonctionnelle() != null ? entity.getNatureLigneFonctionnelle().getLabel() : null);
        response.setNatureLigneFonctionnelleDescription(entity.getNatureLigneFonctionnelle() != null ? entity.getNatureLigneFonctionnelle().getDescription() : null);
        response.setPlafond(entity.getPlafond());
        response.setTaux(entity.getTaux());
        response.setDateDepart(entity.getDateDepart());
        response.setDateFin(entity.getDateFin());
        response.setDateCreation(entity.getDateCreation());
        response.setDateModification(entity.getDateModification());
        response.setActif(entity.isActif());
        response.setExpired(isExpired(entity.getDateFin()));

        // Map the specific fields
        if (entity.getMarche() != null) {
            response.setMarcheId(entity.getMarche().getId());
            response.setMarcheNom(entity.getMarche().getNom());
            response.setMarcheNumero(entity.getMarche().getNumero());
            response.setBanqueId(entity.getMarche().getBanque().getId());
            response.setBanqueNom(entity.getMarche().getBanque().getNom());

            logger.debug("Marché associé - ID: {}, Numéro: {}",
                    entity.getMarche().getId(), entity.getMarche().getNumero());
        } else {
            logger.debug("Aucun marché associé à l'entity ID: {}", entity.getId());
        }

        logger.debug("Response DTO créé avec succès pour entity ID: {}", entity.getId());

        return response;
    }


    /**
     * Converts a LigneFonctionnelleSpecifique entity to a SpecificLigneResposneDto DTO.
     *
     * @param entity The LigneFonctionnelleSpecifique entity.

     */
    public SpecificLigneResposneDto toPrefinancementResponseDto(LigneFonctionnelleSpecifique entity) {
        logger.debug("Début du mapping Entity -> Préfinancement Response DTO");

        if (entity == null) {
            logger.warn("Tentative de mapping préfinancement avec entity null");
            return null;
        }

        logger.debug("Mapping préfinancement pour entity ID: {}", entity.getId());

        SpecificLigneResposneDto response = new SpecificLigneResposneDto();

        // Map all fields directly
        response.setId(entity.getId());
        // Map the specific fields
        if (entity.getMarche() != null) {
            response.setMarcheNumero(entity.getMarche().getNumero());
            response.setBanqueNom(entity.getMarche().getBanque().getNom());
        }

        // montant
        response.setMontant(entity.getPlafond());

        // encours
        List<Prefinancement> prefinancements = entity.getMarche().getPrefinancements().stream().toList();
        logger.debug("Nombre de préfinancements trouvés: {}", prefinancements.size());

        BigDecimal totalDeblocages = prefinancements.stream()
                .filter(prefinancement -> prefinancement.getType() == Type.DEBLOCAGE)
                .map(Prefinancement::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRemboursements = prefinancements.stream()
                .filter(prefinancement -> prefinancement.getType() == Type.REMBOURSEMENT)
                .map(Prefinancement::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal encours = totalDeblocages.subtract(totalRemboursements);
        response.setEncours(encours);

        logger.debug("Calcul encours préfinancement - Déblocages: {}, Remboursements: {}, Encours: {}",
                totalDeblocages, totalRemboursements, encours);

        //solde
        BigDecimal solde = entity.getPlafond().subtract(encours);
        response.setSolde(solde);

        logger.debug("Solde calculé: {} (Plafond: {} - Encours: {})",
                solde, entity.getPlafond(), encours);

        response.setTaux(entity.getTaux());
        response.setDateDepart(entity.getDateDepart());
        response.setDateFin(entity.getDateFin());
        response.setNatureLigneFonctionnelleLabel(entity.getNatureLigneFonctionnelle() != null ? entity.getNatureLigneFonctionnelle().getLabel() : null);
        response.setActif(entity.isActif());

        logger.debug("Préfinancement Response DTO créé avec succès pour entity ID: {}", entity.getId());

        return response;
    }

    /**
     * Converts a LigneFonctionnelleSpecifique entity to a SpecificLigneResposneDto DTO.
     *
     * @param entity The LigneFonctionnelleSpecifique entity.

     */
    public SpecificLigneResposneDto toDCResponseDto(LigneFonctionnelleSpecifique entity) {
        logger.debug("Début du mapping Entity -> Droit Constaté Response DTO");

        if (entity == null) {
            logger.warn("Tentative de mapping droit constaté avec entity null");
            return null;
        }

        logger.debug("Mapping droit constaté pour entity ID: {}", entity.getId());

        SpecificLigneResposneDto response = new SpecificLigneResposneDto();

        // Map all fields directly
        response.setId(entity.getId());
        // Map the specific fields
        if (entity.getMarche() != null) {
            response.setMarcheNumero(entity.getMarche().getNumero());
            response.setBanqueNom(entity.getMarche().getBanque().getNom());
        }

        // montant
        response.setMontant(entity.getPlafond());

        // encours
        List<DroitConstate> droitConstates = entity.getMarche().getDroitConstates().stream().toList();
        logger.debug("Nombre de droits constatés trouvés: {}", droitConstates.size());

        BigDecimal totalDeblocages = droitConstates.stream()
                .filter(droitConstate -> droitConstate.getType() == Type.DEBLOCAGE)
                .map(DroitConstate::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRembourssement = droitConstates.stream()
                .filter(droitConstate -> droitConstate.getType() == Type.REMBOURSEMENT)
                .map(DroitConstate::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal encours = totalDeblocages.subtract(totalRembourssement);
        response.setEncours(encours);

        logger.debug("Calcul encours DC - Déblocages: {}, Remboursements: {}, Encours: {}",
                totalDeblocages, totalRembourssement, encours);

        //solde
        BigDecimal solde = entity.getPlafond().subtract(encours);
        response.setSolde(solde);

        logger.debug("Solde calculé: {} (Plafond: {} - Encours: {})",
                solde, entity.getPlafond(), encours);

        response.setTaux(entity.getTaux());
        response.setDateDepart(entity.getDateDepart());
        response.setDateFin(entity.getDateFin());
        response.setNatureLigneFonctionnelleLabel(entity.getNatureLigneFonctionnelle() != null ? entity.getNatureLigneFonctionnelle().getLabel() : null);
        response.setActif(entity.isActif());

        logger.debug("Droit Constaté Response DTO créé avec succès pour entity ID: {}", entity.getId());

        return response;
    }

    /**
     * Converts a LigneFonctionnelleSpecifique entity to a SpecificLigneResposneDto DTO.
     *
     * @param entity The LigneFonctionnelleSpecifique entity.
     */
    public SpecificLigneResposneDto toFactoringResponseDto(LigneFonctionnelleSpecifique entity) {
        logger.debug("Début du mapping Entity -> Factoring Response DTO");

        if (entity == null) {
            logger.warn("Tentative de mapping factoring avec entity null");
            return null;
        }

        logger.debug("Mapping factoring pour entity ID: {}", entity.getId());

        SpecificLigneResposneDto response = new SpecificLigneResposneDto();

        // Map all fields directly
        response.setId(entity.getId());
        // Map the specific fields
        if (entity.getMarche() != null) {
            response.setMarcheNumero(entity.getMarche().getNumero());
            response.setBanqueNom(entity.getMarche().getBanque().getNom());
        }

        // montant
        response.setMontant(entity.getPlafond());

        // encours
        List<Factoring> factorings = entity.getMarche().getFactorings().stream().toList();
        logger.debug("Nombre de factorings trouvés: {}", factorings.size());

        BigDecimal totalDeblocage = factorings.stream()
                .filter(factoring -> factoring.getType() == Type.DEBLOCAGE)
                .map(Factoring::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRemboursement = factorings.stream()
                .filter(factoring -> factoring.getType() == Type.REMBOURSEMENT)
                .map(Factoring::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal encours = totalDeblocage.subtract(totalRemboursement);
        response.setEncours(encours);

        logger.debug("Calcul encours factoring - Déblocages: {}, Remboursements: {}, Encours: {}",
                totalDeblocage, totalRemboursement, encours);

        //solde
        BigDecimal solde = entity.getPlafond().subtract(encours);
        response.setSolde(solde);

        logger.debug("Solde calculé: {} (Plafond: {} - Encours: {})",
                solde, entity.getPlafond(), encours);

        response.setTaux(entity.getTaux());
        response.setDateDepart(entity.getDateDepart());
        response.setDateFin(entity.getDateFin());
        response.setNatureLigneFonctionnelleLabel(entity.getNatureLigneFonctionnelle() != null ? entity.getNatureLigneFonctionnelle().getLabel() : null);
        response.setActif(entity.isActif());

        logger.debug("Factoring Response DTO créé avec succès pour entity ID: {}", entity.getId());

        return response;
    }


    private boolean isExpired(OffsetDateTime dateFin) {
        // Si pas de date de fin, elle ne peut pas être expirée
        if (dateFin == null) {
            logger.debug("Vérification expiration: pas de date de fin -> non expirée");
            return false;
        }

        // 1. On prend l'instant présent exact
        OffsetDateTime now = OffsetDateTime.now();

        // 2. On retourne true si l'instant "now" est après l'instant "dateFin"
        boolean expired = now.isAfter(dateFin);

        logger.debug("Vérification expiration: DateFin = {}, Now = {}, Expirée = {}",
                dateFin, now, expired);

        return expired;
    }

}