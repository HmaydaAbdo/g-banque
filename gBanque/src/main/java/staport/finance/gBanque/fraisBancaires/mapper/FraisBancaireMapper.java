package staport.finance.gBanque.fraisBancaires.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireRequestDto;
import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireResposneDto;
import staport.finance.gBanque.fraisBancaires.entities.FraisBancaire;

@Component
public class FraisBancaireMapper {

    // Mapper RequestDto -> Entity (création)
    public FraisBancaire toEntity(FraisBancaireRequestDto dto, Banque banque) {
        if (dto == null) {
            return null;
        }
        FraisBancaire fraisBancaire = new FraisBancaire();
        fraisBancaire.setLibelle(dto.getLibelle());
        fraisBancaire.setObservation(dto.getObservation());
        fraisBancaire.setMontant(dto.getMontant());
        fraisBancaire.setBanque(banque);
        fraisBancaire.setDateEmission(dto.getDateEmission());
        return fraisBancaire;
    }


    // Mapper Entity -> ResponseDto
    public FraisBancaireResposneDto toResponseDto(FraisBancaire entity) {
        if (entity == null) {
            return null;
        }
        FraisBancaireResposneDto dto = new FraisBancaireResposneDto();
        dto.setId(entity.getId());
        dto.setLibelle(entity.getLibelle());
        dto.setMontant(entity.getMontant());

        if (entity.getBanque() != null) {
            dto.setBanqueId(entity.getBanque().getId());
            dto.setBanqueNom(entity.getBanque().getNom());
        }

        dto.setDateEmission(entity.getDateEmission());
        dto.setStatus(entity.getStatus());
        dto.setDateOperation(entity.getDateOperation()!=null ? entity.getDateOperation() : null);
        dto.setDateValeur(entity.getDateValeur() != null ? entity.getDateValeur() : null);
        dto.setObservation(entity.getObservation()!=null ? entity.getObservation() : null);

        return dto;
    }
}
