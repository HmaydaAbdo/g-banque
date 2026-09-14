package staport.finance.gBanque.cautions.commissions.config.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import staport.finance.gBanque.cautions.commissions.config.dtos.request.CautionCommissionConfigRequestDTO;
import staport.finance.gBanque.cautions.commissions.config.dtos.response.CautionCommissionConfigResponseDTO;
import staport.finance.gBanque.cautions.commissions.config.entities.CautionCommissionConfig;

@Mapper(componentModel = "spring")
public interface CautionCommissionConfigMapper {

    /** L'association banque est résolue par le service (IBanqueService.findOrThrow). */
    @Mapping(target = "banque", ignore = true)
    CautionCommissionConfig toEntity(CautionCommissionConfigRequestDTO dto);

    @Mapping(source = "banque.id",  target = "banqueId")
    @Mapping(source = "banque.nom", target = "banqueNom")
    CautionCommissionConfigResponseDTO toResponseDTO(CautionCommissionConfig entity);

    /** L'association banque est résolue par le service (IBanqueService.findOrThrow). */
    @Mapping(target = "banque", ignore = true)
    void updateEntity(CautionCommissionConfigRequestDTO dto, @MappingTarget CautionCommissionConfig entity);
}
