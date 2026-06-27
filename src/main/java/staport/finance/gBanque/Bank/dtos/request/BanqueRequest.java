package staport.finance.gBanque.Bank.dtos.request;

import jakarta.validation.constraints.*;
import staport.finance.gBanque.Bank.enums.Devise;

public record BanqueRequest(

        @NotBlank(message = "Le nom de la banque est obligatoire.")
        @Pattern(
                regexp = "^[\\p{L}\\s\\-']+$",
                message = "Le nom de la banque ne doit contenir que des lettres (y compris accentuées), des espaces, des tirets ou des apostrophes."
        )
        @Size(min = 2, max = 100, message = "Le nom de la banque doit avoir entre 2 et 100 caractères.")
        String nom,

        @NotBlank(message = "Le numéro de compte est obligatoire.")
        @Size(min = 24, max = 24, message = "Le numéro de compte doit comporter exactement 24 chiffres.")
        @Pattern(
                regexp = "^[0-9]*$",
                message = "Le numéro de compte ne doit contenir que des chiffres."
        )
        String numeroDeCompte,

        @NotNull(message = "La devise est obligatoire.")
        Devise devise,

        @NotBlank(message = "L'adresse est obligatoire.")
        @Size(min = 5, max = 255, message = "L'adresse doit avoir entre 5 et 255 caractères.")
        String adresse,

        @NotBlank(message = "L'agence est obligatoire.")
        @Size(min = 2, max = 100, message = "L'agence doit avoir entre 2 et 100 caractères.")
        String agence

) {}
