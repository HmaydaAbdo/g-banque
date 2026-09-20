package staport.finance.gBanque.fraisBancaires.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.fraisBancaires.validation.services.interfaces.IFraisBancaireValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionSyncHandler;

import java.time.LocalDate;

/**
 * Gère la synchronisation d'un frais bancaire depuis sa transaction pour
 * {@code TransactionServiceImpl} — reprend à l'identique l'ancien appel direct à
 * {@code fraisBancaireValidationService.syncFraisBancaireFromTransaction}.
 */
@Component
public class FraisBancaireSyncHandler implements TransactionSyncHandler {

    private final IFraisBancaireValidationService fraisBancaireValidationService;

    public FraisBancaireSyncHandler(IFraisBancaireValidationService fraisBancaireValidationService) {
        this.fraisBancaireValidationService = fraisBancaireValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.FRAIS_BANCAIRE;
    }

    @Override
    public void sync(Long referentId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation) {
        fraisBancaireValidationService.syncFraisBancaireFromTransaction(referentId, dateValeur, dateOperation, libelle, observation);
    }
}
