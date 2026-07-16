package staport.finance.gBanque.transactions.dtos;

import java.math.BigDecimal;

/**
 * Agrégats (débit/crédit/TVA) calculés pour un rapprochement donné — projection en lecture seule,
 * jamais persistée.
 *
 * <p>Exposée par {@code ITransactionService} comme façade publique du domaine Transaction à
 * destination du domaine Rapprochement (voir {@code architecture.md}, section "Encapsulation des
 * domaines" — un Repository d'un autre domaine ne doit jamais être injecté directement).
 */
public record TransactionAggregatesDto(
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        BigDecimal totalTvaDeductible,
        BigDecimal totalTvaCollectee
) {

    /** Constructeur défensif — remplace tout composant {@code null} par {@link BigDecimal#ZERO}. */
    public TransactionAggregatesDto {
        totalDebit = totalDebit != null ? totalDebit : BigDecimal.ZERO;
        totalCredit = totalCredit != null ? totalCredit : BigDecimal.ZERO;
        totalTvaDeductible = totalTvaDeductible != null ? totalTvaDeductible : BigDecimal.ZERO;
        totalTvaCollectee = totalTvaCollectee != null ? totalTvaCollectee : BigDecimal.ZERO;
    }

    /** Instance canonique à zéro — rapprochement sans aucune transaction. */
    public static final TransactionAggregatesDto EMPTY =
            new TransactionAggregatesDto(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    /** Fabrique à partir d'une ligne agrégat JPQL brute ({@code [debit, credit, tvaDed, tvaColl]}). */
    public static TransactionAggregatesDto fromRow(Object[] row) {
        return new TransactionAggregatesDto(
                (BigDecimal) row[0],
                (BigDecimal) row[1],
                (BigDecimal) row[2],
                (BigDecimal) row[3]
        );
    }
}
