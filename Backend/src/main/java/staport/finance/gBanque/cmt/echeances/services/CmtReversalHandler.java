package staport.finance.gBanque.cmt.echeances.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;
import staport.finance.gBanque.cmt.echeances.repositories.EcheanceCMTRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionReversalHandler;

/**
 * Gère l'annulation d'une échéance CMT pour {@code ReverseTransactionService} — reprend à
 * l'identique la logique de l'ancien {@code case CMT} (méthode {@code resetCMT}) qu'il remplace.
 */
@Component
public class CmtReversalHandler implements TransactionReversalHandler {

    private final EcheanceCMTRepository echeanceCMTRepository;

    public CmtReversalHandler(EcheanceCMTRepository echeanceCMTRepository) {
        this.echeanceCMTRepository = echeanceCMTRepository;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.CMT;
    }

    @Override
    public void reset(Long referentId) {
        EcheanceCMT echeance = echeanceCMTRepository.findById(referentId)
                .orElseThrow(() -> new ResourceNotFoundException("Echeance CMT introuvable: " + referentId));
        echeance.setStatutPaiement(StatutEcheanceCMT.PREVU);
        echeance.setDateValeur(null);
        echeance.setDateOperation(null);
        echeanceCMTRepository.save(echeance);
    }
}
