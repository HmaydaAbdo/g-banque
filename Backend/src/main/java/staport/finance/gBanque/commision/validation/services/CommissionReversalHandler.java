package staport.finance.gBanque.commision.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.commision.repository.CommisionRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionReversalHandler;

/**
 * Gère l'annulation d'une commission pour {@code ReverseTransactionService} — reprend à
 * l'identique la logique de l'ancien {@code case COMMISSION} (méthode {@code resetCommission})
 * qu'il remplace.
 */
@Component
public class CommissionReversalHandler implements TransactionReversalHandler {

    private final CommisionRepository commisionRepository;

    public CommissionReversalHandler(CommisionRepository commisionRepository) {
        this.commisionRepository = commisionRepository;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.COMMISSION;
    }

    @Override
    public void reset(Long referentId) {
        Commission commission = commisionRepository.findById(referentId)
                .orElseThrow(() -> new ResourceNotFoundException("Commission introuvable: " + referentId));
        commission.setStatus(Status.PREVU);
        commission.setDateOperation(null);
        commission.setDateValeur(null);
        commisionRepository.save(commission);
    }
}
