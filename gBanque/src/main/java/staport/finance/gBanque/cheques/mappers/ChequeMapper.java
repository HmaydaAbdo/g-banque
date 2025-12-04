package staport.finance.gBanque.cheques.mappers;

import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.cheques.dtos.ChequeRequestDto;
import staport.finance.gBanque.cheques.entities.Cheque;
import staport.finance.gBanque.cheques.dtos.ChequeResponseDto;
import staport.finance.gBanque.virements.enums.Type;

public class ChequeMapper {


    public static ChequeResponseDto toDto(Cheque cheque) {
        if (cheque == null) {
            return null;
        }

        ChequeResponseDto dto = new ChequeResponseDto();
        dto.setId(cheque.getId());
        dto.setNumero(cheque.getNumero() != null ? cheque.getNumero() : null);
        dto.setDateEmission(cheque.getDateEmission());
        dto.setDateEcheance(cheque.getDateEcheance() != null ? cheque.getDateEcheance() : null);
        dto.setBeneficiaire(cheque.getBeneficiaire());
        dto.setMontant(cheque.getMontant());
        dto.setActive(cheque.isActive());
        dto.setType(cheque.getType());
        dto.setEmetteur(cheque.getEmetteur() != null ? cheque.getEmetteur() : null);
        // We only need the bank's name for the DTO, not the entire Banque object,
        // which avoids exposing unnecessary data and improves performance.
        if (cheque.getBanque() != null) {
            dto.setNomBanque(cheque.getBanque().getNom());
            dto.setBanqueId(cheque.getBanque().getId());
        }

        dto.setDateOperation(cheque.getDateOperation() != null ? cheque.getDateOperation() : null);
        dto.setDateValeur(cheque.getDateValeur() != null ? cheque.getDateValeur() : null);
        dto.setObservation( "CHQ N°" +cheque.getNumero());
        dto.setLibelle( cheque.getType()== Type.DECAISSEMENT ? cheque.getBeneficiaire() : cheque.getEmetteur());
        return dto;
    }

    public static Cheque toEntity(ChequeRequestDto dto, Banque banque) {

        Cheque cheque = new Cheque();

        cheque.setNumero(dto.getNumero());
        cheque.setDateEmission(dto.getDateEmission());

        if(dto.getDateEcheance() != null){
            cheque.setDateEcheance(dto.getDateEcheance());
        }

        cheque.setBeneficiaire(dto.getBeneficiaire());
        cheque.setMontant(dto.getMontant());
        cheque.setBanque(banque);
        cheque.setActive(true);
        cheque.setType(dto.getType());
        cheque.setEmetteur(dto.getEmetteur());

        return cheque;

    }
}