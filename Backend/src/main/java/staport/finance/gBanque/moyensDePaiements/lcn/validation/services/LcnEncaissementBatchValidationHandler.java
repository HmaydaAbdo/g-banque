package staport.finance.gBanque.moyensDePaiements.lcn.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.lcn.validation.dto.LcnTransactionRequest;
import staport.finance.gBanque.moyensDePaiements.lcn.validation.dto.LcnTransactionResponseDto;
import staport.finance.gBanque.moyensDePaiements.lcn.validation.services.interfaces.ILcnValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.OperationValideeDto;
import staport.finance.gBanque.validation.services.handlers.BatchValidationHandler;

/**
 * Gère la validation en lot des LCN d'encaissement (nature ENCAISSEMENT) pour
 * {@code BatchValidationService} — voir {@link LcnDecaissementBatchValidationHandler} pour le cas
 * particulier LCN (sens porté par {@code TransactionSource}, pas par {@code requestDto.sens()}) et
 * pour {@code LCN_ESCOMPTE} (volontairement sans handler).
 */
@Component
public class LcnEncaissementBatchValidationHandler implements BatchValidationHandler {

    private final ILcnValidationService lcnValidationService;

    public LcnEncaissementBatchValidationHandler(ILcnValidationService lcnValidationService) {
        this.lcnValidationService = lcnValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.LCN_ENCAISSEMENT;
    }

    @Override
    public OperationValideeDto processOne(Long id, BatchValidationRequestDto requestDto) {
        LcnTransactionRequest dto = new LcnTransactionRequest();
        dto.setId(id);
        dto.setDateValeur(requestDto.dateValeur());
        dto.setDateOperation(requestDto.dateOperation());
        dto.setTauxTva(requestDto.tauxTva());
        dto.setRapprochementId(requestDto.rapprochementId());
        LcnTransactionResponseDto r = lcnValidationService.processLcnTransaction(dto);
        return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
    }
}
