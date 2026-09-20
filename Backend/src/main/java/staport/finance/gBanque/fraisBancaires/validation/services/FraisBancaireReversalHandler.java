package staport.finance.gBanque.fraisBancaires.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.fraisBancaires.entities.FraisBancaire;
import staport.finance.gBanque.fraisBancaires.repositories.FraisBancaireRepository;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionReversalHandler;

/**
 * Gère l'annulation d'un frais bancaire pour {@code ReverseTransactionService} — reprend à
 * l'identique la logique de l'ancien {@code case FRAIS_BANCAIRE} (méthode
 * {@code resetFraisBancaire}) qu'il remplace.
 */
@Component
public class FraisBancaireReversalHandler implements TransactionReversalHandler {

    private final FraisBancaireRepository fraisBancaireRepository;

    public FraisBancaireReversalHandler(FraisBancaireRepository fraisBancaireRepository) {
        this.fraisBancaireRepository = fraisBancaireRepository;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.FRAIS_BANCAIRE;
    }

    @Override
    public void reset(Long referentId) {
        FraisBancaire fraisBancaire = fraisBancaireRepository.findById(referentId)
                .orElseThrow(() -> new ResourceNotFoundException("Frais bancaire introuvable: " + referentId));
        fraisBancaire.setStatus(Status.PREVU);
        fraisBancaire.setDateOperation(null);
        fraisBancaire.setDateValeur(null);
        fraisBancaireRepository.save(fraisBancaire);
    }
}
