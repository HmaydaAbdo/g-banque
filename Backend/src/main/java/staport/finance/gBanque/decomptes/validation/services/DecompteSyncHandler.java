package staport.finance.gBanque.decomptes.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.decomptes.validation.services.interfaces.IDecompteValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionSyncHandler;

import java.time.LocalDate;

/**
 * Gère la synchronisation d'un décompte depuis sa transaction pour {@code TransactionServiceImpl}
 * — reprend à l'identique l'ancien appel direct à
 * {@code decompteValidationService.syncDecompteFromTransaction}.
 */
@Component
public class DecompteSyncHandler implements TransactionSyncHandler {

    private final IDecompteValidationService decompteValidationService;

    public DecompteSyncHandler(IDecompteValidationService decompteValidationService) {
        this.decompteValidationService = decompteValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.DECOMPTES;
    }

    @Override
    public void sync(Long referentId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation) {
        decompteValidationService.syncDecompteFromTransaction(referentId, dateValeur, dateOperation, libelle, observation);
    }
}
