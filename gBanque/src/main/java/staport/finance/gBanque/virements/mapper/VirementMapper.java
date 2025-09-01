package staport.finance.gBanque.virements.mapper;

import staport.finance.gBanque.virements.dtos.VirementResponseDto;
import staport.finance.gBanque.virements.entities.Virement;

public class VirementMapper {

    /**
     * Converts a Virement entity to a VirementResponseDto.
     *
     * @param virement The Virement entity to convert.
     * @return The corresponding VirementResponseDto.
     */
    public static VirementResponseDto toDto(Virement virement) {
        if (virement == null) {
            return null;
        }

        return new VirementResponseDto(
                virement.getNumero(),
                virement.getBeneficiaire(),
                virement.getDateEmission(),
                virement.getMontant(),
                virement.getBanque() != null ? virement.getBanque().getNom() : null,
                virement.getBanqueBeneficiaire(),
                virement.getRibBeneficiaire(),
                virement.isActive()
        );
    }
}