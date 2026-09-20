package staport.finance.gBanque.validation.services.handlers;

import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.OperationValideeDto;

/**
 * Gère la validation en lot d'une source pour {@code BatchValidationService} : construit le DTO
 * unitaire de cette source à partir des champs communs du lot, appelle le service de validation
 * unitaire correspondant, et retourne le résultat agrégé.
 * <p>
 * Une implémentation par source, découverte automatiquement par Spring (chaque implémentation est
 * un {@code @Component}) et enregistrée dans {@code BatchValidationService} via {@link #source()} —
 * ajouter une source à la validation en lot ne demande plus de modifier
 * {@code BatchValidationService.processOne}, seulement d'ajouter une implémentation dans le module
 * de cette source.
 * <p>
 * Migration menée du 14 au 19/09/2026 : les 22 sources (dont les 3 LCN) sont désormais toutes
 * portées par ce patron ({@code LeasingBatchValidationHandler}, {@code CmtBatchValidationHandler},
 * {@code ChequeBatchValidationHandler}, {@code LcnDecaissementBatchValidationHandler},
 * {@code LcnEncaissementBatchValidationHandler}, {@code LcnEscompteBatchValidationHandler},
 * {@code VirementBatchValidationHandler}, {@code PrefinancementBatchValidationHandler},
 * {@code FactoringBatchValidationHandler}, {@code DroitConstateBatchValidationHandler},
 * {@code DecompteBatchValidationHandler}, {@code RestitutionBatchValidationHandler},
 * {@code ProvisionBatchValidationHandler}, {@code ImpayeeBatchValidationHandler},
 * {@code VirementInternationalBatchValidationHandler}, {@code MiseADispositionBatchValidationHandler},
 * {@code AgiosBatchValidationHandler}, {@code CommissionBatchValidationHandler},
 * {@code FraisBancaireBatchValidationHandler}, {@code PrelevementBatchValidationHandler},
 * {@code ImpotEtTaxeBatchValidationHandler}, {@code OperationErroneBatchValidationHandler}). Le
 * 19/09/2026, la fusion de la validation unitaire dans ce même registre (architecture.md, Règle 1)
 * a comblé le seul trou restant : {@code LcnEscompteBatchValidationHandler}, ajouté à cette
 * occasion — {@code TransactionSource.LCN_ESCOMPTE} avait sa propre validation unitaire (ancien
 * endpoint dédié, indifférent à la nature du LCN) mais jamais de handler de lot ; sans cet ajout,
 * la fusion aurait cassé la validation de ces LCN plutôt que de simplement supprimer un endpoint
 * devenu redondant.
 */
public interface BatchValidationHandler {

    /** Source gérée par cette implémentation — clé d'enregistrement dans le registre. */
    TransactionSource source();

    /**
     * Construit le DTO unitaire de cette source à partir des champs communs du lot, appelle le
     * service de validation unitaire correspondant, et retourne le résultat agrégé.
     */
    OperationValideeDto processOne(Long id, BatchValidationRequestDto requestDto);
}
