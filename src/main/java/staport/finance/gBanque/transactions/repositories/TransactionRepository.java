package staport.finance.gBanque.transactions.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import staport.finance.gBanque.transactions.entities.Transaction;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

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
    GROUP BY r.banque.id, r.soldeDebut
""")
    List<Object[]> findBalancesByBankIds(@Param("ids") List<Long> ids);
}