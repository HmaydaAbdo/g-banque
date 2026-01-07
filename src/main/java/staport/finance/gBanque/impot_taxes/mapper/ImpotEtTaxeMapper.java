
package staport.finance.gBanque.impot_taxes.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.impot_taxes.dtos.ImpotEtTaxeRequestDto;
import staport.finance.gBanque.impot_taxes.dtos.ImpotEtTaxeResponseDto;
import staport.finance.gBanque.impot_taxes.entities.ImpotEtTaxe;

@Component
public class ImpotEtTaxeMapper {

    // Mapper RequestDto -> Entity (création)
    public ImpotEtTaxe toEntity(ImpotEtTaxeRequestDto dto, Banque banque) {
        if (dto == null) {
            return null;
        }
        ImpotEtTaxe impotEtTaxe = new ImpotEtTaxe();
        impotEtTaxe.setLibelle(dto.getLibelle());
        impotEtTaxe.setObservation(dto.getObservation());
        impotEtTaxe.setMontant(dto.getMontant());
        impotEtTaxe.setBanque(banque);
        impotEtTaxe.setMotif(dto.getMotif());
        impotEtTaxe.setDateEmission(dto.getDateEmission());
        return impotEtTaxe;
    }

    // Mapper Entity -> ResponseDto
    public ImpotEtTaxeResponseDto toResponseDto(ImpotEtTaxe entity) {
        if (entity == null) {
            return null;
        }
        ImpotEtTaxeResponseDto dto = new ImpotEtTaxeResponseDto();
        dto.setId(entity.getId());
        dto.setLibelle(entity.getLibelle());
        dto.setObservation(entity.getObservation());
        dto.setMotif(entity.getMotif());
        dto.setMontant(entity.getMontant());
        dto.setDateEmission(entity.getDateEmission());
        dto.setStatus(entity.getStatus());

        if (entity.getBanque() != null) {
            dto.setBanqueId(entity.getBanque().getId());
            dto.setBanqueNom(entity.getBanque().getNom());
        }


        dto.setDateOperation(entity.getDateOperation() != null ? entity.getDateOperation() : null);
        dto.setDateValeur(entity.getDateValeur() != null ? entity.getDateValeur() : null);


        return dto;
    }
}