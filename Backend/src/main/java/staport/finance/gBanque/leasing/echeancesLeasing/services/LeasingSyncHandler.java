package staport.finance.gBanque.leasing.echeancesLeasing.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.leasing.echeancesLeasing.services.interfaces.ILeasingValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionSyncHandler;

import java.time.LocalDate;

/**
 * Gère la synchronisation d'une échéance de leasing depuis sa transaction pour
 * {@code TransactionServiceImpl} — reprend à l'identique l'ancien appel direct à
 * {@code leasingValidationService.syncEcheanceFromTransaction}.
 */
@Component
public class LeasingSyncHandler implements TransactionSyncHandler {

    private final ILeasingValidationService leasingValidationService;

    public LeasingSyncHandler(ILeasingValidationService leasingValidationService) {
        this.leasingValidationService = leasingValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.LEASING;
    }

    @Override
    public void sync(Long referentId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation) {
        leasingValidationService.syncEcheanceFromTransaction(referentId, dateValeur, dateOperation, libelle, observation);
    }
}
