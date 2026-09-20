package staport.finance.gBanque.decomptes.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.decomptes.entities.Decompte;
import staport.finance.gBanque.decomptes.repositories.DecompteRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionReversalHandler;

/**
 * Gère l'annulation d'un décompte pour {@code ReverseTransactionService} — reprend à l'identique
 * la logique de l'ancien {@code case DECOMPTES} (méthode {@code resetDecompte}) qu'il remplace.
 */
@Component
public class DecompteReversalHandler implements TransactionReversalHandler {

    private final DecompteRepository decompteRepository;

    public DecompteReversalHandler(DecompteRepository decompteRepository) {
        this.decompteRepository = decompteRepository;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.DECOMPTES;
    }

    @Override
    public void reset(Long referentId) {
        Decompte decompte = decompteRepository.findById(referentId)
                .orElseThrow(() -> new ResourceNotFoundException("Décompte introuvable: " + referentId));
        decompte.setStatus(Status.PREVU);
        decompte.setDateOperation(null);
        decompte.setDateValeur(null);
        decompteRepository.save(decompte);
    }
}
