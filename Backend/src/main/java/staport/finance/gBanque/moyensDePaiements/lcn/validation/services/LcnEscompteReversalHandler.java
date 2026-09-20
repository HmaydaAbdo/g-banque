package staport.finance.gBanque.moyensDePaiements.lcn.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.lcn.repositories.LcnRepository;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionReversalHandler;

/**
 * Gère l'annulation des LCN d'escompte pour {@code ReverseTransactionService} — voir
 * {@link LcnResetSupport} pour la logique de reset, partagée par les 3 natures de LCN.
 * <p>
 * Contrairement à la validation en lot (où {@code LCN_ESCOMPTE} n'a de handler que depuis le
 * 19/09/2026, voir {@code LcnEscompteBatchValidationHandler}), l'annulation gérait déjà cette
 * nature dans l'ancien switch de {@code ReverseTransactionService} — aucun changement de
 * comportement ici, seulement un changement de mécanisme.
 */
@Component
public class LcnEscompteReversalHandler implements TransactionReversalHandler {

    private final LcnRepository lcnRepository;

    public LcnEscompteReversalHandler(LcnRepository lcnRepository) {
        this.lcnRepository = lcnRepository;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.LCN_ESCOMPTE;
    }

    @Override
    public void reset(Long referentId) {
        LcnResetSupport.reset(lcnRepository, referentId);
    }
}
