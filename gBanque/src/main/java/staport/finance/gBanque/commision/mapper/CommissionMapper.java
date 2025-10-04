package staport.finance.gBanque.commision.mapper;


import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.commision.dtos.CommissionRequestDto;
import staport.finance.gBanque.commision.dtos.CommissionResponseDto;
import staport.finance.gBanque.commision.entities.Commission;

@Component
public class CommissionMapper {

    // Mapper RequestDto -> Entity (création)
    public  Commission toEntity(CommissionRequestDto dto, Banque banque) {
        if (dto == null) {
            return null;
        }
        Commission commission = new Commission();
        commission.setLibelle(dto.getLibelle());
        commission.setMontant(dto.getMontant());
        commission.setBanque(banque);
        commission.setOperation(dto.getOperation());
        commission.setDateEmission(dto.getDateEmission());
        return commission;
    }


    // Mapper Entity -> ResponseDto
    public  CommissionResponseDto toResponseDto(Commission entity) {
        if (entity == null) {
            return null;
        }
        CommissionResponseDto dto = new CommissionResponseDto();
        dto.setId(entity.getId());
        dto.setLibelle(entity.getLibelle());
        dto.setOperation(entity.getOperation());
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
