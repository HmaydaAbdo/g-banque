package staport.finance.gBanque.impayees.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.impayees.entities.Impayee;
import staport.finance.gBanque.impayees.repository.ImpayeeRepository;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionReversalHandler;

/**
 * Gère l'annulation d'une impayée pour {@code ReverseTransactionService} — reprend à l'identique
 * la logique de l'ancien {@code case IMPAYEE} (méthode {@code resetImpayee}) qu'il remplace.
 */
@Component
public class ImpayeeReversalHandler implements TransactionReversalHandler {

    private final ImpayeeRepository impayeeRepository;

    public ImpayeeReversalHandler(ImpayeeRepository impayeeRepository) {
        this.impayeeRepository = impayeeRepository;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.IMPAYEE;
    }

    @Override
    public void reset(Long referentId) {
        Impayee impayee = impayeeRepository.findById(referentId)
                .orElseThrow(() -> new ResourceNotFoundException("Impayée introuvable: " + referentId));
        impayee.setStatus(Status.PREVU);
        impayee.setDateOperation(null);
        impayee.setDateValeur(null);
        impayeeRepository.save(impayee);
    }
}
