package staport.finance.gBanque.droitConstate.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.droitConstate.repositories.DroitConstateRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionReversalHandler;

/**
 * Gère l'annulation d'un droit constaté pour {@code ReverseTransactionService} — reprend à
 * l'identique la logique de l'ancien {@code case DROIT_CONSTATE} (méthode
 * {@code resetDroitConstate}) qu'il remplace.
 */
@Component
public class DroitConstateReversalHandler implements TransactionReversalHandler {

    private final DroitConstateRepository droitConstateRepository;

    public DroitConstateReversalHandler(DroitConstateRepository droitConstateRepository) {
        this.droitConstateRepository = droitConstateRepository;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.DROIT_CONSTATE;
    }

    @Override
    public void reset(Long referentId) {
        DroitConstate droitConstate = droitConstateRepository.findById(referentId)
                .orElseThrow(() -> new ResourceNotFoundException("Droit constaté introuvable: " + referentId));
        droitConstate.setStatus(Status.PREVU);
        droitConstate.setDateOperation(null);
        droitConstate.setDateValeur(null);
        droitConstateRepository.save(droitConstate);
    }
}
