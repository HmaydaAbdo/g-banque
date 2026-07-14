package staport.finance.gBanque.operationErrone.validation.services.interfaces;

import staport.finance.gBanque.operationErrone.validation.dto.OperationErroneTransactionRequestDto;
import staport.finance.gBanque.operationErrone.validation.dto.OperationErroneTransactionResponseDto;

import java.time.LocalDate;

/**
 * Contrat de validation (rapprochement) du module Opérations erronées (architecture.md, Règle 1).
 */
public interface IOperationErroneValidationService {

    OperationErroneTransactionResponseDto processOperationErroneTransaction(OperationErroneTransactionRequestDto requestDto);

    void syncOperationErroneFromTransaction(Long operationErroneId, LocalDate dateValeur, LocalDate dateOperation);
}
