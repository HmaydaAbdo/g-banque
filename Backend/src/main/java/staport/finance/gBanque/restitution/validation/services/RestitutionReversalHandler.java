package staport.finance.gBanque.restitution.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.restitution.entities.Restitution;
import staport.finance.gBanque.restitution.repository.RestitutionRepository;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionReversalHandler;

/**
 * Gère l'annulation d'une restitution pour {@code ReverseTransactionService} — reprend à
 * l'identique la logique de l'ancien {@code case RESTITUTION} (méthode {@code resetRestitution})
 * qu'il remplace.
 */
@Component
public class RestitutionReversalHandler implements TransactionReversalHandler {

    private final RestitutionRepository restitutionRepository;

    public RestitutionReversalHandler(RestitutionRepository restitutionRepository) {
        this.restitutionRepository = restitutionRepository;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.RESTITUTION;
    }

    @Override
    public void reset(Long referentId) {
        Restitution restitution = restitutionRepository.findById(referentId)
                .orElseThrow(() -> new ResourceNotFoundException("Restitution introuvable: " + referentId));
        restitution.setStatus(Status.PREVU);
        restitution.setDateOperation(null);
        restitution.setDateValeur(null);
        restitutionRepository.save(restitution);
    }
}
