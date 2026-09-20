package staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.services.interfaces.IMiseADispositionValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionSyncHandler;

import java.time.LocalDate;

/**
 * Gère la synchronisation d'une mise à disposition depuis sa transaction pour
 * {@code TransactionServiceImpl} — reprend à l'identique l'ancien appel direct à
 * {@code miseADispositionValidationService.syncMiseADispositionFromTransaction}.
 */
@Component
public class MiseADispositionSyncHandler implements TransactionSyncHandler {

    private final IMiseADispositionValidationService miseADispositionValidationService;

    public MiseADispositionSyncHandler(IMiseADispositionValidationService miseADispositionValidationService) {
        this.miseADispositionValidationService = miseADispositionValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.MiseADisposition;
    }

    @Override
    public void sync(Long referentId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation) {
        miseADispositionValidationService.syncMiseADispositionFromTransaction(referentId, dateValeur, dateOperation, libelle, observation);
    }
}
