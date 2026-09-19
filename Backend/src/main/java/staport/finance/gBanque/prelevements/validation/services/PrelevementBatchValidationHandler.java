package staport.finance.gBanque.prelevements.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.prelevements.validation.dto.PrelevementTransactionResponse;
import staport.finance.gBanque.prelevements.validation.dto.PrelevementValidationRequest;
import staport.finance.gBanque.prelevements.validation.services.interfaces.IPrelevementValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.OperationValideeDto;
import staport.finance.gBanque.validation.services.handlers.BatchValidationHandler;

/**
 * Gère la validation en lot des prélèvements pour {@code BatchValidationService}.
 * <p>
 * Reprend à l'identique la logique du {@code case PRELEVEMENT} qu'il remplace dans
 * {@code BatchValidationService.processOne} (aucun changement de comportement) : Prélèvement est
 * toujours un décaissement (sens fixe), un seul appel de service, aucun branchement sur
 * {@code sens} ici — même patron que {@code RestitutionBatchValidationHandler}.
 */
@Component
public class PrelevementBatchValidationHandler implements BatchValidationHandler {

    private final IPrelevementValidationService prelevementValidationService;

    public PrelevementBatchValidationHandler(IPrelevementValidationService prelevementValidationService) {
        this.prelevementValidationService = prelevementValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.PRELEVEMENT;
    }

    @Override
    public OperationValideeDto processOne(Long id, BatchValidationRequestDto requestDto) {
        PrelevementValidationRequest dto = new PrelevementValidationRequest();
        dto.setIdPrelevement(id);
        dto.setDateValeur(requestDto.dateValeur());
        dto.setDateOperation(requestDto.dateOperation());
        dto.setTauxTva(requestDto.tauxTva());
        dto.setRapprochementId(requestDto.rapprochementId());
        PrelevementTransactionResponse r = prelevementValidationService.processPrelevementToTransaction(dto);
        return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
    }
}
