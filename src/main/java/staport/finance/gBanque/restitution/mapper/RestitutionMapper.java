package staport.finance.gBanque.restitution.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.restitution.dtos.RestitutionRequestDto;
import staport.finance.gBanque.restitution.dtos.RestitutionResponseDto;
import staport.finance.gBanque.restitution.entities.Restitution;

@Component
public class RestitutionMapper {

    // Mapper RequestDto -> Entity (création)
    public Restitution toEntity(RestitutionRequestDto dto, Banque banque) {
        if (dto == null) {
            return null;
        }
        Restitution restitution = new Restitution();
        restitution.setLibelle(dto.getLibelle());
        restitution.setObservation(dto.getObservation());
        restitution.setMontant(dto.getMontant());
        restitution.setBanque(banque);
        restitution.setDateEmission(dto.getDateEmission());
        return restitution;
    }

    // Mapper Entity -> ResponseDto
    public RestitutionResponseDto toResponseDto(Restitution entity) {
        if (entity == null) {
            return null;
        }
        RestitutionResponseDto dto = new RestitutionResponseDto();
        dto.setId(entity.getId());
        dto.setLibelle(entity.getLibelle());
        dto.setMontant(entity.getMontant());

        if (entity.getBanque() != null) {
            dto.setBanqueId(entity.getBanque().getId());
            dto.setBanqueNom(entity.getBanque().getNom());
        }

        dto.setDateEmission(entity.getDateEmission());
        dto.setStatus(entity.getStatus());
        dto.setDateOperation(entity.getDateOperation() != null ? entity.getDateOperation() : null);
        dto.setDateValeur(entity.getDateValeur() != null ? entity.getDateValeur() : null);
        dto.setObservation(entity.getObservation() != null ? entity.getObservation() : null);

        return dto;
    }
}