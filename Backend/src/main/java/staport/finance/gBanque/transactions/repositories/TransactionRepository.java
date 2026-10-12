package staport.finance.gBanque.transactions.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    /** Transactions de rapprochement issues de l'opération source {@code referent} (une seule en situation normale). */
    List<Transaction> findByTransactionSourceInAndReferent(Collection<TransactionSource> sources, Long referent);

    /**
     * Computes the live balance for a bank account:
     *   soldeDepart + SUM(credits from EN_COURS rapprochement) - SUM(debits from EN_COURS rapprochement)
     *
     * Only transactions linked to an EN_COURS rapprochement are considered "pending".
     * COALESCE guards against NULL when no pending transactions exist yet.
     */
    @Query("""
    SELECT COALESCE(r.soldeDebut, 0)
           + COALESCE(SUM(t.credit), 0)
           - COALESCE(SUM(t.debit), 0)
    FROM rapprochement r
    LEFT JOIN r.transactions t
    WHERE r.banque.id = :banqueId AND r.statut = 'EN_COURS'
    GROUP BY r.id, r.soldeDebut
""")
    BigDecimal computeBalance(@Param("banqueId") Long banqueId);

    /**
     * Counts pending transactions (linked to an EN_COURS rapprochement) for a given bank account.
     * Used as a deletion guard: a banque cannot be deleted while pending transactions exist.
     */
    @Query("""
            SELECT COUNT(t)
            FROM   Transaction t
            JOIN   t.rapprochement r
            WHERE  t.banqueDePrelevement.id = :banqueId
            AND    r.statut = 'EN_COURS'
            """)
    long countPendingByBanque(@Param("banqueId") Long banqueId);

    @Query("""
    SELECT r.banque.id,
           COALESCE(r.soldeDebut, 0)
           + COALESCE(SUM(t.credit), 0)
           - COALESCE(SUM(t.debit), 0)
    FROM rapprochement r
    LEFT JOIN r.transactions t
    WHERE r.banque.id IN :ids AND r.statut = 'EN_COURS'
    GROUP BY r.id, r.banque.id, r.soldeDebut
""")
    List<Object[]> findBalancesByBankIds(@Param("ids") List<Long> ids);

    /**
     * Computes all aggregates for a single rapprochement in one SQL round-trip.
     * Returns a List with exactly one Object[] row (COALESCE guarantees a row even with no transactions):
     * [totalDebit, totalCredit, tvaDeductible, tvaCollectee].
     * NOTE: Optional<Object[]> is intentionally avoided — Spring Data JPA wraps multi-column
     * aggregate results in Object[][] when using Optional, causing a ClassCastException at runtime.
     */
    @Query("""
    SELECT COALESCE(SUM(t.debit),          0),
           COALESCE(SUM(t.credit),         0),
           COALESCE(SUM(t.tvaDeductible),  0),
           COALESCE(SUM(t.tvaCollectee),   0)
    FROM Transaction t
    WHERE t.rapprochement.id = :rapprochementId
""")
    List<Object[]> computeAggregatesForRapprochement(@Param("rapprochementId") Long rapprochementId);

    /**
     * Batch version for paginated list views — avoids N+1.
     * Returns one row per rapprochement: [rapprochementId, totalDebit, totalCredit, tvaDeductible, tvaCollectee].
     * Rapprochements with no transactions are absent from the result; callers should default to EMPTY.
     */
    @Query("""
    SELECT t.rapprochement.id,
           COALESCE(SUM(t.debit),          0),
           COALESCE(SUM(t.credit),         0),
           COALESCE(SUM(t.tvaDeductible),  0),
           COALESCE(SUM(t.tvaCollectee),   0)
    FROM Transaction t
    WHERE t.rapprochement.id IN :ids
    GROUP BY t.rapprochement.id
""")
    List<Object[]> computeAggregatesByRapprochementIds(@Param("ids") List<Long> ids);

    /** Count of transactions attached to a specific rapprochement. */
    long countByRapprochementId(Long rapprochementId);
}