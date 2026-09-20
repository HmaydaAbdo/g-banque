package staport.finance.gBanque.moyensDePaiements.virementInternational.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.virementInternational.validation.services.interfaces.IVirementInternationalValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionSyncHandler;

import java.time.LocalDate;

/**
 * Gère la synchronisation d'un virement international depuis sa transaction pour
 * {@code TransactionServiceImpl} — reprend à l'identique l'ancien appel direct à
 * {@code virementInternationalValidationService.syncVirementInternationalFromTransaction}.
 */
@Component
public class VirementInternationalSyncHandler implements TransactionSyncHandler {

    private final IVirementInternationalValidationService virementInternationalValidationService;

    public VirementInternationalSyncHandler(IVirementInternationalValidationService virementInternationalValidationService) {
        this.virementInternationalValidationService = virementInternationalValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.VIREMENT_INTERNATIONAL;
    }

    @Override
    public void sync(Long referentId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation) {
        virementInternationalValidationService.syncVirementInternationalFromTransaction(referentId, dateValeur, dateOperation, libelle, observation);
    }
}
