package staport.finance.gBanque.moyensDePaiements.lcn.validation.services;

import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.lcn.repositories.LcnRepository;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

/**
 * Logique de reset partagée par les 3 {@link staport.finance.gBanque.validation.services.handlers.TransactionReversalHandler}
 * LCN ({@code LcnDecaissementReversalHandler}, {@code LcnEncaissementReversalHandler},
 * {@code LcnEscompteReversalHandler}) — un LCN se réinitialise de la même façon quelle que soit sa
 * nature, seule la clé de registre ({@code TransactionSource}) diffère entre les 3 handlers (même
 * raison que les 3 {@code LcnXxxBatchValidationHandler} côté validation).
 */
final class LcnResetSupport {

    private LcnResetSupport() {
    }

    static void reset(LcnRepository lcnRepository, Long referentId) {
        Lcn lcn = lcnRepository.findById(referentId)
                .orElseThrow(() -> new ResourceNotFoundException("LCN introuvable: " + referentId));
        lcn.setStatut(StatutMoyenPaiement.ENCOURS);
        lcn.setDateOperation(null);
        lcn.setDateValeur(null);
        lcnRepository.save(lcn);
    }
}
