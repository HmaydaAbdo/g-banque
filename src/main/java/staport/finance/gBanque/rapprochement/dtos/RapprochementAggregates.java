package staport.finance.gBanque.rapprochement.dtos;

import java.math.BigDecimal;

/**
 * Immutable value object carrying the four transaction-level aggregates
 * for a single Rapprochement. Computed on-the-fly via a single JPQL
 * aggregate query — never stored on the entity.
 *
 * <p>Business logic lives here, not in the service or mapper:</p>
 * <ul>
 *   <li>{@link #soldeFin(BigDecimal)} derives the closing balance.</li>
 *   <li>{@link #tvaDue()} derives the net VAT position.</li>
 * </ul>
 */
public record RapprochementAggregates(
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        BigDecimal tvaDeductible,
        BigDecimal tvaCollectee
) {

    /** Canonical zero-value instance — use when a rapprochement has no transactions yet. */
    public static final RapprochementAggregates EMPTY = new RapprochementAggregates(
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
    );

    /** Defensive constructor — replaces any null component with ZERO. */
    public RapprochementAggregates {
        totalDebit     = totalDebit     != null ? totalDebit     : BigDecimal.ZERO;
        totalCredit    = totalCredit    != null ? totalCredit    : BigDecimal.ZERO;
        tvaDeductible  = tvaDeductible  != null ? tvaDeductible  : BigDecimal.ZERO;
        tvaCollectee   = tvaCollectee   != null ? tvaCollectee   : BigDecimal.ZERO;
    }

    /**
     * Derives soldeFin from the opening balance.
     * soldeFin = soldeDebut + totalCredit - totalDebit
     */
    public BigDecimal soldeFin(BigDecimal soldeDebut) {
        BigDecimal base = soldeDebut != null ? soldeDebut : BigDecimal.ZERO;
        return base.add(totalCredit).subtract(totalDebit);
    }

    /**
     * Derives the net VAT due.
     * tvaDue = tvaCollectee - tvaDeductible
     */
    public BigDecimal tvaDue() {
        return tvaCollectee.subtract(tvaDeductible);
    }

    /** Convenience factory from a raw JPQL Object[] row. */
    public static RapprochementAggregates fromRow(Object[] row) {
        return new RapprochementAggregates(
                (BigDecimal) row[0],
                (BigDecimal) row[1],
                (BigDecimal) row[2],
                (BigDecimal) row[3]
        );
    }
}
