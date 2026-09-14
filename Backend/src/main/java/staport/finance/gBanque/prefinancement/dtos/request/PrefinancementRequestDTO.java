package staport.finance.gBanque.prefinancement.dtos.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import staport.finance.gBanque.prefinancement.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Données de création/modification d'un préfinancement.
 */
public record PrefinancementRequestDTO(
        @NotNull(message = "Marche ID cannot be null") Long marcheId,
        @NotNull(message = "Date of emission cannot be null") LocalDate dateEmission,
        @NotNull(message = "Montant cannot be null")
        @DecimalMin(value = "0.01", message = "Montant must be a positive value") BigDecimal montant,
        @NotNull(message = "Type cannot be null") Type type
) {
}
