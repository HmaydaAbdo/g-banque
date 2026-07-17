package staport.finance.gBanque.rapprochement.dtos;

import staport.finance.gBanque.transactions.dtos.TransactionResponseDto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;

public record ReportDto(
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
        List<TransactionResponseDto> transactions,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /** Retourne une copie avec la liste complète des transactions attachée (export PDF). */
    public ReportDto withTransactions(List<TransactionResponseDto> transactions) {
        return new ReportDto(id, banqueId, banqueNom, dateRapprochement, soldeDebut, soldeFin,
                totalDebit, totalCredit, tvaDeductible, tvaCollectee, tvaDue, transactions, createdAt, updatedAt);
    }
}
