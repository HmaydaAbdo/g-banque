package staport.finance.gBanque.factoring.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.factoring.validation.services.interfaces.IFactoringValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionSyncHandler;

import java.time.LocalDate;

/**
 * Gère la synchronisation d'un factoring depuis sa transaction pour
 * {@code TransactionServiceImpl} — reprend à l'identique l'ancien appel direct à
 * {@code factoringValidationService.syncFactoringFromTransaction}.
 */
@Component
public class FactoringSyncHandler implements TransactionSyncHandler {

    private final IFactoringValidationService factoringValidationService;

    public FactoringSyncHandler(IFactoringValidationService factoringValidationService) {
        this.factoringValidationService = factoringValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.FACTORING;
    }

    @Override
    public void sync(Long referentId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation) {
        factoringValidationService.syncFactoringFromTransaction(referentId, dateValeur, dateOperation, libelle, observation);
    }
}
