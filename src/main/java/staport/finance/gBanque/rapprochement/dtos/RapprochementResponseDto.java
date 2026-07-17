package staport.finance.gBanque.rapprochement.dtos;

import org.springframework.data.domain.Page;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.transactions.dtos.TransactionResponseDto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;

public record RapprochementResponseDto(
        Long id,
        Long banqueId,
        String banqueNom,
        YearMonth dateRapprochement,
        BigDecimal soldeDebut,
        BigDecimal soldeFin,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        BigDecimal tvaDeductible,
        BigDecimal tvaCollectee,
        BigDecimal tvaDue,
        StatutRapprochement statutRapprochement,
        Page<TransactionResponseDto> transactions,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /**
     * Retourne une copie avec les transactions paginées attachées — le mapper ne les connaît pas
     * à la construction (recherche/pagination des transactions déléguée à
     * {@code ITransactionService} après le mapping de base).
     */
    public RapprochementResponseDto withTransactions(Page<TransactionResponseDto> transactions) {
        return new RapprochementResponseDto(id, banqueId, banqueNom, dateRapprochement, soldeDebut, soldeFin,
                totalDebit, totalCredit, tvaDeductible, tvaCollectee, tvaDue, statutRapprochement,
                transactions, createdAt, updatedAt);
    }
}
