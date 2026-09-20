package staport.finance.gBanque.commision.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.commision.validation.dto.CommissionTransactionRequestDto;
import staport.finance.gBanque.commision.validation.dto.CommissionTransactionResponseDto;
import staport.finance.gBanque.commision.validation.services.interfaces.ICommissionValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.OperationValideeDto;
import staport.finance.gBanque.validation.services.handlers.BatchValidationHandler;

/**
 * Gère la validation en lot des commissions pour {@code BatchValidationService}.
 * <p>
 * Reprend à l'identique la logique du {@code case COMMISSION} qu'il remplace dans
 * {@code BatchValidationService.processOne} (aucun changement de comportement) : Commission est
 * toujours un décaissement (sens fixe), un seul appel de service, aucun branchement sur
 * {@code sens} ici — même patron que {@code RestitutionBatchValidationHandler}.
 */
@Component
public class CommissionBatchValidationHandler implements BatchValidationHandler {

    private final ICommissionValidationService commissionValidationService;

    public CommissionBatchValidationHandler(ICommissionValidationService commissionValidationService) {
        this.commissionValidationService = commissionValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.COMMISSION;
    }

    @Override
    public OperationValideeDto processOne(Long id, BatchValidationRequestDto requestDto) {
        CommissionTransactionRequestDto dto = new CommissionTransactionRequestDto(
                id, requestDto.tauxTva(), requestDto.montantTva(), null,
                requestDto.dateValeur(), requestDto.dateOperation(),
                requestDto.observation(), requestDto.libelle(), requestDto.rapprochementId());
        CommissionTransactionResponseDto r = commissionValidationService.processCommissionTransaction(dto);
        return new OperationValideeDto(id, r.id(), r.libelle(), r.debit(), r.credit());
    }
}
