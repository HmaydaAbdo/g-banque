package staport.finance.gBanque.miseADisposition.mapper;

import staport.finance.gBanque.miseADisposition.dtos.MiseADispositionResponseDto;
import staport.finance.gBanque.miseADisposition.entities.MiseADisposition;

public class MiseADispositionMapper {

    /**
     * Converts a MiseADisposition entity to a MiseADispositionResponseDto.
     *
     * @param miseADisposition The MiseADisposition entity to convert.
     * @return The corresponding MiseADispositionResponseDto.
     */
    public static MiseADispositionResponseDto toDto(MiseADisposition miseADisposition) {
        if (miseADisposition == null) {
            return null;
        }

        MiseADispositionResponseDto dto = new MiseADispositionResponseDto();
        dto.setNumero(miseADisposition.getNumero());
        dto.setDateEmission(miseADisposition.getDateEmission());
        dto.setBeneficiaire(miseADisposition.getBeneficiaire());
        dto.setMontant(miseADisposition.getMontant());
        dto.setCinBeneficiaire(miseADisposition.getCinBeneficiaire());
        dto.setActive(miseADisposition.isActive());

        if (miseADisposition.getBanque() != null) {
            dto.setBanqueName(miseADisposition.getBanque().getNom());
        }

        dto.setDateOperation(miseADisposition.getDateOperation() !=null ? miseADisposition.getDateOperation() : null);
        dto.setDateValeur(miseADisposition.getDateValeur() !=null ? miseADisposition.getDateValeur() : null);
        dto.setObservation("Mise à disposition N° " + miseADisposition.getNumero());
        dto.setLibelle("Bénéficiaire " + miseADisposition.getBeneficiaire() );

        return dto;
    }
}