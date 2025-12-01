package staport.finance.gBanque.miseADisposition.mapper;

import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.miseADisposition.dtos.MiseADispositionResponseDto;
import staport.finance.gBanque.miseADisposition.dtos.MiseAdispositionRequestDto;
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
        dto.setId(miseADisposition.getId());

        dto.setNumero(miseADisposition.getNumero() != null ? miseADisposition.getNumero() : null);
        dto.setDateEmission(miseADisposition.getDateEmission());
        dto.setBeneficiaire(miseADisposition.getBeneficiaire());
        dto.setMontant(miseADisposition.getMontant());
        dto.setCinBeneficiaire(miseADisposition.getCinBeneficiaire());
        dto.setActive(miseADisposition.isActive());
        dto.setType(miseADisposition.getType());
        dto.setEmetteur(miseADisposition.getEmetteur() !=null ? miseADisposition.getEmetteur() : null);

        if (miseADisposition.getBanque() != null) {
            dto.setBanqueName(miseADisposition.getBanque().getNom());
            dto.setBanqueId(miseADisposition.getBanque().getId());
        }

        dto.setDateOperation(miseADisposition.getDateOperation() !=null ? miseADisposition.getDateOperation() : null);
        dto.setDateValeur(miseADisposition.getDateValeur() !=null ? miseADisposition.getDateValeur() : null);
        dto.setObservation("Mise à disposition N° " + miseADisposition.getNumero());
        dto.setLibelle("Bénéficiaire " + miseADisposition.getBeneficiaire() );

        return dto;
    }

    public  static MiseADisposition toEntity(MiseAdispositionRequestDto dto, Banque banque) {
        MiseADisposition miseADisposition = new MiseADisposition();

        miseADisposition.setDateEmission(dto.getDateEmission());
        miseADisposition.setBeneficiaire(dto.getBeneficiaire());
        miseADisposition.setMontant(dto.getMontant());
        miseADisposition.setBanque(banque);
        miseADisposition.setActive(true);
        miseADisposition.setType(dto.getType());
        miseADisposition.setEmetteur(dto.getEmetteur());

        return miseADisposition;
    }
}