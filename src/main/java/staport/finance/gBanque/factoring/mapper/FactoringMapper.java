package staport.finance.gBanque.factoring.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.factoring.dtos.FactoringRequestDto;
import staport.finance.gBanque.factoring.dtos.FactoringResponseDto;
import staport.finance.gBanque.factoring.dtos.SuivieFactoringLineResponseDto;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

@Component
public class FactoringMapper {


    private static final Logger log = LoggerFactory.getLogger(FactoringMapper.class);

    public FactoringResponseDto toDto(Factoring factoring) {
        log.debug("Début de la conversion de l'entité Factoring vers le DTO de réponse");

        if (factoring == null) {
            log.warn("L'entité Factoring reçue est null");
            return null;
        }

        log.debug("Conversion du Factoring ID: {} vers DTO", factoring.getId());

        FactoringResponseDto dto = new FactoringResponseDto();
        dto.setId(factoring.getId());
        dto.setDateEmission(factoring.getDateEmission());
        dto.setMontant(factoring.getMontant());

        dto.setType(factoring.getType());
        dto.setDateOperation(factoring.getDateOperation()!=null ? factoring.getDateOperation() : null);
        dto.setDateValeur(factoring.getDateValeur() != null ? factoring.getDateValeur() : null);
        dto.setStatus(factoring.getStatus());
        dto.setObservation("Marche N° " + factoring.getMarche().getNumero() );
        // Check if the marche and its functional lines exist to prevent NullPointerException
        Marche marche = factoring.getMarche();
        if (marche != null) {
            log.debug("Récupération des informations du marché: {} et de la banque: {}",
                    marche.getNom(), marche.getBanque() != null ? marche.getBanque().getNom() : "null");

            dto.setMarcheId(marche.getId());
            dto.setMarcheName(marche.getNom());
            dto.setMarcheNumero(marche.getNumero());
            dto.setBanqueId(marche.getBanque().getId());
            dto.setBanqueName(marche.getBanque().getNom());
            if( factoring.getType()== Type.DEBLOCAGE){
                dto.setLibelle("Déblocage factoring marché " +  marche.getNom() );
                log.debug("Libellé généré pour DEBLOCAGE");
            }else {
                dto.setLibelle("Remboursement factoring marché " + marche.getNom()  );
                log.debug("Libellé généré pour REMBOURSEMENT");
            }
        } else {
            log.warn("Le marché associé au Factoring ID: {} est null", factoring.getId());
            throw new ResourceNotFoundException("le marché associé au Factoring ID: "+factoring.getId()+" est null");
        }

        log.info("Conversion vers FactoringResponseDto terminée avec succès pour l'ID: {}", factoring.getId());

        return dto;
    }

    /**
     * Converts a FactoringRequestDto to a Factoring entity.
     * This method maps the basic fields from the DTO to the entity and sets the
     * related Marche and Banque entities passed as arguments.
     *
     * @param requestDTO The FactoringRequestDto to map.
     * @param marche The Marche entity fetched from the database.
     * @return A new Factoring entity instance with relationships set.
     */
    public Factoring toEntity(FactoringRequestDto requestDTO , Marche marche) {
        log.debug("Début de la conversion du FactoringRequestDto vers l'entité Factoring");

        if (requestDTO == null) {
            log.warn("Le FactoringRequestDto reçu est null");
            return null;
        }

        log.debug("Mapping du DTO vers l'entité pour le marché ID: {}", marche != null ? marche.getId() : "null");

        Factoring entity = new Factoring();
        entity.setDateEmission(requestDTO.getDateEmission());
        entity.setMontant(requestDTO.getMontant());
        entity.setObservation(marche.getNumero());
        entity.setType(requestDTO.getType());
        entity.setMarche(marche);
        entity.setStatus(Status.PREVU);

        log.info("Factoring créé avec succès - Type: {}, Montant: {}, Marché: {}",
                requestDTO.getType(), requestDTO.getMontant(), marche != null ? marche.getNumero() : "null");

        return entity;
    }

    public SuivieFactoringLineResponseDto toSuivieFactoringLigneResponseDto(Factoring factoring){
        log.debug("Début de la conversion de l'entité Factoring vers SuivieFactoringLineResponseDto");

        if (factoring == null) {
            log.warn("L'entité Factoring reçue est null pour le suivi");
            return null;
        }

        log.debug("Conversion du Factoring ID: {} vers SuivieFactoringLineResponseDto", factoring.getId());

        SuivieFactoringLineResponseDto dto = new SuivieFactoringLineResponseDto();
        dto.setId(factoring.getId());
        dto.setDateEmission(factoring.getDateEmission());
        dto.setDateOperation(factoring.getDateOperation()!=null ? factoring.getDateOperation() : null);
        dto.setDateValeur(factoring.getDateValeur() != null ? factoring.getDateValeur() : null);

        // Check if the marche and its functional lines exist to prevent NullPointerException
        Marche marche = factoring.getMarche();
        if (marche != null) {
            log.debug("Attribution des informations du marché et de la banque au DTO de suivi");

            dto.setMarcheName(marche.getNom());
            dto.setMarcheNumero(marche.getNumero());
            dto.setBanqueName(marche.getBanque().getNom());
            if( factoring.getType()== Type.DEBLOCAGE){
                dto.setLibelle("Déblocage du factoring pour le marché " + marche.getNom());
                dto.setCredit(factoring.getMontant());
                log.debug("Ligne de suivi configurée en CRÉDIT - Montant: {}", factoring.getMontant());
            }else {
                dto.setLibelle("Remboursement du factoring pour le marché " +  marche.getNom() );
                dto.setDebit(factoring.getMontant());
                log.debug("Ligne de suivi configurée en DÉBIT - Montant: {}", factoring.getMontant());
            }
        } else {
            log.warn("Le marché associé est null pour le suivi du Factoring ID: {}", factoring.getId());
            throw  new ResourceNotFoundException("le marché associé est null pour le suivi du Factoring ID: "+factoring.getId());
        }

        dto.setObservation(factoring.getObservation());
        dto.setType(factoring.getType());
        dto.setStatus(factoring.getStatus());

        log.info("Conversion vers SuivieFactoringLineResponseDto terminée avec succès pour l'ID: {}", factoring.getId());

        return dto;
    }
}