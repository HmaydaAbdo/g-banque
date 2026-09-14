package staport.finance.gBanque.provision.dtos;

import org.springframework.format.annotation.DateTimeFormat;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.SensOperation;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Critères de recherche pour l'écran de recherche des provisions (endpoint {@code /search}).
 */
public record ProvisionSearchDto(

        Long banqueId,
        String libelle,
        BigDecimal montant,
        Status status,
        TransactionSource motif,
        SensOperation sens,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateEmissionDebut,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateEmissionFin,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateOperationDebut,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateOperationFin,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateValeurDebut,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateValeurFin

) {}
