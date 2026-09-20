package staport.finance.gBanque.operationErrone.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.operationErrone.validation.dto.OperationErroneTransactionRequestDto;
import staport.finance.gBanque.operationErrone.validation.dto.OperationErroneTransactionResponseDto;
import staport.finance.gBanque.operationErrone.validation.services.interfaces.IOperationErroneValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.OperationValideeDto;
import staport.finance.gBanque.validation.services.handlers.BatchValidationHandler;

/**
 * Gère la validation en lot des opérations erronées pour {@code BatchValidationService}.
 * <p>
 * Reprend à l'identique la logique du {@code case OPERATION_ERRONE} qu'il remplace dans
 * {@code BatchValidationService.processOne} (aucun changement de comportement) : Opération erronée
 * est toujours un décaissement (sens fixe), un seul appel de service, aucun branchement sur
 * {@code sens} ici — même patron que {@code RestitutionBatchValidationHandler}.
 */
@Component
public class OperationErroneBatchValidationHandler implements BatchValidationHandler {

    private final IOperationErroneValidationService operationErroneValidationService;

    public OperationErroneBatchValidationHandler(IOperationErroneValidationService operationErroneValidationService) {
        this.operationErroneValidationService = operationErroneValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.OPERATION_ERRONE;
    }

    @Override
    public OperationValideeDto processOne(Long id, BatchValidationRequestDto requestDto) {
        OperationErroneTransactionRequestDto dto = new OperationErroneTransactionRequestDto(
                id, requestDto.tauxTva(), requestDto.montantTva(), null,
                requestDto.dateValeur(), requestDto.dateOperation(),
                requestDto.observation(), requestDto.libelle(), requestDto.rapprochementId());
        OperationErroneTransactionResponseDto r = operationErroneValidationService.processOperationErroneTransaction(dto);
        return new OperationValideeDto(id, r.id(), r.libelle(), r.debit(), r.credit());
    }
}
