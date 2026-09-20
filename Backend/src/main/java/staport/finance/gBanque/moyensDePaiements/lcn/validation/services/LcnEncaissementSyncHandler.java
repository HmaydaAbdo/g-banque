package staport.finance.gBanque.moyensDePaiements.lcn.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.lcn.validation.services.interfaces.ILcnValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionSyncHandler;

import java.time.LocalDate;

/**
 * Gère la synchronisation des LCN d'encaissement depuis leur transaction pour
 * {@code TransactionServiceImpl} — voir {@link LcnDecaissementSyncHandler} pour le cas particulier
 * LCN.
 */
@Component
public class LcnEncaissementSyncHandler implements TransactionSyncHandler {

    private final ILcnValidationService lcnValidationService;

    public LcnEncaissementSyncHandler(ILcnValidationService lcnValidationService) {
        this.lcnValidationService = lcnValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.LCN_ENCAISSEMENT;
    }

    @Override
    public void sync(Long referentId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation) {
        lcnValidationService.syncLcnFromTransaction(referentId, dateValeur, dateOperation, libelle, observation);
    }
}
