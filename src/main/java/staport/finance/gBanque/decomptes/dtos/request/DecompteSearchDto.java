package staport.finance.gBanque.decomptes.dtos.request;

import org.springframework.format.annotation.DateTimeFormat;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Critères de recherche pour la liste des décomptes. Tous les champs sont optionnels.
 */
public record DecompteSearchDto(

        Long marcheId,

        Long banqueId,

        Status status,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateValeurFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateValeurTo,

        BigDecimal montantMin,

        BigDecimal montantMax

) {}
