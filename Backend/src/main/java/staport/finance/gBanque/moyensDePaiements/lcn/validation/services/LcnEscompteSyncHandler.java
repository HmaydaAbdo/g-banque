package staport.finance.gBanque.moyensDePaiements.lcn.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.lcn.validation.services.interfaces.ILcnValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionSyncHandler;

import java.time.LocalDate;

/**
 * Gère la synchronisation des LCN d'escompte depuis leur transaction pour
 * {@code TransactionServiceImpl} — voir {@link LcnDecaissementSyncHandler} pour le cas particulier
 * LCN. Contrairement à la validation en lot (voir {@code LcnEscompteBatchValidationHandler}), la
 * synchronisation gérait déjà cette nature dans l'ancien if-chain — aucun changement de
 * comportement ici, seulement de mécanisme.
 */
@Component
public class LcnEscompteSyncHandler implements TransactionSyncHandler {

    private final ILcnValidationService lcnValidationService;

    public LcnEscompteSyncHandler(ILcnValidationService lcnValidationService) {
        this.lcnValidationService = lcnValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.LCN_ESCOMPTE;
    }

    @Override
    public void sync(Long referentId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation) {
        lcnValidationService.syncLcnFromTransaction(referentId, dateValeur, dateOperation, libelle, observation);
    }
}
