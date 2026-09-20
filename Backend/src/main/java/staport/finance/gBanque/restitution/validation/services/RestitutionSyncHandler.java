package staport.finance.gBanque.restitution.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.restitution.validation.services.interfaces.IRestitutionValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionSyncHandler;

import java.time.LocalDate;

/**
 * Gère la synchronisation d'une restitution depuis sa transaction pour
 * {@code TransactionServiceImpl} — reprend à l'identique l'ancien appel direct à
 * {@code restitutionValidationService.syncRestitutionFromTransaction}.
 */
@Component
public class RestitutionSyncHandler implements TransactionSyncHandler {

    private final IRestitutionValidationService restitutionValidationService;

    public RestitutionSyncHandler(IRestitutionValidationService restitutionValidationService) {
        this.restitutionValidationService = restitutionValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.RESTITUTION;
    }

    @Override
    public void sync(Long referentId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation) {
        restitutionValidationService.syncRestitutionFromTransaction(referentId, dateValeur, dateOperation, libelle, observation);
    }
}
