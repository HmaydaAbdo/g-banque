package staport.finance.gBanque.impot_taxes.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.impot_taxes.entities.ImpotEtTaxe;
import staport.finance.gBanque.impot_taxes.repositories.ImpotEtTaxeRepository;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionReversalHandler;

/**
 * Gère l'annulation d'un impôt/taxe pour {@code ReverseTransactionService} — reprend à l'identique
 * la logique de l'ancien {@code case IMPOT_TAXE} (méthode {@code resetImpotEtTaxe}) qu'il
 * remplace.
 */
@Component
public class ImpotEtTaxeReversalHandler implements TransactionReversalHandler {

    private final ImpotEtTaxeRepository impotEtTaxeRepository;

    public ImpotEtTaxeReversalHandler(ImpotEtTaxeRepository impotEtTaxeRepository) {
        this.impotEtTaxeRepository = impotEtTaxeRepository;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.IMPOT_TAXE;
    }

    @Override
    public void reset(Long referentId) {
        ImpotEtTaxe impotEtTaxe = impotEtTaxeRepository.findById(referentId)
                .orElseThrow(() -> new ResourceNotFoundException("Impôt/Taxe introuvable: " + referentId));
        impotEtTaxe.setStatus(Status.PREVU);
        impotEtTaxe.setDateOperation(null);
        impotEtTaxe.setDateValeur(null);
        impotEtTaxeRepository.save(impotEtTaxe);
    }
}
