package staport.finance.gBanque.prefinancement.mapper;

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

    /**
     * Converts a Prefinancement entity to a PrefinancementResponseDTO.
     * This method manually maps the fields from the entity, including details
     * from the related Marche and Banque entities.
     *
     * @param prefinancement The Prefinancement entity to map.
     * @return A new PrefinancementResponseDTO instance.
     */
    public PrefinancementResponseDTO toDto(Prefinancement prefinancement) {
        if (prefinancement == null) {
            return null;
        }

        PrefinancementResponseDTO dto = new PrefinancementResponseDTO();
        dto.setId(prefinancement.getId());
        dto.setDateEmission(prefinancement.getDateEmission());
        dto.setMontant(prefinancement.getMontant());
        dto.setObservation(prefinancement.getObservation());
        dto.setType(prefinancement.getType());
        dto.setDateOperation(prefinancement.getDateOperation()!=null ? prefinancement.getDateOperation() : null);
        dto.setDateValeur(prefinancement.getDateValeur() != null ? prefinancement.getDateValeur() : null);
        dto.setStatus(prefinancement.getStatus());

        // Check if the marche and its functional lines exist to prevent NullPointerException
        Marche marche = prefinancement.getMarche();
        if (marche != null) {
            dto.setMarcheId(marche.getId());
            dto.setMarcheName(marche.getNom());
            dto.setMarcheNumero(marche.getNumero());


            if (marche.getLignesFonctionnellesSpecifiques() != null) {

                // Filter the list to find all active pre-financing lines
                List<LigneFonctionnelleSpecifique> activePrefiLines = marche.getLignesFonctionnellesSpecifiques().stream()
                        .filter(ligne -> ligne.isActif() && "Préfinancement".equals(ligne.getNatureLigneFonctionnelle().getLabel()))
                        .toList();

                // Defensive programming check: enforce the business rule of a single active line
                if (activePrefiLines.size() > 1) {
                    throw new IllegalStateException("The market with ID " + marche.getId() + " has more than one active 'Préfinancement' functional line.");
                }

                // Check if there is exactly one active pre-financing line and retrieve the bank information
                if (activePrefiLines.size() == 1) {
                    LigneFonctionnelleSpecifique activeLigne = activePrefiLines.get(0);
                    dto.setBanqueId(activeLigne.getBanque().getId());
                    dto.setBanqueName(activeLigne.getBanque().getNom());

                }
            }
        }

        if( prefinancement.getType()== Type.DEBLOCAGE){
            dto.setLibelle("Déblocage du préfinancement du marché " + (marche != null ? marche.getNom() : null));
        }else {
            dto.setLibelle("Remboursement du préfinancement du marché " + (marche != null ? marche.getNom() : null) + "");
        }
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
        if (requestDTO == null) {
            return null;
        }

        Prefinancement entity = new Prefinancement();
        entity.setDateEmission(requestDTO.getDateEmission());
        entity.setMontant(requestDTO.getMontant());
        entity.setObservation("Marche: " + marche.getNumero());
        entity.setType(requestDTO.getType());
        entity.setMarche(marche);
        entity.setStatus(Status.PREVU);

        return entity;
    }

    public SuiviePrefiLigneResponseDto toSuiviePrefiLigneResponseDto(Prefinancement prefinancement){

        if (prefinancement == null) {
            return null;
        }
        SuiviePrefiLigneResponseDto dto = new SuiviePrefiLigneResponseDto();
        dto.setId(prefinancement.getId());
        dto.setDateEmission(prefinancement.getDateEmission());
        dto.setDateOperation(prefinancement.getDateOperation()!=null ? prefinancement.getDateOperation() : null);
        dto.setDateValeur(prefinancement.getDateValeur() != null ? prefinancement.getDateValeur() : null);

        // Check if the marche and its functional lines exist to prevent NullPointerException
        Marche marche = prefinancement.getMarche();
        if (marche != null) {
            dto.setMarcheName(marche.getNom());
            dto.setMarcheNumero(marche.getNumero());


            if (marche.getLignesFonctionnellesSpecifiques() != null) {

                // Filter the list to find all active pre-financing lines
                List<LigneFonctionnelleSpecifique> activePrefiLines = marche.getLignesFonctionnellesSpecifiques().stream()
                        .filter(ligne -> ligne.isActif() && "Préfinancement".equals(ligne.getNatureLigneFonctionnelle().getLabel()))
                        .toList();

                // Defensive programming check: enforce the business rule of a single active line
                if (activePrefiLines.size() > 1) {
                    throw new IllegalStateException("The market with ID " + marche.getId() + " has more than one active 'Préfinancement' functional line.");
                }

                // Check if there is exactly one active pre-financing line and retrieve the bank information
                if (activePrefiLines.size() == 1) {
                    LigneFonctionnelleSpecifique activeLigne = activePrefiLines.get(0);
                    dto.setBanqueName(activeLigne.getBanque().getNom());

                }
            }
        }
        if( prefinancement.getType()== Type.DEBLOCAGE){
            dto.setLibelle("Déblocage du préfinancement du marché " + (marche != null ? marche.getNom() : null));
            dto.setCredit(prefinancement.getMontant());
        }else {
            dto.setLibelle("Remboursement du préfinancement du marché " + (marche != null ? marche.getNom() : null) );
            dto.setDebit(prefinancement.getMontant());
        }

        dto.setObservation(prefinancement.getObservation());
        dto.setType(prefinancement.getType());
        dto.setStatus(prefinancement.getStatus());
        return dto;
    }
}
