package staport.finance.gBanque.restitution.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.restitution.validation.dto.RestitutionTransactionRequestDto;
import staport.finance.gBanque.restitution.validation.dto.RestitutionTransactionResponseDto;
import staport.finance.gBanque.restitution.validation.services.interfaces.IRestitutionValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.OperationValideeDto;
import staport.finance.gBanque.validation.services.handlers.BatchValidationHandler;

/**
 * Gère la validation en lot des restitutions pour {@code BatchValidationService}.
 * <p>
 * Reprend à l'identique la logique de l'ancien {@code case RESTITUTION} qu'il remplace. Une
 * restitution est toujours un encaissement (aucun champ de sens à lire) et ne porte pas de
 * {@code banqueId} propre : la banque est résolue depuis l'entité elle-même
 * ({@code Restitution.banque}), pas transmise par l'appelant — contrairement aux sources
 * « marché » (Préfinancement/Factoring/Droit constaté/Décomptes).
 */
@Component
public class RestitutionBatchValidationHandler implements BatchValidationHandler {

    private final IRestitutionValidationService restitutionValidationService;

    public RestitutionBatchValidationHandler(IRestitutionValidationService restitutionValidationService) {
        this.restitutionValidationService = restitutionValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.RESTITUTION;
    }

    @Override
    public OperationValideeDto processOne(Long id, BatchValidationRequestDto requestDto) {
        RestitutionTransactionRequestDto dto = new RestitutionTransactionRequestDto(
                id, requestDto.tauxTva(), null, null,
                requestDto.dateValeur(), requestDto.dateOperation(),
                null, null, requestDto.rapprochementId());
        RestitutionTransactionResponseDto r = restitutionValidationService.processRestitutionTransaction(dto);
        return new OperationValideeDto(id, r.id(), r.libelle(), r.debit(), r.credit());
    }
}
