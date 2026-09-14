package staport.finance.gBanque.impayees.dtos;

import org.springframework.format.annotation.DateTimeFormat;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Critères de recherche pour l'écran de recherche des impayées (endpoint {@code /search}).
 */
public record ImpayeeSearchDto(

        Long banqueId,
        String libelle,
        TransactionSource motif,
        String numero,
        Type type,
        BigDecimal montant,
        Status status,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEmissionDebut,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEmissionFin,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOperationDebut,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOperationFin,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateValeurDebut,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateValeurFin

) {}
