package staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrganismeLeasingRequestDTO(
        @NotBlank(message = "Le nom est obligatoire") String nom,
        @NotNull(message = "Le plafond est obligatoire") BigDecimal plafond,
        @NotNull(message = "La date de début est obligatoire") LocalDate dateDebut,
        @NotNull(message = "La date de fin est obligatoire") LocalDate dateFin
) {
    @AssertTrue(message = "La date de fin doit être postérieure à la date de début")
    public boolean isDateFinApresDateDebut() {
        return dateDebut != null && dateFin != null && dateFin.isAfter(dateDebut);
    }
}
