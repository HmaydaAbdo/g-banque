package staport.finance.gBanque.prelevements.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prelevements.entitie.Prelevement;
import staport.finance.gBanque.prelevements.repository.PrelevementRepository;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionReversalHandler;

/**
 * Gère l'annulation d'un prélèvement pour {@code ReverseTransactionService} — reprend à
 * l'identique la logique de l'ancien {@code case PRELEVEMENT} (méthode {@code resetPrelevement})
 * qu'il remplace.
 */
@Component
public class PrelevementReversalHandler implements TransactionReversalHandler {

    private final PrelevementRepository prelevementRepository;

    public PrelevementReversalHandler(PrelevementRepository prelevementRepository) {
        this.prelevementRepository = prelevementRepository;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.PRELEVEMENT;
    }

    @Override
    public void reset(Long referentId) {
        Prelevement prelevement = prelevementRepository.findById(referentId)
                .orElseThrow(() -> new ResourceNotFoundException("Prélèvement introuvable: " + referentId));
        prelevement.setStatus(Status.PREVU);
        prelevement.setDateOperation(null);
        prelevement.setDateValeur(null);
        prelevementRepository.save(prelevement);
    }
}
