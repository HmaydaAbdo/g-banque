package staport.finance.gBanque.impot_taxes.dtos.request;

import org.springframework.format.annotation.DateTimeFormat;
import staport.finance.gBanque.impot_taxes.enums.Motif;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Critères de recherche pour la liste générale des impôts/taxes. Tous les champs sont optionnels.
 */
public record ImpotEtTaxeSearchDto(

        Long banqueId,

        String libelle,

        Motif motif,

        BigDecimal montant,

        Status status,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEmissionDebut,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEmissionFin,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOperationDebut,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOperationFin,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateValeurDebut,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateValeurFin

) {}
