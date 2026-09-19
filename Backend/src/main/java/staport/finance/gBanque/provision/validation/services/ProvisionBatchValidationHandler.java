package staport.finance.gBanque.provision.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.provision.validation.dto.ProvisionTransactionRequestDto;
import staport.finance.gBanque.provision.validation.dto.ProvisionTransactionResponseDto;
import staport.finance.gBanque.provision.validation.services.interfaces.IProvisionValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.OperationValideeDto;
import staport.finance.gBanque.validation.services.handlers.BatchValidationHandler;

/**
 * Gère la validation en lot des provisions pour {@code BatchValidationService}.
 * <p>
 * Reprend à l'identique la logique de l'ancien {@code case PROVISION} qu'il remplace. Le sens
 * (décaissement/encaissement) est porté par {@code Provision.sens} (champ {@code SensOperation}
 * direct sur l'entité), lu uniquement par {@code ProvisionTransactionMapper} — un seul appel de
 * service, aucun branchement sur {@code requestDto.sens()} ici (contrairement à Virement/Cheque).
 * Pas de {@code banqueId} propre : la banque est résolue depuis l'entité elle-même.
 */
@Component
public class ProvisionBatchValidationHandler implements BatchValidationHandler {

    private final IProvisionValidationService provisionValidationService;

    public ProvisionBatchValidationHandler(IProvisionValidationService provisionValidationService) {
        this.provisionValidationService = provisionValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.PROVISION;
    }

    @Override
    public OperationValideeDto processOne(Long id, BatchValidationRequestDto requestDto) {
        ProvisionTransactionRequestDto dto = new ProvisionTransactionRequestDto(
                id, requestDto.tauxTva(), null, null,
                requestDto.dateValeur(), requestDto.dateOperation(),
                null, null, requestDto.rapprochementId());
        ProvisionTransactionResponseDto r = provisionValidationService.processProvisionTransaction(dto);
        return new OperationValideeDto(id, r.id(), r.libelle(), r.debit(), r.credit());
    }
}
