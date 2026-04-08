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
    SELECT b.soldeDepart
           + COALESCE(SUM(CASE WHEN r.statut = 'EN_COURS' THEN t.credit ELSE 0 END), 0)
           - COALESCE(SUM(CASE WHEN r.statut = 'EN_COURS' THEN t.debit ELSE 0 END), 0)
    FROM Banque b
    LEFT JOIN Transaction t ON t.banqueDePrelevement.id = b.id
    LEFT JOIN t.rapprochement r
    WHERE b.id = :banqueId
    GROUP BY b.id, b.soldeDepart
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
    SELECT b.id,
           (b.soldeDepart + SUM(CASE WHEN r.statut = 'EN_COURS' THEN COALESCE(t.credit, 0) ELSE 0 END)
                          - SUM(CASE WHEN r.statut = 'EN_COURS' THEN COALESCE(t.debit, 0) ELSE 0 END))
    FROM Banque b
    LEFT JOIN Transaction t ON t.banqueDePrelevement.id = b.id
    LEFT JOIN t.rapprochement r
    WHERE b.id IN :ids
    GROUP BY b.id, b.soldeDepart
""")
    List<Object[]> findBalancesByBankIds(@Param("ids") List<Long> ids);
}