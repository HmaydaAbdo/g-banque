package staport.finance.gBanque.restitution.dtos;

import org.springframework.format.annotation.DateTimeFormat;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Critères de recherche pour l'écran de recherche des restitutions (endpoint {@code /search}).
 */
public record RestitutionSearchDto(

        Long banqueId,
        String libelle,
        BigDecimal montant,
        Status status,

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
