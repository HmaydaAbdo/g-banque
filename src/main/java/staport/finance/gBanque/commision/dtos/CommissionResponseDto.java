package staport.finance.gBanque.commision.dtos;

import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de réponse pour une commission.
 *
 * <p>{@code cautionId}/{@code cautionMontant}/{@code natureId}/{@code natureLabel} sont nullable —
 * absents pour les commissions saisies manuellement ou générées hors caution (ex : virements).
 */
public record CommissionResponseDto(

        Long id,
        String libelle,
        String observation,
        BigDecimal montant,
        Long banqueId,
        String banqueNom,
        TransactionSource operation,
        LocalDate dateEmission,
        LocalDate dateValeur,
        LocalDate dateOperation,
        Status status,
        Long cautionId,
        BigDecimal cautionMontant,
        Long natureId,
        String natureLabel

) {}
