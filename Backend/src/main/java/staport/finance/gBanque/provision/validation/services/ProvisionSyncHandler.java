package staport.finance.gBanque.provision.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.provision.validation.services.interfaces.IProvisionValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionSyncHandler;

import java.time.LocalDate;

/**
 * Gère la synchronisation d'une provision depuis sa transaction pour
 * {@code TransactionServiceImpl} — reprend à l'identique l'ancien appel direct à
 * {@code provisionValidationService.syncProvisionFromTransaction}.
 */
@Component
public class ProvisionSyncHandler implements TransactionSyncHandler {

    private final IProvisionValidationService provisionValidationService;

    public ProvisionSyncHandler(IProvisionValidationService provisionValidationService) {
        this.provisionValidationService = provisionValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.PROVISION;
    }

    @Override
    public void sync(Long referentId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation) {
        provisionValidationService.syncProvisionFromTransaction(referentId, dateValeur, dateOperation, libelle, observation);
    }
}
