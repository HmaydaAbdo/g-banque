package staport.finance.gBanque.prefinancement.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.prefinancement.validation.services.interfaces.IPrefinancementValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionSyncHandler;

import java.time.LocalDate;

/**
 * Gère la synchronisation d'un préfinancement depuis sa transaction pour
 * {@code TransactionServiceImpl} — reprend à l'identique l'ancien appel direct à
 * {@code prefinancementValidationService.syncPrefinancementFromTransaction}.
 */
@Component
public class PrefinancementSyncHandler implements TransactionSyncHandler {

    private final IPrefinancementValidationService prefinancementValidationService;

    public PrefinancementSyncHandler(IPrefinancementValidationService prefinancementValidationService) {
        this.prefinancementValidationService = prefinancementValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.PREFINANCEMENT;
    }

    @Override
    public void sync(Long referentId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation) {
        prefinancementValidationService.syncPrefinancementFromTransaction(referentId, dateValeur, dateOperation, libelle, observation);
    }
}
