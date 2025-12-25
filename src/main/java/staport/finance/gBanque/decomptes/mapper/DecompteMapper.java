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



    public Decompte toEntity(DecompteRequestDto requestDTO , Marche marche) {
        if (requestDTO == null) {
            return null;
        }

        Decompte entity = new Decompte();

        entity.setMarche(marche);

        entity.setDateEmission(requestDTO.getDateEmission());
        entity.setMontant(requestDTO.getMontant());
        entity.setLibelle(requestDTO.getLibelle());
        entity.setObservation( marche.getNumero()  );
        entity.setStatus(Status.PREVU);

        return entity;
    }


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

        dto.setLibelle(decompte.getLibelle() );
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



        return dto;
    }

}
