package staport.finance.gBanque.user.dtos.request;

import jakarta.validation.constraints.NotNull;

/**
 * DTO pour l'activation/désactivation d'un compte utilisateur.
 * Remplace le {@code Map<String, Boolean>} non validé précédent.
 */
public record SetEnabledRequest(

        @NotNull(message = "enabled est obligatoire.")
        Boolean enabled

) {}
