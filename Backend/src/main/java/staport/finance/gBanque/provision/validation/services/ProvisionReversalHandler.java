package staport.finance.gBanque.provision.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.provision.entities.Provision;
import staport.finance.gBanque.provision.repositories.ProvisionRepository;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionReversalHandler;

/**
 * Gère l'annulation d'une provision pour {@code ReverseTransactionService} — reprend à l'identique
 * la logique de l'ancien {@code case PROVISION} (méthode {@code resetProvision}) qu'il remplace.
 */
@Component
public class ProvisionReversalHandler implements TransactionReversalHandler {

    private final ProvisionRepository provisionRepository;

    public ProvisionReversalHandler(ProvisionRepository provisionRepository) {
        this.provisionRepository = provisionRepository;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.PROVISION;
    }

    @Override
    public void reset(Long referentId) {
        Provision provision = provisionRepository.findById(referentId)
                .orElseThrow(() -> new ResourceNotFoundException("Provision introuvable: " + referentId));
        provision.setStatus(Status.PREVU);
        provision.setDateOperation(null);
        provision.setDateValeur(null);
        provisionRepository.save(provision);
    }
}
