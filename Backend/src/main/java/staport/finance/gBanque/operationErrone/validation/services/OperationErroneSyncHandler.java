package staport.finance.gBanque.operationErrone.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.operationErrone.validation.services.interfaces.IOperationErroneValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionSyncHandler;

import java.time.LocalDate;

/**
 * Gère la synchronisation d'une opération erronée depuis sa transaction pour
 * {@code TransactionServiceImpl} — reprend à l'identique l'ancien appel direct à
 * {@code operationErroneValidationService.syncOperationErroneFromTransaction}.
 */
@Component
public class OperationErroneSyncHandler implements TransactionSyncHandler {

    private final IOperationErroneValidationService operationErroneValidationService;

    public OperationErroneSyncHandler(IOperationErroneValidationService operationErroneValidationService) {
        this.operationErroneValidationService = operationErroneValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.OPERATION_ERRONE;
    }

    @Override
    public void sync(Long referentId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation) {
        operationErroneValidationService.syncOperationErroneFromTransaction(referentId, dateValeur, dateOperation, libelle, observation);
    }
}
