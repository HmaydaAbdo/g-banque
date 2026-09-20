package staport.finance.gBanque.impayees.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.impayees.validation.services.interfaces.IImpayeeValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionSyncHandler;

import java.time.LocalDate;

/**
 * Gère la synchronisation d'une impayée depuis sa transaction pour
 * {@code TransactionServiceImpl} — reprend à l'identique l'ancien appel direct à
 * {@code impayeeValidationService.syncImpayeeFromTransaction}.
 */
@Component
public class ImpayeeSyncHandler implements TransactionSyncHandler {

    private final IImpayeeValidationService impayeeValidationService;

    public ImpayeeSyncHandler(IImpayeeValidationService impayeeValidationService) {
        this.impayeeValidationService = impayeeValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.IMPAYEE;
    }

    @Override
    public void sync(Long referentId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation) {
        impayeeValidationService.syncImpayeeFromTransaction(referentId, dateValeur, dateOperation, libelle, observation);
    }
}
