package staport.finance.gBanque.moyensDePaiements.cheques.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.cheques.validation.dto.ChequeTransactionRequest;
import staport.finance.gBanque.moyensDePaiements.cheques.validation.dto.ChequeTransactionResponseDto;
import staport.finance.gBanque.moyensDePaiements.cheques.validation.services.interfaces.IChequeValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.OperationValideeDto;
import staport.finance.gBanque.validation.services.handlers.BatchValidationHandler;

/**
 * Gère la validation en lot des chèques pour {@code BatchValidationService}.
 * <p>
 * Le chèque a un {@code sens} propre (contrairement à Leasing/CMT) — {@code
 * BatchValidationService.processBatch} exige déjà ce champ pour CHEQUE via
 * {@code requireSensIfNeeded} avant d'appeler ce handler, mais {@link IChequeValidationService} n'a
 * plus qu'une seule méthode de validation depuis la fusion du 19/09/2026 (le sens est lu sur
 * l'entité par le mapper, jamais transmis par l'appelant) — ce handler n'a donc plus besoin de
 * brancher sur {@code requestDto.sens()}.
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
        ChequeTransactionResponseDto r = chequeValidationService.processChequeTransaction(dto);
        return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
    }
}
