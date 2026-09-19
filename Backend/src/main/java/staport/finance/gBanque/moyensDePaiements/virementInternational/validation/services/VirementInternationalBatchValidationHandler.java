package staport.finance.gBanque.moyensDePaiements.virementInternational.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.virementInternational.validation.dto.VirementInternationalTransactionRequestDto;
import staport.finance.gBanque.moyensDePaiements.virementInternational.validation.dto.VirementInternationalTransactionResponseDto;
import staport.finance.gBanque.moyensDePaiements.virementInternational.validation.services.interfaces.IVirementInternationalValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.OperationValideeDto;
import staport.finance.gBanque.validation.services.handlers.BatchValidationHandler;

/**
 * Gère la validation en lot des virements internationaux pour {@code BatchValidationService}.
 * <p>
 * Le virement international a un {@code sens} propre — {@code BatchValidationService.processBatch}
 * exige déjà ce champ pour VIREMENT_INTERNATIONAL via {@code requireSensIfNeeded} avant d'appeler
 * ce handler, mais {@link IVirementInternationalValidationService} n'a plus qu'une seule méthode
 * de validation depuis la fusion du 19/09/2026 (le sens est lu sur l'entité, jamais transmis par
 * l'appelant) — ce handler n'a donc plus besoin de brancher sur {@code requestDto.sens()}.
 */
@Component
public class VirementInternationalBatchValidationHandler implements BatchValidationHandler {

    private final IVirementInternationalValidationService virementInternationalValidationService;

    public VirementInternationalBatchValidationHandler(IVirementInternationalValidationService virementInternationalValidationService) {
        this.virementInternationalValidationService = virementInternationalValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.VIREMENT_INTERNATIONAL;
    }

    @Override
    public OperationValideeDto processOne(Long id, BatchValidationRequestDto requestDto) {
        VirementInternationalTransactionRequestDto dto = new VirementInternationalTransactionRequestDto();
        dto.setId(id);
        dto.setDateValeur(requestDto.dateValeur());
        dto.setDateOperation(requestDto.dateOperation());
        dto.setTauxTva(requestDto.tauxTva());
        dto.setRapprochementId(requestDto.rapprochementId());
        VirementInternationalTransactionResponseDto r = virementInternationalValidationService.processVirementInternationalTransaction(dto);
        return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
    }
}
