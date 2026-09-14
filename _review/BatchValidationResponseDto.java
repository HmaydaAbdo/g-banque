package staport.finance.gBanque.validation.dtos.lot;

import java.math.BigDecimal;
import java.util.List;

/**
 * Résultat d'une validation en lot : toutes les opérations demandées ont été validées
 * (le service garantit un traitement tout-ou-rien — voir {@code BatchValidationService}).
 */
public record BatchValidationResponseDto(
        List<OperationValideeDto> operationsValidees,
        int nombreOperations,
        BigDecimal montantTotalDebit,
        BigDecimal montantTotalCredit
) {

    public BatchValidationResponseDto(List<OperationValideeDto> operationsValidees,
                                       BigDecimal montantTotalDebit,
                                       BigDecimal montantTotalCredit) {
        this(operationsValidees, operationsValidees.size(), montantTotalDebit, montantTotalCredit);
    }
}
