package staport.finance.gBanque.Agios.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Agios.dtos.AgiosRequestDto;
import staport.finance.gBanque.Agios.dtos.AgiosResponseDto;
import staport.finance.gBanque.Agios.entities.Agios;
import staport.finance.gBanque.Bank.entities.Banque;

@Component
public class AgiosMapper {

    // Mapper RequestDto -> Entity (création)
    public Agios toEntity(AgiosRequestDto dto, Banque banque) {
        if (dto == null) {
            return null;
        }
        Agios agios = new Agios();
        agios.setLibelle(dto.getLibelle());
        agios.setObservation(dto.getObservation());
        agios.setMontant(dto.getMontant());
        agios.setBanque(banque);
        agios.setDateEmission(dto.getDateEmission());
        return agios;
    }


    // Mapper Entity -> ResponseDto
    public AgiosResponseDto toResponseDto(Agios entity) {
        if (entity == null) {
            return null;
        }
        AgiosResponseDto dto = new AgiosResponseDto();
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
