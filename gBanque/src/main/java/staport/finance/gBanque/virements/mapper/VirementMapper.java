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

        VirementResponseDto response = new VirementResponseDto();
        response.setNumero(virement.getNumero());
        response.setBeneficiaire(virement.getBeneficiaire());
        response.setDateEmission(virement.getDateEmission());
        response.setMontant(virement.getMontant());
        response.setBanqueEmettriceNom(virement.getBanque() != null ? virement.getBanque().getNom() : null);
        response.setBanqueBeneficiaire(virement.getBanqueBeneficiaire());
        response.setRibBeneficiaire(virement.getRibBeneficiaire());
        response.setActive(virement.isActive());
        response.setDateOperation(virement.getDateOperation()!=null ? virement.getDateOperation() : null);
        response.setDateValeur(virement.getDateValeur()!=null ? virement.getDateValeur() : null);
        response.setObservation("Virement N° " + virement.getNumero());
        response.setLibelle("Bénéficiaire " + virement.getBeneficiaire() );
        return response;
    }
}