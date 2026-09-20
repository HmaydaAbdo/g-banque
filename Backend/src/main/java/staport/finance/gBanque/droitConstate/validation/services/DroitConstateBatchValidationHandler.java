package staport.finance.gBanque.droitConstate.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.droitConstate.validation.dto.DroitConstateTransactionRequest;
import staport.finance.gBanque.droitConstate.validation.dto.DroitConstateTransactionResponse;
import staport.finance.gBanque.droitConstate.validation.services.interfaces.IDroitConstateValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.OperationValideeDto;
import staport.finance.gBanque.validation.services.handlers.BatchValidationHandler;

/**
 * Gère la validation en lot des droits constatés pour {@code BatchValidationService}.
 * <p>
 * Reprend à l'identique la logique de l'ancien {@code case DROIT_CONSTATE} — voir Javadoc de
 * {@code PrefinancementBatchValidationHandler} pour le cas particulier partagé par les 3 sources
 * marché (Préfinancement/Factoring/Droit constaté) : sens porté par {@code DroitConstate.type},
 * jamais transmis par l'appelant ; {@code banqueId} obligatoire pour vérifier la cohérence avec la
 * banque propre au droit constaté (pas celle du marché, contrairement à Préfinancement/Factoring).
 */
@Component
public class DroitConstateBatchValidationHandler implements BatchValidationHandler {

    private final IDroitConstateValidationService droitConstateValidationService;

    public DroitConstateBatchValidationHandler(IDroitConstateValidationService droitConstateValidationService) {
        this.droitConstateValidationService = droitConstateValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.DROIT_CONSTATE;
    }

    @Override
    public OperationValideeDto processOne(Long id, BatchValidationRequestDto requestDto) {
        DroitConstateTransactionRequest dto = new DroitConstateTransactionRequest();
        dto.setDroitConstateId(id);
        dto.setBanqueId(requestDto.banqueId());
        dto.setDateValeur(requestDto.dateValeur());
        dto.setDateOperation(requestDto.dateOperation());
        dto.setTauxTva(requestDto.tauxTva());
        dto.setMontantTva(requestDto.montantTva());
        dto.setObservation(requestDto.observation());
        dto.setLibelle(requestDto.libelle());
        dto.setRapprochementId(requestDto.rapprochementId());
        DroitConstateTransactionResponse r = droitConstateValidationService.processDroitConstateTransaction(dto);
        return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
    }
}
