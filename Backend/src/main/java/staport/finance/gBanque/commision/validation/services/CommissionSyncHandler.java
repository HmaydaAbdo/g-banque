package staport.finance.gBanque.commision.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.commision.validation.services.interfaces.ICommissionValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionSyncHandler;

import java.time.LocalDate;

/**
 * Gère la synchronisation d'une commission depuis sa transaction pour
 * {@code TransactionServiceImpl} — reprend à l'identique l'ancien appel direct à
 * {@code commissionValidationService.syncCommissionFromTransaction}.
 */
@Component
public class CommissionSyncHandler implements TransactionSyncHandler {

    private final ICommissionValidationService commissionValidationService;

    public CommissionSyncHandler(ICommissionValidationService commissionValidationService) {
        this.commissionValidationService = commissionValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.COMMISSION;
    }

    @Override
    public void sync(Long referentId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation) {
        commissionValidationService.syncCommissionFromTransaction(referentId, dateValeur, dateOperation, libelle, observation);
    }
}
