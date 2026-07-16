package staport.finance.gBanque.transactions.dtos;

import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Représente une transaction telle qu'exposée par l'API.
 *
 * <p>{@code referent} est {@code null} pour toute transaction qui n'est pas synchronisée avec une
 * entité source (voir {@code architecture.md}, Règle 2) — ne jamais le typer en primitif ({@code long})
 * sous peine de {@code NullPointerException} au moment du mapping.
 */
public record TransactionResponseDto(
        Long id,
        LocalDate dateEmission,
        LocalDate dateValeur,
        LocalDate dateOperations,
        String observation,
        BigDecimal tvaDeductible,
        BigDecimal tvaCollectee,
        String libelle,
        TransactionSource transactionSource,
        BigDecimal debit,
        BigDecimal credit,
        Long banqueDePrelevementId,
        String banqueDePrelevementNom,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        StatutRapprochement statutRapprochement,
        Long referent
) {
}
