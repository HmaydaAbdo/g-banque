package staport.finance.gBanque.moyensDePaiements.lcn.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.lcn.validation.dto.LcnTransactionRequest;
import staport.finance.gBanque.moyensDePaiements.lcn.validation.dto.LcnTransactionResponseDto;
import staport.finance.gBanque.moyensDePaiements.lcn.validation.services.interfaces.ILcnValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.OperationValideeDto;
import staport.finance.gBanque.validation.services.handlers.BatchValidationHandler;

/**
 * Gère la validation en lot des LCN de décaissement pour {@code BatchValidationService}.
 * <p>
 * Cas particulier LCN (contrairement à Virement/Chèque) : le sens n'est pas porté par
 * {@code requestDto.sens()} mais directement par {@code TransactionSource} — d'où deux handlers
 * distincts ({@code LcnDecaissementBatchValidationHandler}, {@code LcnEncaissementBatchValidationHandler})
 * plutôt qu'un seul avec branchement sur le sens ; ce partage par clé de registre ne change pas
 * avec la fusion du 19/09/2026 des méthodes {@code ILcnValidationService} — seule la couche
 * service n'a désormais plus qu'une méthode. {@code TransactionSource.LCN_ESCOMPTE} n'a
 * volontairement pas de handler : il n'était déjà pas géré par l'ancien switch (retombait sur le
 * {@code default} et levait une exception), comportement inchangé.
 */
@Component
public class LcnDecaissementBatchValidationHandler implements BatchValidationHandler {

    private final ILcnValidationService lcnValidationService;

    public LcnDecaissementBatchValidationHandler(ILcnValidationService lcnValidationService) {
        this.lcnValidationService = lcnValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.LCN_DECAISSEMENT;
    }

    @Override
    public OperationValideeDto processOne(Long id, BatchValidationRequestDto requestDto) {
        LcnTransactionRequest dto = new LcnTransactionRequest();
        dto.setId(id);
        dto.setDateValeur(requestDto.dateValeur());
        dto.setDateOperation(requestDto.dateOperation());
        dto.setTauxTva(requestDto.tauxTva());
        dto.setRapprochementId(requestDto.rapprochementId());
        LcnTransactionResponseDto r = lcnValidationService.processLcnTransaction(dto);
        return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
    }
}
