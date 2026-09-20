package staport.finance.gBanque.operationErrone.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.operationErrone.entities.OperationErrone;
import staport.finance.gBanque.operationErrone.repositories.OperationErroneRepository;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionReversalHandler;

/**
 * Gère l'annulation d'une opération erronée pour {@code ReverseTransactionService} — reprend à
 * l'identique la logique de l'ancien {@code case OPERATION_ERRONE} (méthode
 * {@code resetOperationErrone}) qu'il remplace.
 */
@Component
public class OperationErroneReversalHandler implements TransactionReversalHandler {

    private final OperationErroneRepository operationErroneRepository;

    public OperationErroneReversalHandler(OperationErroneRepository operationErroneRepository) {
        this.operationErroneRepository = operationErroneRepository;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.OPERATION_ERRONE;
    }

    @Override
    public void reset(Long referentId) {
        OperationErrone operationErrone = operationErroneRepository.findById(referentId)
                .orElseThrow(() -> new ResourceNotFoundException("Operation erroné introuvable: " + referentId));
        operationErrone.setStatus(Status.PREVU);
        operationErrone.setDateOperation(null);
        operationErrone.setDateValeur(null);
        operationErroneRepository.save(operationErrone);
    }
}
