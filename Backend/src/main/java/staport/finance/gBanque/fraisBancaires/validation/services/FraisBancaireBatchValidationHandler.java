package staport.finance.gBanque.fraisBancaires.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.fraisBancaires.validation.dto.FraisBancaireTransactionRequestDto;
import staport.finance.gBanque.fraisBancaires.validation.dto.FraisBancaireTransactionResponseDto;
import staport.finance.gBanque.fraisBancaires.validation.services.interfaces.IFraisBancaireValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.OperationValideeDto;
import staport.finance.gBanque.validation.services.handlers.BatchValidationHandler;

/**
 * Gère la validation en lot des frais bancaires pour {@code BatchValidationService}.
 * <p>
 * Reprend à l'identique la logique du {@code case FRAIS_BANCAIRE} qu'il remplace dans
 * {@code BatchValidationService.processOne} (aucun changement de comportement) : Frais bancaire est
 * toujours un décaissement (sens fixe), un seul appel de service, aucun branchement sur
 * {@code sens} ici — même patron que {@code RestitutionBatchValidationHandler}.
 */
@Component
public class FraisBancaireBatchValidationHandler implements BatchValidationHandler {

    private final IFraisBancaireValidationService fraisBancaireValidationService;

    public FraisBancaireBatchValidationHandler(IFraisBancaireValidationService fraisBancaireValidationService) {
        this.fraisBancaireValidationService = fraisBancaireValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.FRAIS_BANCAIRE;
    }

    @Override
    public OperationValideeDto processOne(Long id, BatchValidationRequestDto requestDto) {
        FraisBancaireTransactionRequestDto dto = new FraisBancaireTransactionRequestDto(
                id, requestDto.tauxTva(), null, null,
                requestDto.dateValeur(), requestDto.dateOperation(),
                null, null, requestDto.rapprochementId());
        FraisBancaireTransactionResponseDto r = fraisBancaireValidationService.processFraisBancaireTransaction(dto);
        return new OperationValideeDto(id, r.id(), r.libelle(), r.debit(), r.credit());
    }
}
