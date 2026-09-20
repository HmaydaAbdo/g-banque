package staport.finance.gBanque.impayees.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.impayees.validation.dto.ImpayeeTransactionRequestDto;
import staport.finance.gBanque.impayees.validation.dto.ImpayeeTransactionResponseDto;
import staport.finance.gBanque.impayees.validation.services.interfaces.IImpayeeValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.OperationValideeDto;
import staport.finance.gBanque.validation.services.handlers.BatchValidationHandler;

/**
 * Gère la validation en lot des impayées pour {@code BatchValidationService}.
 * <p>
 * Reprend à l'identique la logique de l'ancien {@code case IMPAYEE} qu'il remplace. Le sens
 * (décaissement/encaissement) est porté par {@code Impayee.type} (champ {@code Type} direct sur
 * l'entité), lu uniquement par {@code ImpayeeTransactionMapper} — un seul appel de service, aucun
 * branchement sur {@code requestDto.sens()} ici (contrairement à Virement/Cheque). Pas de
 * {@code banqueId} propre : la banque est résolue depuis l'entité elle-même.
 */
@Component
public class ImpayeeBatchValidationHandler implements BatchValidationHandler {

    private final IImpayeeValidationService impayeeValidationService;

    public ImpayeeBatchValidationHandler(IImpayeeValidationService impayeeValidationService) {
        this.impayeeValidationService = impayeeValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.IMPAYEE;
    }

    @Override
    public OperationValideeDto processOne(Long id, BatchValidationRequestDto requestDto) {
        ImpayeeTransactionRequestDto dto = new ImpayeeTransactionRequestDto(
                id, requestDto.tauxTva(), requestDto.montantTva(), null,
                requestDto.dateValeur(), requestDto.dateOperation(),
                requestDto.observation(), requestDto.libelle(), requestDto.rapprochementId());
        ImpayeeTransactionResponseDto r = impayeeValidationService.processImpayeeTransaction(dto);
        return new OperationValideeDto(id, r.id(), r.libelle(), r.debit(), r.credit());
    }
}
