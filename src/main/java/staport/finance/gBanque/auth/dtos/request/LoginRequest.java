package staport.finance.gBanque.auth.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "Le nom d'utilisateur est obligatoire.")
        @Size(min = 3, max = 50, message = "Le nom d'utilisateur doit comporter entre 3 et 50 caractères.")
        String username,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(min = 5, message = "Le mot de passe doit comporter au moins 5 caractères.")
        String password
) {
}
