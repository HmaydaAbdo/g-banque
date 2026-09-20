package staport.finance.gBanque.factoring.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.factoring.repository.FactoringRepository;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionReversalHandler;

/**
 * Gère l'annulation d'un factoring pour {@code ReverseTransactionService} — reprend à l'identique
 * la logique de l'ancien {@code case FACTORING} (méthode {@code resetFactoring}) qu'il remplace.
 */
@Component
public class FactoringReversalHandler implements TransactionReversalHandler {

    private final FactoringRepository factoringRepository;

    public FactoringReversalHandler(FactoringRepository factoringRepository) {
        this.factoringRepository = factoringRepository;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.FACTORING;
    }

    @Override
    public void reset(Long referentId) {
        Factoring factoring = factoringRepository.findById(referentId)
                .orElseThrow(() -> new ResourceNotFoundException("Factoring introuvable: " + referentId));
        factoring.setStatus(Status.PREVU);
        factoring.setDateOperation(null);
        factoring.setDateValeur(null);
        factoringRepository.save(factoring);
    }
}
