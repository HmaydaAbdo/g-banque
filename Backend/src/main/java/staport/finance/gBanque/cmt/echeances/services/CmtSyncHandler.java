package staport.finance.gBanque.cmt.echeances.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.cmt.echeances.services.interfaces.ICmtValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionSyncHandler;

import java.time.LocalDate;

/**
 * Gère la synchronisation d'une échéance CMT depuis sa transaction pour
 * {@code TransactionServiceImpl} — reprend à l'identique l'ancien appel direct à
 * {@code cmtValidationService.syncEcheanceFromTransaction}.
 */
@Component
public class CmtSyncHandler implements TransactionSyncHandler {

    private final ICmtValidationService cmtValidationService;

    public CmtSyncHandler(ICmtValidationService cmtValidationService) {
        this.cmtValidationService = cmtValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.CMT;
    }

    @Override
    public void sync(Long referentId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation) {
        cmtValidationService.syncEcheanceFromTransaction(referentId, dateValeur, dateOperation, libelle, observation);
    }
}
