package staport.finance.gBanque.Agios.service;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Agios.dtos.AgiosTransactionResponse;
import staport.finance.gBanque.Agios.dtos.AgiosValidationRequest;
import staport.finance.gBanque.Agios.service.interfaces.IAgiosValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.OperationValideeDto;
import staport.finance.gBanque.validation.services.handlers.BatchValidationHandler;

/**
 * Gère la validation en lot des agios pour {@code BatchValidationService}.
 * <p>
 * Reprend à l'identique la logique du {@code case AGIOS} qu'il remplace dans
 * {@code BatchValidationService.processOne} (aucun changement de comportement) : Agios est toujours
 * un décaissement (sens fixe), un seul appel de service, aucun branchement sur {@code sens} ici —
 * même patron que {@code RestitutionBatchValidationHandler}.
 */
@Component
public class AgiosBatchValidationHandler implements BatchValidationHandler {

    private final IAgiosValidationService agiosValidationService;

    public AgiosBatchValidationHandler(IAgiosValidationService agiosValidationService) {
        this.agiosValidationService = agiosValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.AGIOS;
    }

    @Override
    public OperationValideeDto processOne(Long id, BatchValidationRequestDto requestDto) {
        AgiosValidationRequest dto = new AgiosValidationRequest(
                id,
                requestDto.tauxTva(),
                requestDto.montantTva(),
                null,
                requestDto.dateValeur(),
                requestDto.dateOperation(),
                requestDto.observation(),
                requestDto.libelle(),
                requestDto.rapprochementId());
        AgiosTransactionResponse r = agiosValidationService.processAgiosToTransaction(dto);
        return new OperationValideeDto(id, r.id(), r.libelle(), r.debit(), r.credit());
    }
}
