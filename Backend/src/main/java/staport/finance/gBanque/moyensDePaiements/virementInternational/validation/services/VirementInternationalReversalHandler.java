package staport.finance.gBanque.moyensDePaiements.virementInternational.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.virementInternational.entities.VirementInternational;
import staport.finance.gBanque.moyensDePaiements.virementInternational.repositories.VirementInternationalRepository;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionReversalHandler;

/**
 * Gère l'annulation d'un virement international pour {@code ReverseTransactionService} — reprend
 * à l'identique la logique de l'ancien {@code case VIREMENT_INTERNATIONAL} (méthode
 * {@code resetVirementInternational}) qu'il remplace.
 */
@Component
public class VirementInternationalReversalHandler implements TransactionReversalHandler {

    private final VirementInternationalRepository virementInternationalRepository;

    public VirementInternationalReversalHandler(VirementInternationalRepository virementInternationalRepository) {
        this.virementInternationalRepository = virementInternationalRepository;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.VIREMENT_INTERNATIONAL;
    }

    @Override
    public void reset(Long referentId) {
        VirementInternational virementInternational = virementInternationalRepository.findById(referentId)
                .orElseThrow(() -> new ResourceNotFoundException("Virement international introuvable: " + referentId));
        virementInternational.setStatut(StatutMoyenPaiement.ENCOURS);
        virementInternational.setDateOperation(null);
        virementInternational.setDateValeur(null);
        virementInternationalRepository.save(virementInternational);
    }
}
