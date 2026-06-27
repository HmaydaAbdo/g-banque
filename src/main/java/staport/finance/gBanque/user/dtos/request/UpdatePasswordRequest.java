package staport.finance.gBanque.user.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO pour le changement de mot de passe d'un utilisateur.
 * Remplace le {@code Map<String, String>} non validé précédent.
 */
public record UpdatePasswordRequest(

        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(min = 8, message = "Le mot de passe doit comporter au moins 8 caractères.")
        String password

) {}
