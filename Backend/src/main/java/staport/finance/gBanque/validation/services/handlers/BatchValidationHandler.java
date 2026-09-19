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
 * Migration menée du 14 au 19/09/2026 : les 20 sources de la validation en lot sont désormais
 * toutes portées par ce patron ({@code LeasingBatchValidationHandler},
 * {@code CmtBatchValidationHandler}, {@code ChequeBatchValidationHandler},
 * {@code LcnDecaissementBatchValidationHandler}, {@code LcnEncaissementBatchValidationHandler},
 * {@code VirementBatchValidationHandler}, {@code PrefinancementBatchValidationHandler},
 * {@code FactoringBatchValidationHandler}, {@code DroitConstateBatchValidationHandler},
 * {@code DecompteBatchValidationHandler}, {@code RestitutionBatchValidationHandler},
 * {@code ProvisionBatchValidationHandler}, {@code ImpayeeBatchValidationHandler},
 * {@code VirementInternationalBatchValidationHandler}, {@code MiseADispositionBatchValidationHandler},
 * {@code AgiosBatchValidationHandler}, {@code CommissionBatchValidationHandler},
 * {@code FraisBancaireBatchValidationHandler}, {@code PrelevementBatchValidationHandler},
 * {@code ImpotEtTaxeBatchValidationHandler}, {@code OperationErroneBatchValidationHandler}), chacun
 * en remplacement à l'identique du {@code case} qu'il portait auparavant dans le switch historique
 * de {@code BatchValidationService.processOne} (désormais supprimé) — aucun changement de
 * comportement. {@code TransactionSource.LCN_ESCOMPTE} n'a jamais eu de handler ni de case — non
 * pris en charge en lot, avant comme après cette migration.
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
