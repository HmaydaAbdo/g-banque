package staport.finance.gBanque.prelevements.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.prelevements.validation.services.interfaces.IPrelevementValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionSyncHandler;

import java.time.LocalDate;

/**
 * Gère la synchronisation d'un prélèvement depuis sa transaction pour
 * {@code TransactionServiceImpl} — reprend à l'identique l'ancien appel direct à
 * {@code prelevementValidationService.syncPrelevementFromTransaction}.
 */
@Component
public class PrelevementSyncHandler implements TransactionSyncHandler {

    private final IPrelevementValidationService prelevementValidationService;

    public PrelevementSyncHandler(IPrelevementValidationService prelevementValidationService) {
        this.prelevementValidationService = prelevementValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.PRELEVEMENT;
    }

    @Override
    public void sync(Long referentId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation) {
        prelevementValidationService.syncPrelevementFromTransaction(referentId, dateValeur, dateOperation, libelle, observation);
    }
}
