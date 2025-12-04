package staport.finance.gBanque.droitConstate.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.droitConstate.dtos.DroitConstateRequestDto;
import staport.finance.gBanque.droitConstate.dtos.DroitConstateResponseDto;
import staport.finance.gBanque.droitConstate.dtos.SuivieDcLigneResponseDto;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

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
        entity.setObservation( marche.getNumero());
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
            dto.setBanqueId(marche.getBanque().getId());
            dto.setBanqueName(marche.getBanque().getNom());
        }
        if( droitConstate.getType()== Type.DEBLOCAGE){
            dto.setLibelle("Déblocage  droit constaté  marché " + (marche != null ? marche.getNom() : null));
        }else {
            dto.setLibelle("Remboursement  droit constaté  marché " + (marche != null ? marche.getNom() : null) + "");
        }

        return dto;
    }

    public SuivieDcLigneResponseDto toSuivieDcLigneResponseDto(DroitConstate droitConstate){

        if (droitConstate == null) {
            return null;
        }
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

            dto.setMarcheName(marche.getNom());
            dto.setMarcheNumero(marche.getNumero());
            dto.setBanqueName(marche.getBanque().getNom());

        }

        if( droitConstate.getType()== Type.DEBLOCAGE){

            dto.setLibelle("Déblocage du Droit constaté du marché " + (marche != null ? marche.getNom() : null));
            dto.setCredit(droitConstate.getMontant());

        }else {

            dto.setLibelle("Remboursement du Droit constaté du marché " + (marche != null ? marche.getNom() : null) );
            dto.setDebit(droitConstate.getMontant());
        }


        return dto;
    }
}
