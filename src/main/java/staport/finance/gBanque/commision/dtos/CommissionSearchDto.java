package staport.finance.gBanque.commision.dtos;

import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;

/**
 * Critères de recherche pour les écrans de recherche/export des commissions (endpoints
 * {@code /search} et {@code /export/pdf}).
 *
 * <p>{@code natureId} filtre sur la nature de la ligne fonctionnelle de la caution d'origine
 * (commission.caution.nature.id) — sans effet pour les commissions sans caution associée
 * (ex : commissions sur virements).
 */
public record CommissionSearchDto(

        Long banqueId,
        String libelle,
        TransactionSource operation,
        BigDecimal montant,
        Status status,
        Long natureId

) {}
