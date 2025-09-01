package staport.finance.gBanque.cheques.mappers;

import staport.finance.gBanque.cheques.entities.Cheque;
import staport.finance.gBanque.cheques.dtos.ChequeResponseDto;

public class ChequeMapper {

    /**
     * Converts a Cheque entity to a ChequeResponseDto.
     * This method is crucial for separating the domain model from the API's data contract.
     *
     * @param cheque The Cheque entity to convert.
     * @return The populated ChequeResponseDto.
     */
    public static ChequeResponseDto toDto(Cheque cheque) {
        if (cheque == null) {
            return null;
        }

        ChequeResponseDto dto = new ChequeResponseDto();
        dto.setNumero(cheque.getNumero());
        dto.setDateEmission(cheque.getDateEmission());
        dto.setDateEcheance(cheque.getDateEcheance());
        dto.setBeneficiaire(cheque.getBeneficiaire());
        dto.setMontant(cheque.getMontant());
        dto.setActive(cheque.isActive());

        // We only need the bank's name for the DTO, not the entire Banque object,
        // which avoids exposing unnecessary data and improves performance.
        if (cheque.getBanque() != null) {
            dto.setNomBanque(cheque.getBanque().getNom());
        }

        return dto;
    }
}