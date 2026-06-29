package staport.finance.gBanque.droitConstate.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.droitConstate.dtos.DroitConstateRequestDto;
import staport.finance.gBanque.droitConstate.dtos.DroitConstateResponseDto;
import staport.finance.gBanque.droitConstate.dtos.SuivieDcLigneResponseDto;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

@Component
public class DroitConstateMapper {


    private static final Logger log = LoggerFactory.getLogger(DroitConstateMapper.class);

    /**
     * Converts a DroitConstateRequestDto to a DroitConstate entity.
     * This method maps the basic fields from the DTO to the entity and sets the
     * related Marche and Banque entities passed as arguments.
     *
     * @param requestDTO The DroitConstateRequestDto to map.
     * @param marche The Marche entity fetched from the database.
     * @param banque The Banque entity : fournie explicitement ou déduite de marche.banque.
     * @return A new DroitConstate entity instance with relationships set.
     */
    public DroitConstate toEntity(DroitConstateRequestDto requestDTO, Marche marche, Banque banque) {
        log.debug("Début de la conversion du DroitConstateRequestDto vers l'entité DroitConstate");

        if (requestDTO == null) {
            log.warn("Le DroitConstateRequestDto reçu est null");
            return null;
        }

        log.debug("Mapping du DTO vers l'entité pour le marché ID: {}", marche != null ? marche.getId() : "null");

        DroitConstate entity = new DroitConstate();
        entity.setDateEmission(requestDTO.getDateEmission());
        entity.setMontant(requestDTO.getMontant());
        entity.setObservation(marche.getNumero());
        entity.setType(requestDTO.getType());
        entity.setMarche(marche);
        entity.setBanque(banque);
        entity.setStatus(Status.PREVU);

        log.info("DroitConstate créé avec succès - Type: {}, Montant: {}, Marché: {}, Banque: {}",
                requestDTO.getType(), requestDTO.getMontant(),
                marche != null ? marche.getNumero() : "null",
                banque != null ? banque.getNom() : "null");

        return entity;
    }


    /**
     * Converts a DroitConstate entity to a DroitConstateResponseDto..
     * This method manually maps the fields from the entity, including details
     * from the related Marche and Banque entities.
     *
     * @param droitConstate The DroitConstate entity to map.
     * @return A new DroitConstateResponseDto instance.
     */
    public DroitConstateResponseDto toDto(DroitConstate droitConstate) {
        log.debug("Début de la conversion de l'entité DroitConstate vers le DTO de réponse");

        if (droitConstate == null) {
            log.warn("L'entité DroitConstate reçue est null");
            return null;
        }

        log.debug("Conversion du DroitConstate ID: {} vers DTO", droitConstate.getId());

        DroitConstateResponseDto dto = new DroitConstateResponseDto();
        dto.setId(droitConstate.getId());
        dto.setDateEmission(droitConstate.getDateEmission());
        dto.setMontant(droitConstate.getMontant());

        dto.setType(droitConstate.getType());
        dto.setDateOperation(droitConstate.getDateOperation()!=null ? droitConstate.getDateOperation() : null);
        dto.setDateValeur(droitConstate.getDateValeur() != null ? droitConstate.getDateValeur() : null);
        dto.setStatus(droitConstate.getStatus());
        dto.setObservation("Marche N° " + droitConstate.getMarche().getNumero());
        // Check if the marche and its functional lines exist to prevent NullPointerException
        Marche marche = droitConstate.getMarche();

        if (marche != null) {
            dto.setMarcheId(marche.getId());
            dto.setMarcheName(marche.getNom());
            dto.setMarcheNumero(marche.getNumero());
        } else {
            log.warn("Le marché associé au DroitConstate ID: {} est null", droitConstate.getId());
            throw new ResourceNotFoundException("Le marché associé au DroitConstate ID: " + droitConstate.getId() + " est null");
        }

        // Banque directe du DC — toujours obligatoire.
        Banque banque = droitConstate.getBanque();
        if (banque != null) {
            log.debug("Banque du DC: {}", banque.getNom());
            dto.setBanqueId(banque.getId());
            dto.setBanqueName(banque.getNom());
        } else {
            log.warn("Aucune banque trouvée pour le DroitConstate ID: {} (données legacy sans backfill)", droitConstate.getId());
        }

        if( droitConstate.getType()== Type.DEBLOCAGE){
            dto.setLibelle("Déblocage  droit constaté  marché " + (marche != null ? marche.getNom() : null));
            log.debug("Libellé généré pour DEBLOCAGE");
        }else {
            dto.setLibelle("Remboursement  droit constaté  marché " + (marche != null ? marche.getNom() : null) + "");
            log.debug("Libellé généré pour REMBOURSEMENT");
        }

        log.info("Conversion vers DroitConstateResponseDto terminée avec succès pour l'ID: {}", droitConstate.getId());

        return dto;
    }

    public SuivieDcLigneResponseDto toSuivieDcLigneResponseDto(DroitConstate droitConstate){
        log.debug("Début de la conversion de l'entité DroitConstate vers SuivieDcLigneResponseDto");

        if (droitConstate == null) {
            log.warn("L'entité DroitConstate reçue est null pour le suivi");
            return null;
        }

        log.debug("Conversion du DroitConstate ID: {} vers SuivieDcLigneResponseDto", droitConstate.getId());

        SuivieDcLigneResponseDto dto = new SuivieDcLigneResponseDto();
        dto.setId(droitConstate.getId());
        dto.setDateEmission(droitConstate.getDateEmission());
        dto.setDateOperation(droitConstate.getDateOperation()!=null ? droitConstate.getDateOperation() : null);
        dto.setDateValeur(droitConstate.getDateValeur() != null ? droitConstate.getDateValeur() : null);
        dto.setObservation(droitConstate.getObservation());
        dto.setType(droitConstate.getType());
        dto.setStatus(droitConstate.getStatus());

        // Check if the marche and its functional lines exist to prevent NullPointerException
        Marche marche = droitConstate.getMarche();

        if (marche != null) {
            log.debug("Attribution des informations du marché et de la banque au DTO de suivi");
            dto.setMarcheName(marche.getNom());
            dto.setMarcheNumero(marche.getNumero());
            // Banque directe du DC — toujours obligatoire.
            Banque banqueSuivi = droitConstate.getBanque();
            dto.setBanqueName(banqueSuivi != null ? banqueSuivi.getNom() : null);
        } else {
            log.warn("Le marché associé est null pour le suivi du DroitConstate ID: {}", droitConstate.getId());
            throw new ResourceNotFoundException("Le marché associé au DroitConstate ID: " + droitConstate.getId() + " est null");
        }

        if( droitConstate.getType()== Type.DEBLOCAGE){

            dto.setLibelle("Déblocage du Droit constaté du marché " + (marche != null ? marche.getNom() : null));
            dto.setCredit(droitConstate.getMontant());
            log.debug("Ligne de suivi configurée en CRÉDIT - Montant: {}", droitConstate.getMontant());

        }else {

            dto.setLibelle("Remboursement du Droit constaté du marché " + (marche != null ? marche.getNom() : null) );
            dto.setDebit(droitConstate.getMontant());
            log.debug("Ligne de suivi configurée en DÉBIT - Montant: {}", droitConstate.getMontant());
        }

        log.info("Conversion vers SuivieDcLigneResponseDto terminée avec succès pour l'ID: {}", droitConstate.getId());

        return dto;
    }
}