package staport.finance.gBanque.prefinancement.dtos.request;

import org.springframework.format.annotation.DateTimeFormat;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

import java.time.LocalDate;

/**
 * Critères de recherche des préfinancements. Tous les champs sont optionnels.
 *
 * <p>Les 3 paires de dates (émission/opération/valeur) permettent une recherche par période
 * indépendante sur chacune — bornes inclusives, combinables entre elles et avec les autres
 * critères.
 */
public record PrefinancementSearchDTO(
        Long marcheId,
        Long banqueId,
        Type type,
        Status status,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateEmissionFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateEmissionTo,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateOperationFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateOperationTo,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateValeurFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateValeurTo
) {
}
