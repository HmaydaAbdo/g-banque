package staport.finance.gBanque.marches.dtos.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MarcheRequestDTO(

        @NotNull(message = "Le marche doit etre rattache a une banque")
        Long banqueId,

        @NotBlank(message = "Le numero du marche ne peut pas etre vide")
        @Size(max = 50, message = "Le numero doit avoir au maximum 50 caracteres")
        String numero,

        @NotBlank(message = "Le nom du marche ne peut pas etre vide")
        @Size(max = 255, message = "Le nom doit avoir au maximum 255 caracteres")
        String nom,

        @NotNull(message = "Le montant ne peut pas etre nul")
        @DecimalMin(value = "0.01", message = "Le montant doit etre superieur a zero")
        BigDecimal montant,

        @NotBlank(message = "Le maitre d'ouvrage ne peut pas etre vide")
        @Size(max = 255, message = "Le maitre d'ouvrage doit avoir au maximum 255 caracteres")
        String maitreDouvrage,

        LocalDate dateDebutDesTravaux,

        LocalDate dateDAchievementDesTravaux
) {
}
