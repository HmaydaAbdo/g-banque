package staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.dto.MiseAdispositionTransactionRequestDto;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.dto.MiseAdispositionTransactionResponseDto;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.services.interfaces.IMiseADispositionValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.OperationValideeDto;
import staport.finance.gBanque.validation.services.handlers.BatchValidationHandler;

/**
 * Gère la validation en lot des mises à disposition pour {@code BatchValidationService}.
 * <p>
 * La mise à disposition a un {@code sens} propre — {@code BatchValidationService.processBatch}
 * exige déjà ce champ pour MiseADisposition via {@code requireSensIfNeeded} avant d'appeler ce
 * handler, mais {@link IMiseADispositionValidationService} n'a plus qu'une seule méthode de
 * validation depuis la fusion du 19/09/2026 (le sens est lu sur l'entité, jamais transmis par
 * l'appelant) — ce handler n'a donc plus besoin de brancher sur {@code requestDto.sens()}.
 */
@Component
public class MiseADispositionBatchValidationHandler implements BatchValidationHandler {

    private final IMiseADispositionValidationService miseADispositionValidationService;

    public MiseADispositionBatchValidationHandler(IMiseADispositionValidationService miseADispositionValidationService) {
        this.miseADispositionValidationService = miseADispositionValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.MiseADisposition;
    }

    @Override
    public OperationValideeDto processOne(Long id, BatchValidationRequestDto requestDto) {
        MiseAdispositionTransactionRequestDto dto = new MiseAdispositionTransactionRequestDto();
        dto.setId(id);
        dto.setDateValeur(requestDto.dateValeur());
        dto.setDateOperation(requestDto.dateOperation());
        dto.setTauxTva(requestDto.tauxTva());
        dto.setRapprochementId(requestDto.rapprochementId());
        MiseAdispositionTransactionResponseDto r = miseADispositionValidationService.processMiseAdispositionTransaction(dto);
        return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
    }
}
