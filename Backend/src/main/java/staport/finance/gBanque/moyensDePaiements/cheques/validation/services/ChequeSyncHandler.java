package staport.finance.gBanque.moyensDePaiements.cheques.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.cheques.validation.services.interfaces.IChequeValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionSyncHandler;

import java.time.LocalDate;

/**
 * Gère la synchronisation d'un chèque depuis sa transaction pour {@code TransactionServiceImpl} —
 * reprend à l'identique l'ancien appel direct à
 * {@code chequeValidationService.syncChequeFromTransaction}.
 */
@Component
public class ChequeSyncHandler implements TransactionSyncHandler {

    private final IChequeValidationService chequeValidationService;

    public ChequeSyncHandler(IChequeValidationService chequeValidationService) {
        this.chequeValidationService = chequeValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.CHEQUE;
    }

    @Override
    public void sync(Long referentId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation) {
        chequeValidationService.syncChequeFromTransaction(referentId, dateValeur, dateOperation, libelle, observation);
    }
}
