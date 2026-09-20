package staport.finance.gBanque.droitConstate.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.droitConstate.validation.services.interfaces.IDroitConstateValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionSyncHandler;

import java.time.LocalDate;

/**
 * Gère la synchronisation d'un droit constaté depuis sa transaction pour
 * {@code TransactionServiceImpl} — reprend à l'identique l'ancien appel direct à
 * {@code droitConstateValidationService.syncDroitConstateFromTransaction}.
 */
@Component
public class DroitConstateSyncHandler implements TransactionSyncHandler {

    private final IDroitConstateValidationService droitConstateValidationService;

    public DroitConstateSyncHandler(IDroitConstateValidationService droitConstateValidationService) {
        this.droitConstateValidationService = droitConstateValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.DROIT_CONSTATE;
    }

    @Override
    public void sync(Long referentId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation) {
        droitConstateValidationService.syncDroitConstateFromTransaction(referentId, dateValeur, dateOperation, libelle, observation);
    }
}
