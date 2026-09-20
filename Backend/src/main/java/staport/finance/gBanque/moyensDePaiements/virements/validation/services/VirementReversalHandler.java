package staport.finance.gBanque.moyensDePaiements.virements.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.moyensDePaiements.virements.repositories.VirementRepository;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionReversalHandler;

/**
 * Gère l'annulation d'un virement pour {@code ReverseTransactionService} — reprend à l'identique
 * la logique de l'ancien {@code case Virement} (méthode {@code resetVirement}) qu'il remplace.
 */
@Component
public class VirementReversalHandler implements TransactionReversalHandler {

    private final VirementRepository virementRepository;

    public VirementReversalHandler(VirementRepository virementRepository) {
        this.virementRepository = virementRepository;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.Virement;
    }

    @Override
    public void reset(Long referentId) {
        Virement virement = virementRepository.findById(referentId)
                .orElseThrow(() -> new ResourceNotFoundException("Virement introuvable: " + referentId));
        virement.setStatut(StatutMoyenPaiement.ENCOURS);
        virement.setDateOperation(null);
        virement.setDateValeur(null);
        virementRepository.save(virement);
    }
}
