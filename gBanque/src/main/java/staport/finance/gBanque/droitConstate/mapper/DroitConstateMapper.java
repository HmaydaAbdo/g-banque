package staport.finance.gBanque.droitConstate.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.droitConstate.dtos.DroitConstateRequestDto;
import staport.finance.gBanque.droitConstate.dtos.DroitConstateResponseDto;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Status;
import java.util.List;

@Component
public class DroitConstateMapper {

    /**
     * Converts a DroitConstateRequestDto to a DroitConstate entity.
     * This method maps the basic fields from the DTO to the entity and sets the
     * related Marche entity passed as argument.
     *
     * @param requestDTO The DroitConstateRequestDto to map.
     * @param marche The Marche entity fetched from the database.
     * @return A new DroitConstate entity instance with relationships set.
     */
    public DroitConstate toEntity(DroitConstateRequestDto requestDTO , Marche marche) {
        if (requestDTO == null) {
            return null;
        }

        DroitConstate entity = new DroitConstate();
        entity.setDateEmission(requestDTO.getDateEmission());
        entity.setMontant(requestDTO.getMontant());
        entity.setObservation("Marche: " + marche.getNumero());
        entity.setType(requestDTO.getType());
        entity.setMarche(marche);
        entity.setStatus(Status.PREVU);

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
        if (droitConstate == null) {
            return null;
        }

        DroitConstateResponseDto dto = new DroitConstateResponseDto();
        dto.setId(droitConstate.getId());
        dto.setDateEmission(droitConstate.getDateEmission());
        dto.setMontant(droitConstate.getMontant());
        dto.setObservation(droitConstate.getObservation());
        dto.setType(droitConstate.getType());
        dto.setDateOperation(droitConstate.getDateOperation()!=null ? droitConstate.getDateOperation() : null);
        dto.setDateValeur(droitConstate.getDateValeur() != null ? droitConstate.getDateValeur() : null);
        dto.setStatus(droitConstate.getStatus());

        // Check if the marche and its functional lines exist to prevent NullPointerException
        Marche marche = droitConstate.getMarche();
        if (marche != null) {
            dto.setMarcheId(marche.getId());
            dto.setMarcheName(marche.getNom());
            dto.setMarcheNumero(marche.getNumero());


            if (marche.getLignesFonctionnellesSpecifiques() != null) {

                // Filter the list to find all active DC  lines
                List<LigneFonctionnelleSpecifique> droitContateesLines = marche.getLignesFonctionnellesSpecifiques().stream()
                        .filter(ligne -> ligne.isActif() && "Droit constaté".equals(ligne.getNatureLigneFonctionnelle().getLabel()))
                        .toList();

                // Defensive programming check: enforce the business rule of a single active line
                if (droitContateesLines.size() > 1) {
                    throw new IllegalStateException("The market with ID " + marche.getId() + " has more than one active 'Droit constaté' functional line.");
                }

                // Check if there is exactly one active DC line and retrieve the bank information
                if (droitContateesLines.size() == 1) {
                    LigneFonctionnelleSpecifique activeLigne = droitContateesLines.get(0);
                    dto.setBanqueId(activeLigne.getBanque().getId());
                    dto.setBanqueName(activeLigne.getBanque().getNom());

                }
            }
        }

        return dto;
    }

}
