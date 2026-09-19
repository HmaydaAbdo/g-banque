package staport.finance.gBanque.decomptes.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.decomptes.validation.dto.DecompteTransactionRequestDto;
import staport.finance.gBanque.decomptes.validation.dto.DecompteTransactionResponseDto;
import staport.finance.gBanque.decomptes.validation.services.interfaces.IDecompteValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.OperationValideeDto;
import staport.finance.gBanque.validation.services.handlers.BatchValidationHandler;

/**
 * Gère la validation en lot des décomptes pour {@code BatchValidationService}.
 * <p>
 * Reprend à l'identique la logique de l'ancien {@code case DECOMPTES} qu'il remplace. Un décompte
 * est toujours un encaissement (pas de {@code Type} à lire, contrairement à Préfinancement/
 * Factoring/Droit constaté) ; {@code banqueId} reste obligatoire pour vérifier la cohérence avec la
 * banque d'encaissement du décompte.
 */
@Component
public class DecompteBatchValidationHandler implements BatchValidationHandler {

    private final IDecompteValidationService decompteValidationService;

    public DecompteBatchValidationHandler(IDecompteValidationService decompteValidationService) {
        this.decompteValidationService = decompteValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.DECOMPTES;
    }

    @Override
    public OperationValideeDto processOne(Long id, BatchValidationRequestDto requestDto) {
        DecompteTransactionRequestDto dto = new DecompteTransactionRequestDto(
                id,
                requestDto.banqueId(),
                null,
                requestDto.dateValeur(),
                requestDto.dateOperation(),
                requestDto.tauxTva(),
                null,
                null,
                null,
                requestDto.rapprochementId());
        DecompteTransactionResponseDto r = decompteValidationService.processDecompteTransaction(dto);
        return new OperationValideeDto(id, r.id(), r.libelle(), r.debit(), r.credit());
    }
}
