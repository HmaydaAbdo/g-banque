package staport.finance.gBanque.moyensDePaiements.lcn.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.lcn.repositories.LcnRepository;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionReversalHandler;

/**
 * Gère l'annulation des LCN d'encaissement pour {@code ReverseTransactionService} — voir
 * {@link LcnResetSupport} pour la logique de reset, partagée par les 3 natures de LCN.
 */
@Component
public class LcnEncaissementReversalHandler implements TransactionReversalHandler {

    private final LcnRepository lcnRepository;

    public LcnEncaissementReversalHandler(LcnRepository lcnRepository) {
        this.lcnRepository = lcnRepository;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.LCN_ENCAISSEMENT;
    }

    @Override
    public void reset(Long referentId) {
        LcnResetSupport.reset(lcnRepository, referentId);
    }
}
