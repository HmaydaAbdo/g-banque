package staport.finance.gBanque.lcn.mapper;

import staport.finance.gBanque.lcn.dtos.LcnResponseDTO;
import staport.finance.gBanque.lcn.entities.Lcn;


public class LcnMapper {


    /**
     * Maps an Lcn entity to an LcnResponseDTO.
     * This method is crucial for decoupling the internal entity from the external API representation.
     * @param lcn The Lcn entity to map.
     * @return The corresponding LcnResponseDTO.
     */
    public static LcnResponseDTO toDto(Lcn lcn) {
        if (lcn == null) {
            return null;
        }

        LcnResponseDTO dto = new LcnResponseDTO();
        dto.setNumero(lcn.getNumero());
        dto.setDateEmission(lcn.getDateEmission());
        dto.setBeneficiaire(lcn.getBeneficiaire());
        dto.setDateEcheance(lcn.getDateEcheance());
        dto.setMontant(lcn.getMontant());
        dto.setActive(lcn.isActive());

        // We only expose the bank's name, not the whole Banque object.
        if (lcn.getBanque() != null) {
            dto.setBanqueName(lcn.getBanque().getNom());
        }

        dto.setDateOperation(lcn.getDateOperation()!=null ? lcn.getDateOperation() : null);
        dto.setDateValeur(lcn.getDateValeur() !=null ? lcn.getDateValeur() : null);
        dto.setObservation("LCN N° " + lcn.getNumero());
        dto.setLibelle( "Bénéficiaire : " + lcn.getBeneficiaire());

        return dto;
    }

}
