package staport.finance.gBanque.prefinancement.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.dtos.PrefinancementRequestDTO;
import staport.finance.gBanque.prefinancement.dtos.PrefinancementResponseDTO;
import staport.finance.gBanque.prefinancement.dtos.SuiviePrefiLigneResponseDto;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

import java.util.List;


/**
 * Component for manually mapping between Prefinancement entity and its DTOs.
 */
@Component
public class PrefinancementMapper {

    private static final Logger log = LoggerFactory.getLogger(PrefinancementMapper.class);

    /**
     * Converts a Prefinancement entity to a PrefinancementResponseDTO.
     * This method manually maps the fields from the entity, including details
     * from the related Marche and Banque entities.
     *
     * @param prefinancement The Prefinancement entity to map.
     * @return A new PrefinancementResponseDTO instance.
     */
    public PrefinancementResponseDTO toDto(Prefinancement prefinancement) {
        log.debug("Starting mapping Prefinancement entity to PrefinancementResponseDTO");

        if (prefinancement == null) {
            log.warn("Prefinancement entity is null, returning null");
            return null;
        }

        log.debug("Mapping Prefinancement with ID: {}", prefinancement.getId());

        PrefinancementResponseDTO dto = new PrefinancementResponseDTO();
        dto.setId(prefinancement.getId());
        dto.setDateEmission(prefinancement.getDateEmission());
        dto.setMontant(prefinancement.getMontant());

        dto.setType(prefinancement.getType());
        dto.setDateOperation(prefinancement.getDateOperation()!=null ? prefinancement.getDateOperation() : null);
        dto.setDateValeur(prefinancement.getDateValeur() != null ? prefinancement.getDateValeur() : null);
        dto.setStatus(prefinancement.getStatus());
        dto.setObservation("Marche N° " + prefinancement.getMarche().getNumero() );

        // Check if the marche and its functional lines exist to prevent NullPointerException
        Marche marche = prefinancement.getMarche();
        if (marche != null) {
            log.debug("Mapping Marche details - Marche ID: {}, Numero: {}", marche.getId(), marche.getNumero());

            dto.setMarcheId(marche.getId());
            dto.setMarcheName(marche.getNom());
            dto.setMarcheNumero(marche.getNumero());
            dto.setBanqueId(marche.getBanque().getId());
            dto.setBanqueName(marche.getBanque().getNom());

            if( prefinancement.getType()== Type.DEBLOCAGE){
                log.debug("Setting libelle for DEBLOCAGE type");
                dto.setLibelle("Déblocage  préfinancement  marché " +  marche.getNom() );
            }else {
                log.debug("Setting libelle for REMBOURSEMENT type");
                dto.setLibelle("Remboursement  préfinancement  marché " + marche.getNom()  );
            }
        } else {
            log.warn("Marche is null for Prefinancement ID: {}", prefinancement.getId());
            throw new NullPointerException("Marche is null for Prefinancement ID: " + prefinancement.getId());
        }

        log.debug("Successfully mapped Prefinancement to DTO");
        return dto;
    }

    /**
     * Converts a PrefinancementRequestDTO to a Prefinancement entity.
     * This method maps the basic fields from the DTO to the entity and sets the
     * related Marche and Banque entities passed as arguments.
     *
     * @param requestDTO The PrefinancementRequestDTO to map.
     * @param marche The Marche entity fetched from the database.
     * @return A new Prefinancement entity instance with relationships set.
     */
    public Prefinancement toEntity(PrefinancementRequestDTO requestDTO , Marche marche) {
        log.debug("Starting mapping PrefinancementRequestDTO to Prefinancement entity");

        if (requestDTO == null) {
            log.warn("PrefinancementRequestDTO is null, returning null");
            return null;
        }

        log.debug("Mapping PrefinancementRequestDTO - Marche ID: {}, Type: {}, Montant: {}",
                requestDTO.getMarcheId(), requestDTO.getType(), requestDTO.getMontant());

        Prefinancement entity = new Prefinancement();
        entity.setDateEmission(requestDTO.getDateEmission());
        entity.setMontant(requestDTO.getMontant());
        entity.setObservation( marche.getNumero());
        entity.setType(requestDTO.getType());
        entity.setMarche(marche);
        entity.setStatus(Status.PREVU);

        log.debug("Successfully mapped RequestDTO to Prefinancement entity");
        return entity;
    }

    public SuiviePrefiLigneResponseDto toSuiviePrefiLigneResponseDto(Prefinancement prefinancement){
        log.debug("Starting mapping Prefinancement to SuiviePrefiLigneResponseDto");

        if (prefinancement == null) {
            log.warn("Prefinancement is null, returning null");
            return null;
        }

        log.debug("Mapping Prefinancement with ID: {} to SuiviePrefiLigneResponseDto", prefinancement.getId());

        SuiviePrefiLigneResponseDto dto = new SuiviePrefiLigneResponseDto();
        dto.setId(prefinancement.getId());
        dto.setDateEmission(prefinancement.getDateEmission());
        dto.setDateOperation(prefinancement.getDateOperation()!=null ? prefinancement.getDateOperation() : null);
        dto.setDateValeur(prefinancement.getDateValeur() != null ? prefinancement.getDateValeur() : null);

        // Check if the marche and its functional lines exist to prevent NullPointerException
        Marche marche = prefinancement.getMarche();
        if (marche != null) {
            log.debug("Mapping Marche details for SuiviePrefi - Marche: {}, Numero: {}",
                    marche.getNom(), marche.getNumero());

            dto.setMarcheName(marche.getNom());
            dto.setMarcheNumero(marche.getNumero());
            dto.setBanqueName(marche.getBanque().getNom());

            if( prefinancement.getType()== Type.DEBLOCAGE){
                log.debug("Setting DEBLOCAGE details - Credit: {}", prefinancement.getMontant());
                dto.setLibelle("Déblocage du préfinancement du marché " + marche.getNom());
                dto.setCredit(prefinancement.getMontant());
            }else {
                log.debug("Setting REMBOURSEMENT details - Debit: {}", prefinancement.getMontant());
                dto.setLibelle("Remboursement du préfinancement du marché " +  marche.getNom() );
                dto.setDebit(prefinancement.getMontant());
            }
        } else {
            log.warn("Marche is null for Prefinancement ID: {}", prefinancement.getId());
        }

        dto.setObservation(prefinancement.getObservation());
        dto.setType(prefinancement.getType());
        dto.setStatus(prefinancement.getStatus());

        log.debug("Successfully mapped Prefinancement to SuiviePrefiLigneResponseDto");
        return dto;
    }
}