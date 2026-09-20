package staport.finance.gBanque.Agios.service;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Agios.service.interfaces.IAgiosValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionReversalHandler;

/**
 * Gère l'annulation d'un agios pour {@code ReverseTransactionService} — reprend à l'identique la
 * logique de l'ancien {@code case AGIOS}, qui déléguait déjà à
 * {@link IAgiosValidationService#resetAgios(Long)} plutôt que de manipuler directement le
 * repository (seule source à suivre déjà ce patron avant cette migration).
 */
@Component
public class AgiosReversalHandler implements TransactionReversalHandler {

    private final IAgiosValidationService agiosValidationService;

    public AgiosReversalHandler(IAgiosValidationService agiosValidationService) {
        this.agiosValidationService = agiosValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.AGIOS;
    }

    @Override
    public void reset(Long referentId) {
        agiosValidationService.resetAgios(referentId);
    }
}
