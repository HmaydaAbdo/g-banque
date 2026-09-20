package staport.finance.gBanque.leasing.echeancesLeasing.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.repositories.EcheanceLeasingRepository;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionReversalHandler;

/**
 * Gère l'annulation d'une échéance de leasing pour {@code ReverseTransactionService} — reprend à
 * l'identique la logique de l'ancien {@code case LEASING} (méthode {@code resetLeasing}) qu'il
 * remplace.
 */
@Component
public class LeasingReversalHandler implements TransactionReversalHandler {

    private final EcheanceLeasingRepository echeanceLeasingRepository;

    public LeasingReversalHandler(EcheanceLeasingRepository echeanceLeasingRepository) {
        this.echeanceLeasingRepository = echeanceLeasingRepository;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.LEASING;
    }

    @Override
    public void reset(Long referentId) {
        EcheanceLeasing echeance = echeanceLeasingRepository.findById(referentId)
                .orElseThrow(() -> new ResourceNotFoundException("Echeance leasing introuvable: " + referentId));
        echeance.setStatutPaiement(StatutEcheanceLeasing.PREVU);
        echeance.setDateValeur(null);
        echeance.setDateOperation(null);
        echeanceLeasingRepository.save(echeance);
    }
}
