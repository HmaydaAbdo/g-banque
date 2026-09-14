package staport.finance.gBanque.commision.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import staport.finance.gBanque.commision.dtos.CommissionConfigRequestDTO;
import staport.finance.gBanque.commision.dtos.CommissionConfigResponseDTO;
import staport.finance.gBanque.commision.entities.CommissionConfig;

@Mapper(componentModel = "spring")
public interface CommissionConfigMapper {

    /** L'association banque est résolue par le service (IBanqueService.findOrThrow). */
    @Mapping(target = "banque", ignore = true)
    CommissionConfig toEntity(CommissionConfigRequestDTO dto);

    @Mapping(source = "banque.id",  target = "banqueId")
    @Mapping(source = "banque.nom", target = "banqueNom")
    CommissionConfigResponseDTO toResponseDTO(CommissionConfig entity);

    /** L'association banque est résolue par le service (IBanqueService.findOrThrow). */
    @Mapping(target = "banque", ignore = true)
    void updateEntity(CommissionConfigRequestDTO dto, @MappingTarget CommissionConfig entity);
}
