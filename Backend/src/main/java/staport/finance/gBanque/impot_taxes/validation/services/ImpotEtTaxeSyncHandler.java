package staport.finance.gBanque.impot_taxes.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.impot_taxes.validation.services.interfaces.IImpotEtTaxeValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.services.handlers.TransactionSyncHandler;

import java.time.LocalDate;

/**
 * Gère la synchronisation d'un impôt/taxe depuis sa transaction pour
 * {@code TransactionServiceImpl} — reprend à l'identique l'ancien appel direct à
 * {@code impotEtTaxeValidationService.syncImpotEtTaxeFromTransaction}.
 */
@Component
public class ImpotEtTaxeSyncHandler implements TransactionSyncHandler {

    private final IImpotEtTaxeValidationService impotEtTaxeValidationService;

    public ImpotEtTaxeSyncHandler(IImpotEtTaxeValidationService impotEtTaxeValidationService) {
        this.impotEtTaxeValidationService = impotEtTaxeValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.IMPOT_TAXE;
    }

    @Override
    public void sync(Long referentId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation) {
        impotEtTaxeValidationService.syncImpotEtTaxeFromTransaction(referentId, dateValeur, dateOperation, libelle, observation);
    }
}
