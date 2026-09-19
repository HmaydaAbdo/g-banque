package staport.finance.gBanque.factoring.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.factoring.validation.dto.FactoringTransactionRequest;
import staport.finance.gBanque.factoring.validation.dto.FactoringTransactionResponse;
import staport.finance.gBanque.factoring.validation.services.interfaces.IFactoringValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.OperationValideeDto;
import staport.finance.gBanque.validation.services.handlers.BatchValidationHandler;

/**
 * Gère la validation en lot des factorings pour {@code BatchValidationService}.
 * <p>
 * Reprend à l'identique la logique de l'ancien {@code case FACTORING} — voir Javadoc de
 * {@code PrefinancementBatchValidationHandler} pour le cas particulier partagé par les 3 sources
 * marché : sens porté par {@code Factoring.type}, jamais transmis par l'appelant ; {@code banqueId}
 * obligatoire pour vérifier la cohérence avec la banque du marché associé.
 */
@Component
public class FactoringBatchValidationHandler implements BatchValidationHandler {

    private final IFactoringValidationService factoringValidationService;

    public FactoringBatchValidationHandler(IFactoringValidationService factoringValidationService) {
        this.factoringValidationService = factoringValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.FACTORING;
    }

    @Override
    public OperationValideeDto processOne(Long id, BatchValidationRequestDto requestDto) {
        FactoringTransactionRequest dto = new FactoringTransactionRequest();
        dto.setFactoringId(id);
        dto.setBanqueId(requestDto.banqueId());
        dto.setDateValeur(requestDto.dateValeur());
        dto.setDateOperation(requestDto.dateOperation());
        dto.setTauxTva(requestDto.tauxTva());
        dto.setRapprochementId(requestDto.rapprochementId());
        FactoringTransactionResponse r = factoringValidationService.processFactoringTransaction(dto);
        return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
    }
}
