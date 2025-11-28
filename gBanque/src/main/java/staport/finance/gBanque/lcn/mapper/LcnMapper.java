package staport.finance.gBanque.lcn.mapper;

import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.cheques.entities.Cheque;
import staport.finance.gBanque.lcn.dtos.LcnRequestDto;
import staport.finance.gBanque.lcn.dtos.LcnResponseDTO;
import staport.finance.gBanque.lcn.entities.Lcn;


public class LcnMapper {



    public static LcnResponseDTO toDto(Lcn lcn) {
        if (lcn == null) {
            return null;
        }

        LcnResponseDTO dto = new LcnResponseDTO();
        dto.setId(lcn.getId());
        dto.setNumero(lcn.getNumero()!=null ? lcn.getNumero() : null);
        dto.setDateEmission(lcn.getDateEmission());
        dto.setBeneficiaire(lcn.getBeneficiaire());
        dto.setDateEcheance(lcn.getDateEcheance());
        dto.setMontant(lcn.getMontant());
        dto.setActive(lcn.isActive());

        // We only expose the bank's name, not the whole Banque object.
        if (lcn.getBanque() != null) {
            dto.setBanqueName(lcn.getBanque().getNom());
            dto.setBanqueId(lcn.getBanque().getId());
        }

        dto.setDateOperation(lcn.getDateOperation()!=null ? lcn.getDateOperation() : null);
        dto.setDateValeur(lcn.getDateValeur() !=null ? lcn.getDateValeur() : null);
        dto.setObservation("LCN N° " + lcn.getNumero());
        dto.setLibelle( "Bénéficiaire : " + lcn.getBeneficiaire());
        dto.setType(lcn.getType());
        dto.setEmetteur(lcn.getEmetteur() !=null ? lcn.getEmetteur() : null);
        return dto;
    }


    public static Lcn toEntity(LcnRequestDto dto, Banque banque) {

        Lcn lcn = new Lcn();
        lcn.setDateEmission(dto.getDateEmission());
        lcn.setDateEcheance(dto.getDateEcheance());
        lcn.setBeneficiaire(dto.getBeneficiaire());
        lcn.setMontant(dto.getMontant());
        lcn.setBanque(banque);
        lcn.setActive(true);
        lcn.setType(dto.getType());
        lcn.setEmetteur(dto.getEmetteur());

        return lcn;
    }

}
