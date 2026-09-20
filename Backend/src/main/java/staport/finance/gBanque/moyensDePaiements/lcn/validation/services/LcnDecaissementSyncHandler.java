package staport.finance.gBanque.moyensDePaiements.lcn.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.lcn.validation.services.interfaces.ILcnValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionSyncHandler;

import java.time.LocalDate;

/**
 * Gère la synchronisation des LCN de décaissement depuis leur transaction pour
 * {@code TransactionServiceImpl} — voir {@code LcnEncaissementSyncHandler}/
 * {@code LcnEscompteSyncHandler} pour le cas particulier LCN (sens porté par
 * {@code TransactionSource}, un seul {@code ILcnValidationService.syncLcnFromTransaction}
 * indifférent à la nature, d'où 3 handlers délégant au même appel).
 */
@Component
public class LcnDecaissementSyncHandler implements TransactionSyncHandler {

    private final ILcnValidationService lcnValidationService;

    public LcnDecaissementSyncHandler(ILcnValidationService lcnValidationService) {
        this.lcnValidationService = lcnValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.LCN_DECAISSEMENT;
    }

    @Override
    public void sync(Long referentId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation) {
        lcnValidationService.syncLcnFromTransaction(referentId, dateValeur, dateOperation, libelle, observation);
    }
}
