package staport.finance.gBanque.user.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de création d'un utilisateur.
 *
 * <p>{@code enabled} est nullable : si omis, le service applique {@code true} par défaut.
 */
public record CreateUserRequest(

        @NotBlank(message = "Le nom d'utilisateur est obligatoire.")
        @Size(min = 3, max = 50, message = "Le nom d'utilisateur doit avoir entre 3 et 50 caractères.")
        String username,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(min = 8, message = "Le mot de passe doit comporter au moins 8 caractères.")
        String password,

        Boolean enabled

) {}
