package staport.finance.gBanque.transactions.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.transactions.dtos.AgreratedTransactionResponseDto;
import staport.finance.gBanque.transactions.dtos.TransactionAggregatesDto;
import staport.finance.gBanque.transactions.dtos.TransactionResponseDto;
import staport.finance.gBanque.transactions.dtos.TransactionSearchDto;
import staport.finance.gBanque.transactions.dtos.TransactionUpdateRequestDto;

import java.math.BigDecimal;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import java.util.function.UnaryOperator;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Façade publique du domaine Transaction.
 *
 * <p>Toute communication inter-domaines avec Transaction passe exclusivement par cette interface
 * (jamais {@code TransactionRepository} injecté directement — voir {@code architecture.md},
 * section "Encapsulation des domaines"). La section dédiée au domaine Rapprochement expose les
 * agrégats et recherches dont {@code RapprochementServiceImpl}/{@code SoldeCascadeService} ont besoin.
 */
public interface ITransactionService {

    Page<TransactionResponseDto> getTransactionHistory(TransactionSearchDto searchDto, Pageable pageable);

    /**
     * Exporte en PDF l'intégralité de l'historique des transactions correspondant aux critères
     * fournis (mêmes filtres que {@link #getTransactionHistory}, sans pagination).
     */
    byte[] exportTransactionHistoryPdf(TransactionSearchDto searchDto);

    TransactionResponseDto getTransactionById(Long id);

    AgreratedTransactionResponseDto getTransactionsByBanque(Long banqueId, TransactionSearchDto searchDto, Pageable pageable);

    /**
     * Exporte en PDF les transactions d'une banque (mêmes critères que
     * {@link #getTransactionsByBanque}, sans pagination) — écran "Rapprochement en cours".
     */
    byte[] exportBankTransactionsPdf(Long banqueId, TransactionSearchDto searchDto);

    TransactionResponseDto updateTransactionById(Long id, TransactionUpdateRequestDto updateRequestDto);

    /**
     * Réécrit le libellé et l'observation des transactions d'une opération source (ex. mention d'avance de leasing sur
     * la transaction d'un chèque réglé), puis propage les textes à l'entité source comme le fait
     * {@link #updateTransactionById}. Sans transaction pour cette source, ne fait rien.
     *
     * @param libelle     fonction appliquée au libellé actuel (renvoie le texte tel quel pour ne rien changer)
     * @param observation idem pour l'observation
     */
    void modifierTextes(Collection<TransactionSource> sources, Long referent,
                        UnaryOperator<String> libelle, UnaryOperator<String> observation);

    BigDecimal computeBalance(Long banqueId);

    long countPendingTransactions(Long banqueId);

    Map<Long, BigDecimal> computeBalancesForBanks(List<Long> bankIds);

    // ── Façade pour le domaine Rapprochement ────────────────────────────────────────────────

    /**
     * Transactions d'un rapprochement, filtrées et paginées — {@code pageable} peut être
     * {@link Pageable#unpaged()} pour récupérer l'intégralité des résultats filtrés en une page
     * (usage : export PDF filtré du rapport de rapprochement, voir {@code RapprochementServiceImpl}).
     */
    Page<TransactionResponseDto> searchTransactionsForRapprochement(Long rapprochementId, TransactionSearchDto searchDto, Pageable pageable);

    /** Nombre de transactions attachées à un rapprochement. */
    long countByRapprochementId(Long rapprochementId);

    /** Agrégats (débit/crédit/TVA) d'un rapprochement — une seule requête SQL. */
    TransactionAggregatesDto computeAggregatesForRapprochement(Long rapprochementId);

    /** Agrégats batch pour une liste de rapprochements — évite le N+1 sur les listes paginées. */
    Map<Long, TransactionAggregatesDto> computeAggregatesByRapprochementIds(List<Long> rapprochementIds);
}
