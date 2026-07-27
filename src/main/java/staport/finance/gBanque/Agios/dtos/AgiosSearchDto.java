package staport.finance.gBanque.Agios.dtos;

import org.springframework.format.annotation.DateTimeFormat;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Critères de recherche pour la liste paginée des agios.
 */
public record AgiosSearchDto(

        Long banqueId,
        String libelle,
        BigDecimal montant,
        Status status,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEmissionDebut,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEmissionFin,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOperationDebut,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOperationFin,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateValeurDebut,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateValeurFin

) {}
