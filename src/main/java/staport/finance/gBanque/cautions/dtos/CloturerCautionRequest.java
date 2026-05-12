package staport.finance.gBanque.cautions.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

/**
 * Requête de clôture d'une caution (main levée).
 *
 * <p>La date de main levée est obligatoire et ne peut pas être dans le futur.
 */
public record CloturerCautionRequest(

    @NotNull(message = "La date de main levée est obligatoire")
    @PastOrPresent(message = "La date de main levée ne peut pas être dans le futur")
    LocalDate dateDeMainLevee

) {}
