package staport.finance.gBanque.moyensDePaiements.cheques.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.cheques.validation.dto.ChequeTransactionRequest;
import staport.finance.gBanque.moyensDePaiements.cheques.validation.dto.ChequeTransactionResponseDto;
import staport.finance.gBanque.moyensDePaiements.cheques.validation.services.interfaces.IChequeValidationService;
import staport.finance.gBanque.transactions.enums.SensOperation;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.OperationValideeDto;
import staport.finance.gBanque.validation.services.handlers.BatchValidationHandler;

/**
 * Gère la validation en lot des chèques pour {@code BatchValidationService}.
 * <p>
 * Reprend à l'identique la logique du {@code case CHEQUE} qu'il remplace dans
 * {@code BatchValidationService.processOne} (aucun changement de comportement) : le chèque a un
 * {@code sens} propre (contrairement à Leasing/CMT) — {@code BatchValidationService.processBatch}
 * exige déjà ce champ pour CHEQUE via {@code requireSensIfNeeded} avant d'appeler ce handler.
 */
@Component
public class ChequeBatchValidationHandler implements BatchValidationHandler {

    private final IChequeValidationService chequeValidationService;

    public ChequeBatchValidationHandler(IChequeValidationService chequeValidationService) {
        this.chequeValidationService = chequeValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.CHEQUE;
    }

    @Override
    public OperationValideeDto processOne(Long id, BatchValidationRequestDto requestDto) {
        ChequeTransactionRequest dto = new ChequeTransactionRequest();
        dto.setIdCheque(id);
        dto.setDateValeur(requestDto.dateValeur());
        dto.setDateOperation(requestDto.dateOperation());
        dto.setTauxTva(requestDto.tauxTva());
        dto.setRapprochementId(requestDto.rapprochementId());
        ChequeTransactionResponseDto r = requestDto.sens() == SensOperation.DECAISSEMENT
                ? chequeValidationService.processChequeDecaissementTransaction(dto)
                : chequeValidationService.processChequeEncaissementTransaction(dto);
        return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
    }
}
