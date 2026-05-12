package staport.finance.gBanque.cautions.dtos;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Requête de clôture d'une caution (main levée).
 *
 * <p>La date de main levée est obligatoire — aucune contrainte min/max.
 */
public record CloturerCautionRequest(

    @NotNull(message = "La date de main levée est obligatoire")
    LocalDate dateDeMainLevee

) {}
