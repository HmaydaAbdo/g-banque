package staport.finance.gBanque.Agios.service;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Agios.service.interfaces.IAgiosValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionSyncHandler;

import java.time.LocalDate;

/**
 * Gère la synchronisation d'un agios depuis sa transaction pour {@code TransactionServiceImpl} —
 * reprend à l'identique l'ancien appel direct à
 * {@code agiosValidationService.syncAgiosFromTransaction}.
 */
@Component
public class AgiosSyncHandler implements TransactionSyncHandler {

    private final IAgiosValidationService agiosValidationService;

    public AgiosSyncHandler(IAgiosValidationService agiosValidationService) {
        this.agiosValidationService = agiosValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.AGIOS;
    }

    @Override
    public void sync(Long referentId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation) {
        agiosValidationService.syncAgiosFromTransaction(referentId, dateValeur, dateOperation, libelle, observation);
    }
}
