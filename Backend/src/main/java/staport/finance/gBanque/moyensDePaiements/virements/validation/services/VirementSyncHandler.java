package staport.finance.gBanque.moyensDePaiements.virements.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.virements.validation.services.interfaces.IVirementValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionSyncHandler;

import java.time.LocalDate;

/**
 * Gère la synchronisation d'un virement depuis sa transaction pour
 * {@code TransactionServiceImpl} — reprend à l'identique l'ancien appel direct à
 * {@code virementValidationService.syncVirementFromTransaction}.
 */
@Component
public class VirementSyncHandler implements TransactionSyncHandler {

    private final IVirementValidationService virementValidationService;

    public VirementSyncHandler(IVirementValidationService virementValidationService) {
        this.virementValidationService = virementValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.Virement;
    }

    @Override
    public void sync(Long referentId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation) {
        virementValidationService.syncVirementFromTransaction(referentId, dateValeur, dateOperation, libelle, observation);
    }
}
