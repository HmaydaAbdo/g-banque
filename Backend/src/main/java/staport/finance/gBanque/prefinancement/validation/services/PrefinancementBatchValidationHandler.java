package staport.finance.gBanque.prefinancement.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.prefinancement.validation.dto.PrefinancementTransactionRequest;
import staport.finance.gBanque.prefinancement.validation.dto.PrefinancementTransactionResponse;
import staport.finance.gBanque.prefinancement.validation.services.interfaces.IPrefinancementValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.OperationValideeDto;
import staport.finance.gBanque.validation.services.handlers.BatchValidationHandler;

/**
 * Gère la validation en lot des préfinancements pour {@code BatchValidationService}.
 * <p>
 * Reprend à l'identique la logique de l'ancien {@code case PREFINANCEMENT} qu'il remplace (aucun
 * changement de comportement). Cas particulier (contrairement à Cheque/Virement) : le sens
 * (remboursement/déblocage) n'est jamais transmis par l'appelant — porté par
 * {@code Prefinancement.type}, lu côté serveur par {@code PrefinancementTransactionMapper} — donc
 * un seul appel de service, sans branchement sur {@code requestDto.sens()}. En revanche
 * {@code banqueId} est obligatoire ici (déjà exigé par {@code requireBanqueIdIfNeeded}) : il n'est
 * pas déductible de l'entité seule, il sert à vérifier que la banque fournie correspond bien à
 * celle du marché associé.
 */
@Component
public class PrefinancementBatchValidationHandler implements BatchValidationHandler {

    private final IPrefinancementValidationService prefinancementValidationService;

    public PrefinancementBatchValidationHandler(IPrefinancementValidationService prefinancementValidationService) {
        this.prefinancementValidationService = prefinancementValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.PREFINANCEMENT;
    }

    @Override
    public OperationValideeDto processOne(Long id, BatchValidationRequestDto requestDto) {
        PrefinancementTransactionRequest dto = new PrefinancementTransactionRequest();
        dto.setPrefinancementId(id);
        dto.setBanqueId(requestDto.banqueId());
        dto.setDateValeur(requestDto.dateValeur());
        dto.setDateOperation(requestDto.dateOperation());
        dto.setTauxTva(requestDto.tauxTva());
        dto.setMontantTva(requestDto.montantTva());
        dto.setObservation(requestDto.observation());
        dto.setLibelle(requestDto.libelle());
        dto.setRapprochementId(requestDto.rapprochementId());
        PrefinancementTransactionResponse r = prefinancementValidationService.processPrefinancementTransaction(dto);
        return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
    }
}
