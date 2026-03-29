package staport.finance.gBanque.transactions.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import staport.finance.gBanque.transactions.entities.Transaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    /**
     * Computes the live balance for a bank account:
     *   soldeDepart + SUM(unconfirmed credits) - SUM(unconfirmed debits)
     *
     * COALESCE on each SUM guards against NULL when no confirmed transactions exist yet.
     * Returns empty when the banque itself does not exist.
     */
    @Query("""
            SELECT b.soldeDepart
                   + COALESCE(SUM(COALESCE(t.credit, 0)), 0)
                   - COALESCE(SUM(COALESCE(t.debit,  0)), 0)
            FROM   Transaction t
            JOIN   t.banqueDePrelevement b
            WHERE  b.id          = :banqueId
            AND    t.isConfirmed = false
            """)
    Optional<BigDecimal> computeBalance(@Param("banqueId") Long banqueId);

    /**
     * Counts pending (unconfirmed) transactions for a given bank account.
     * Used as a deletion guard: a banque cannot be deleted while pending
     * transactions exist (they may still be confirmed and affect the balance).
     */
    @Query("""
            SELECT COUNT(t)
            FROM   Transaction t
            WHERE  t.banqueDePrelevement.id = :banqueId
            AND    t.isConfirmed            = false
            """)
    long countPendingByBanque(@Param("banqueId") Long banqueId);

    @Query("""
    SELECT b.id,
           (b.soldeDepart + SUM(CASE WHEN t.isConfirmed = false THEN COALESCE(t.credit, 0) ELSE 0 END)
                          - SUM(CASE WHEN t.isConfirmed = false THEN COALESCE(t.debit, 0) ELSE 0 END))
    FROM Transaction t
    JOIN t.banqueDePrelevement b
    WHERE b.id IN :ids
    GROUP BY b.id, b.soldeDepart
""")
    List<Object[]> findBalancesByBankIds(@Param("ids") List<Long> ids);
}