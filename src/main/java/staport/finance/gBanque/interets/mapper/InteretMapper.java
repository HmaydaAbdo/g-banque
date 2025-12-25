package staport.finance.gBanque.interets.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.interets.dtos.InteretRequestDto;
import staport.finance.gBanque.interets.dtos.InteretResponseDto;
import staport.finance.gBanque.interets.entities.Interet;

@Component
public class InteretMapper {


    // Mapper RequestDto -> Entity (création)
    public Interet toEntity(InteretRequestDto dto, Banque banque) {
        if (dto == null) {
            return null;
        }
        Interet interet = new Interet();
        interet.setLibelle(dto.getLibelle());
        interet.setObservation(dto.getObservation());
        interet.setMontant(dto.getMontant());
        interet.setBanque(banque);
        interet.setDateEmission(dto.getDateEmission());
        return interet;
    }


    // Mapper Entity -> ResponseDto
    public InteretResponseDto toResponseDto(Interet entity) {
        if (entity == null) {
            return null;
        }
        InteretResponseDto dto = new InteretResponseDto();
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
