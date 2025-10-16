package staport.finance.gBanque.factoring.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.factoring.dtos.FactoringRequestDto;
import staport.finance.gBanque.factoring.dtos.FactoringResponseDto;
import staport.finance.gBanque.factoring.dtos.SuivieFactoringLineResponseDto;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

@Component
public class FactoringMapper {


    /**
     * Converts a Factoring entity to a FactoringResponseDto.
     * This method manually maps the fields from the entity, including details
     * from the related Marche and Banque entities.
     *
     * @param factoring The Factoring entity to map.
     * @return A new FactoringResponseDto instance.
     */
    public FactoringResponseDto toDto(Factoring factoring) {

        if (factoring == null) {
            return null;
        }

        FactoringResponseDto dto = new FactoringResponseDto();
        dto.setId(factoring.getId());
        dto.setDateEmission(factoring.getDateEmission());
        dto.setMontant(factoring.getMontant());
        dto.setObservation(factoring.getObservation());
        dto.setType(factoring.getType());
        dto.setDateOperation(factoring.getDateOperation()!=null ? factoring.getDateOperation() : null);
        dto.setDateValeur(factoring.getDateValeur() != null ? factoring.getDateValeur() : null);
        dto.setStatus(factoring.getStatus());

        // Check if the marche and its functional lines exist to prevent NullPointerException
        Marche marche = factoring.getMarche();
        if (marche != null) {
            dto.setMarcheId(marche.getId());
            dto.setMarcheName(marche.getNom());
            dto.setMarcheNumero(marche.getNumero());
            dto.setBanqueId(marche.getBanque().getId());
            dto.setBanqueName(marche.getBanque().getNom());
            if( factoring.getType()== Type.DEBLOCAGE){
                dto.setLibelle("Déblocage du factoring pour le  marché " +  marche.getNom() );
            }else {
                dto.setLibelle("Remboursement du factoring pour le marché " + marche.getNom()  );
            }
        }


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

        if (requestDTO == null) {
            return null;
        }

        Factoring entity = new Factoring();
        entity.setDateEmission(requestDTO.getDateEmission());
        entity.setMontant(requestDTO.getMontant());
        entity.setObservation("Marche: " + marche.getNumero());
        entity.setType(requestDTO.getType());
        entity.setMarche(marche);
        entity.setStatus(Status.PREVU);

        return entity;
    }

    public SuivieFactoringLineResponseDto toSuivieFactoringLigneResponseDto(Factoring factoring){

        if (factoring == null) {
            return null;
        }

        SuivieFactoringLineResponseDto dto = new SuivieFactoringLineResponseDto();
        dto.setId(factoring.getId());
        dto.setDateEmission(factoring.getDateEmission());
        dto.setDateOperation(factoring.getDateOperation()!=null ? factoring.getDateOperation() : null);
        dto.setDateValeur(factoring.getDateValeur() != null ? factoring.getDateValeur() : null);

        // Check if the marche and its functional lines exist to prevent NullPointerException
        Marche marche = factoring.getMarche();
        if (marche != null) {
            dto.setMarcheName(marche.getNom());
            dto.setMarcheNumero(marche.getNumero());
            dto.setBanqueName(marche.getBanque().getNom());
            if( factoring.getType()== Type.DEBLOCAGE){
                dto.setLibelle("Déblocage du factoring pour le marché " + marche.getNom());
                dto.setCredit(factoring.getMontant());
            }else {
                dto.setLibelle("Remboursement du factoring pour le marché " +  marche.getNom() );
                dto.setDebit(factoring.getMontant());
            }
        }

        dto.setObservation(factoring.getObservation());
        dto.setType(factoring.getType());
        dto.setStatus(factoring.getStatus());
        return dto;
    }
}
