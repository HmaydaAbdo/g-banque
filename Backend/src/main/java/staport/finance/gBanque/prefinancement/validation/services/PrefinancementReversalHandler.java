package staport.finance.gBanque.prefinancement.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.repository.PrefinancementRepository;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionReversalHandler;

/**
 * Gère l'annulation d'un préfinancement pour {@code ReverseTransactionService} — reprend à
 * l'identique la logique de l'ancien {@code case PREFINANCEMENT} (méthode
 * {@code resetPrefinancement}) qu'il remplace.
 */
@Component
public class PrefinancementReversalHandler implements TransactionReversalHandler {

    private final PrefinancementRepository prefinancementRepository;

    public PrefinancementReversalHandler(PrefinancementRepository prefinancementRepository) {
        this.prefinancementRepository = prefinancementRepository;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.PREFINANCEMENT;
    }

    @Override
    public void reset(Long referentId) {
        Prefinancement prefinancement = prefinancementRepository.findById(referentId)
                .orElseThrow(() -> new ResourceNotFoundException("Préfinancement introuvable: " + referentId));
        prefinancement.setStatus(Status.PREVU);
        prefinancement.setDateOperation(null);
        prefinancement.setDateValeur(null);
        prefinancementRepository.save(prefinancement);
    }
}
