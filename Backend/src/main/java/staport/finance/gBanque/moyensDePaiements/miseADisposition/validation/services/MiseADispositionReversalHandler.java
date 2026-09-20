package staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.repositories.MiseADispositionRepository;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionReversalHandler;

/**
 * Gère l'annulation d'une mise à disposition pour {@code ReverseTransactionService} — reprend à
 * l'identique la logique de l'ancien {@code case MiseADisposition} (méthode
 * {@code resetMiseADisposition}) qu'il remplace.
 */
@Component
public class MiseADispositionReversalHandler implements TransactionReversalHandler {

    private final MiseADispositionRepository miseADispositionRepository;

    public MiseADispositionReversalHandler(MiseADispositionRepository miseADispositionRepository) {
        this.miseADispositionRepository = miseADispositionRepository;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.MiseADisposition;
    }

    @Override
    public void reset(Long referentId) {
        MiseADisposition miseADispo = miseADispositionRepository.findById(referentId)
                .orElseThrow(() -> new ResourceNotFoundException("Mise à disposition introuvable: " + referentId));
        miseADispo.setStatut(StatutMoyenPaiement.ENCOURS);
        miseADispo.setDateOperation(null);
        miseADispo.setDateValeur(null);
        miseADispositionRepository.save(miseADispo);
    }
}
