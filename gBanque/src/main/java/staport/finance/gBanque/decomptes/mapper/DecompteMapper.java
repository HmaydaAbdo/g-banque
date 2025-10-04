package staport.finance.gBanque.decomptes.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.decomptes.dto.DecompteRequestDto;
import staport.finance.gBanque.decomptes.dto.DecompteResponseDto;
import staport.finance.gBanque.decomptes.entities.Decompte;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Status;



@Component
public class DecompteMapper {


    /**
     * Converts a DecompteRequestDto to a Decompte entity.
     * This method maps the basic fields from the DTO to the entity and sets the
     * related Marche and banque entities are passed as argument.
     *
     * @param requestDTO The DroitConstateRequestDto to map.
     * @param marche The Marche entity fetched from the database.
     * @return A new DroitConstate entity instance with relationships set.
     */
    public Decompte toEntity(DecompteRequestDto requestDTO , Marche marche) {
        if (requestDTO == null) {
            return null;
        }

        Decompte entity = new Decompte();

        entity.setMarche(marche);

        entity.setDateEmission(requestDTO.getDateEmission());
        entity.setMontant(requestDTO.getMontant());

        entity.setObservation("decompte , Marché N° : " + marche.getNumero()  );
        entity.setStatus(Status.PREVU);

        return entity;
    }


    /**
     * Converts a Decompte entity to a DecompteResponseDto..
     * This method manually maps the fields from the entity, including details
     * from the related Marche and Banque entities.
     *
     * @param decompte The decompte entity to map.
     * @return A new DecompteResponseDto instance.
     */
    public DecompteResponseDto toDto(Decompte decompte) {
        if (decompte == null) {
            return null;
        }

        DecompteResponseDto dto = new DecompteResponseDto();
        dto.setId(decompte.getId());
        dto.setDateEmission(decompte.getDateEmission());
        dto.setMontant(decompte.getMontant());
        dto.setObservation(decompte.getObservation());
        dto.setDateOperation(decompte.getDateOperation()!=null ? decompte.getDateOperation() : null);
        dto.setDateValeur(decompte.getDateValeur() != null ? decompte.getDateValeur() : null);
        dto.setStatus(decompte.getStatus());

        // Check if the marche and its functional lines exist to prevent NullPointerException
        Marche marche = decompte.getMarche();

        if (marche != null) {
            dto.setMarcheId(marche.getId());
            dto.setMarcheName(marche.getNom());
            dto.setMarcheNumero(marche.getNumero());
        }

        Banque banque = decompte.getMarche().getBanque();
        if (banque != null) {
            dto.setBanqueId(banque.getId());
            dto.setBanqueName(banque.getNom());
        }

        dto.setLibelle("Marché N° : " + decompte.getMarche().getNumero() );

        return dto;
    }

}
