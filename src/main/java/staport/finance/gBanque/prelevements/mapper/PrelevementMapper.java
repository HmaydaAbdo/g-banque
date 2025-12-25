package staport.finance.gBanque.prelevements.mapper;

import org.springframework.stereotype.Component;

import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.prelevements.dtos.PrelevementRequestDto;
import staport.finance.gBanque.prelevements.dtos.PrelevementResponseDto;
import staport.finance.gBanque.prelevements.entitie.Prelevement;

@Component
public class PrelevementMapper {

    // Mapper RequestDto -> Entity (création)
    public Prelevement toEntity(PrelevementRequestDto dto, Banque banque) {
        if (dto == null) {
            return null;
        }
        Prelevement prelevement = new Prelevement();
        prelevement.setLibelle(dto.getLibelle());
        prelevement.setObservation(dto.getObservation());
        prelevement.setMontant(dto.getMontant());
        prelevement.setBanque(banque);
        prelevement.setDateEmission(dto.getDateEmission());
        return prelevement;
    }


    // Mapper Entity -> ResponseDto
    public PrelevementResponseDto toResponseDto(Prelevement entity) {
        if (entity == null) {
            return null;
        }
        PrelevementResponseDto dto = new PrelevementResponseDto();
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
