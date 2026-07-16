package staport.finance.gBanque.transactions.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.transactions.dtos.AgreratedTransactionResponseDto;
import staport.finance.gBanque.transactions.dtos.TransactionAggregatesDto;
import staport.finance.gBanque.transactions.dtos.TransactionResponseDto;
import staport.finance.gBanque.transactions.dtos.TransactionSearchDto;
import staport.finance.gBanque.transactions.dtos.TransactionUpdateRequestDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Façade publique du domaine Transaction.
 *
 * <p>Toute communication inter-domaines avec Transaction passe exclusivement par cette interface
 * (jamais {@code TransactionRepository} injecté directement — voir {@code architecture.md},
 * section "Encapsulation des domaines"). La section dédiée au domaine Rapprochement expose les
 * agrégats et recherches dont {@code RapprochementService}/{@code SoldeCascadeService} ont besoin.
 */
public interface ITransactionService {

    Page<TransactionResponseDto> getTransactionHistory(TransactionSearchDto searchDto, Pageable pageable);

    TransactionResponseDto getTransactionById(Long id);

    AgreratedTransactionResponseDto getTransactionsByBanque(Long banqueId, TransactionSearchDto searchDto, Pageable pageable);

    TransactionResponseDto updateTransactionById(Long id, TransactionUpdateRequestDto updateRequestDto);

    BigDecimal computeBalance(Long banqueId);

    long countPendingTransactions(Long banqueId);

    Map<Long, BigDecimal> computeBalancesForBanks(List<Long> bankIds);

    // ── Façade pour le domaine Rapprochement ────────────────────────────────────────────────

    /** Transactions d'un rapprochement, filtrées et paginées. */
    Page<TransactionResponseDto> searchTransactionsForRapprochement(Long rapprochementId, TransactionSearchDto searchDto, Pageable pageable);

    /** Toutes les transactions d'un rapprochement, triées par date d'opération (usage : export PDF). */
    List<TransactionResponseDto> getAllTransactionsForRapprochementSortedByDate(Long rapprochementId);

    /** Nombre de transactions attachées à un rapprochement. */
    long countByRapprochementId(Long rapprochementId);

    /** Agrégats (débit/crédit/TVA) d'un rapprochement — une seule requête SQL. */
    TransactionAggregatesDto computeAggregatesForRapprochement(Long rapprochementId);

    /** Agrégats batch pour une liste de rapprochements — évite le N+1 sur les listes paginées. */
    Map<Long, TransactionAggregatesDto> computeAggregatesByRapprochementIds(List<Long> rapprochementIds);
}
