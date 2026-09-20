package staport.finance.gBanque.moyensDePaiements.cheques.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.moyensDePaiements.cheques.repositories.ChequeRepository;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionReversalHandler;

/**
 * Gère l'annulation d'un chèque pour {@code ReverseTransactionService} — reprend à l'identique la
 * logique de l'ancien {@code case CHEQUE} (méthode {@code resetCheque}) qu'il remplace.
 */
@Component
public class ChequeReversalHandler implements TransactionReversalHandler {

    private final ChequeRepository chequeRepository;

    public ChequeReversalHandler(ChequeRepository chequeRepository) {
        this.chequeRepository = chequeRepository;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.CHEQUE;
    }

    @Override
    public void reset(Long referentId) {
        Cheque cheque = chequeRepository.findById(referentId)
                .orElseThrow(() -> new ResourceNotFoundException("Cheque introuvable: " + referentId));
        cheque.setStatut(StatutMoyenPaiement.ENCOURS);
        cheque.setDateOperation(null);
        cheque.setDateValeur(null);
        chequeRepository.save(cheque);
    }
}
