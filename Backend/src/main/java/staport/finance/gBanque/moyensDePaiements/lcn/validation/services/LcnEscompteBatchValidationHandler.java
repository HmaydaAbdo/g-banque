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
 * Gère la validation des LCN d'escompte (nature ESCOMPTE) pour {@code BatchValidationService}.
 * <p>
 * Ajouté le 19/09/2026 avec la fusion de la validation unitaire et en lot (architecture.md,
 * Règle 1) : {@code LCN_ESCOMPTE} n'avait jamais eu de handler de lot (l'ancien switch de
 * {@code BatchValidationService} n'avait pas de {@code case} pour cette valeur), mais avait
 * toujours sa propre validation unitaire via l'ancien endpoint dédié {@code /process/lcn}, déjà
 * indifférent à la nature du LCN ({@code LcnTransactionMapper} la lit directement sur l'entité).
 * Maintenant que la validation unitaire passe par ce même registre de handlers (il n'existe plus
 * d'endpoint dédié), l'absence de handler aurait cassé la validation des LCN d'escompte — ce
 * handler comble ce trou plutôt que de perdre la fonctionnalité existante.
 */
@Component
public class LcnEscompteBatchValidationHandler implements BatchValidationHandler {

    private final ILcnValidationService lcnValidationService;

    public LcnEscompteBatchValidationHandler(ILcnValidationService lcnValidationService) {
        this.lcnValidationService = lcnValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.LCN_ESCOMPTE;
    }

    @Override
    public OperationValideeDto processOne(Long id, BatchValidationRequestDto requestDto) {
        LcnTransactionRequest dto = new LcnTransactionRequest();
        dto.setId(id);
        dto.setDateValeur(requestDto.dateValeur());
        dto.setDateOperation(requestDto.dateOperation());
        dto.setTauxTva(requestDto.tauxTva());
        dto.setMontantTva(requestDto.montantTva());
        dto.setObservation(requestDto.observation());
        dto.setLibelle(requestDto.libelle());
        dto.setRapprochementId(requestDto.rapprochementId());
        LcnTransactionResponseDto r = lcnValidationService.processLcnTransaction(dto);
        return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
    }
}
