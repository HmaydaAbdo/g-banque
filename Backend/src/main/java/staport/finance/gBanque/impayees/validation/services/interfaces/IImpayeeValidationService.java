package staport.finance.gBanque.impayees.validation.services.interfaces;

import staport.finance.gBanque.impayees.validation.dto.ImpayeeTransactionRequestDto;
import staport.finance.gBanque.impayees.validation.dto.ImpayeeTransactionResponseDto;

import java.time.LocalDate;

/**
 * Contrat de validation (rapprochement) du module Impayées (architecture.md, Règle 1).
 */
public interface IImpayeeValidationService {

    ImpayeeTransactionResponseDto processImpayeeTransaction(ImpayeeTransactionRequestDto requestDto);

    void syncImpayeeFromTransaction(Long impayeeId, LocalDate dateValeur, LocalDate dateOperation);
}
