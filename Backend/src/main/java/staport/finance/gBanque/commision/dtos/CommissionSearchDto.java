package staport.finance.gBanque.commision.dtos;

import org.springframework.format.annotation.DateTimeFormat;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

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
        Long natureId,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEmissionDebut,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEmissionFin,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOperationDebut,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOperationFin,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateValeurDebut,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateValeurFin

) {}
