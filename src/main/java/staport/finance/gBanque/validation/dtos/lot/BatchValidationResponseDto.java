package staport.finance.gBanque.validation.dtos.lot;

import java.math.BigDecimal;
import java.util.List;

/**
 * Résultat d'une validation en lot : toutes les opérations demandées ont été validées
 * (le service garantit un traitement tout-ou-rien — voir {@code BatchValidationService}).
 */
public class BatchValidationResponseDto {

    private final List<OperationValideeDto> operationsValidees;
    private final int nombreOperations;
    private final BigDecimal montantTotalDebit;
    private final BigDecimal montantTotalCredit;

    public BatchValidationResponseDto(List<OperationValideeDto> operationsValidees,
                                       BigDecimal montantTotalDebit,
                                       BigDecimal montantTotalCredit) {
        this.operationsValidees = operationsValidees;
        this.nombreOperations = operationsValidees.size();
        this.montantTotalDebit = montantTotalDebit;
        this.montantTotalCredit = montantTotalCredit;
    }

    public List<OperationValideeDto> getOperationsValidees() {
        return operationsValidees;
    }

    public int getNombreOperations() {
        return nombreOperations;
    }

    public BigDecimal getMontantTotalDebit() {
        return montantTotalDebit;
    }

    public BigDecimal getMontantTotalCredit() {
        return montantTotalCredit;
    }
}
