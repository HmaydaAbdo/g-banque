package staport.finance.gBanque.moyensDePaiements.virements.validation.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.virements.validation.dto.VirementTransactionRequestDto;
import staport.finance.gBanque.moyensDePaiements.virements.validation.dto.VirementTransactionResponseDto;
import staport.finance.gBanque.moyensDePaiements.virements.validation.services.interfaces.IVirementValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.OperationValideeDto;
import staport.finance.gBanque.validation.services.handlers.BatchValidationHandler;

/**
 * Gère la validation en lot des virements pour {@code BatchValidationService}.
 * <p>
 * Le virement a un {@code sens} propre — {@code BatchValidationService.processBatch} exige déjà ce
 * champ pour Virement via {@code requireSensIfNeeded} avant d'appeler ce handler, mais
 * {@link IVirementValidationService} n'a plus qu'une seule méthode de validation depuis la fusion
 * du 19/09/2026 (le sens est lu sur l'entité, jamais transmis par l'appelant) — ce handler n'a donc
 * plus besoin de brancher sur {@code requestDto.sens()}.
 */
@Component
public class VirementBatchValidationHandler implements BatchValidationHandler {

    private final IVirementValidationService virementValidationService;

    public VirementBatchValidationHandler(IVirementValidationService virementValidationService) {
        this.virementValidationService = virementValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.Virement;
    }

    @Override
    public OperationValideeDto processOne(Long id, BatchValidationRequestDto requestDto) {
        VirementTransactionRequestDto dto = new VirementTransactionRequestDto();
        dto.setId(id);
        dto.setDateValeur(requestDto.dateValeur());
        dto.setDateOperation(requestDto.dateOperation());
        dto.setTauxTva(requestDto.tauxTva());
        dto.setMontantTva(requestDto.montantTva());
        dto.setObservation(requestDto.observation());
        dto.setLibelle(requestDto.libelle());
        dto.setRapprochementId(requestDto.rapprochementId());
        VirementTransactionResponseDto r = virementValidationService.processVirementTransaction(dto);
        return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
    }
}
