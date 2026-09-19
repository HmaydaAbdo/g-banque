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
 * Migration en cours (14-19/09/2026) : Leasing, CMT et Chèque sont les trois premières sources
 * migrées vers ce patron ({@code LeasingBatchValidationHandler}, {@code CmtBatchValidationHandler},
 * {@code ChequeBatchValidationHandler}), en remplacement à l'identique des
 * {@code case LEASING}/{@code case CMT}/{@code case CHEQUE} qu'ils portaient auparavant — aucun
 * changement de comportement. Les 19 autres sources restent gérées par le switch historique de
 * {@code BatchValidationService.processOne}, à migrer une par une.
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
