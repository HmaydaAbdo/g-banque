package staport.finance.gBanque.user.dtos.request;

import jakarta.validation.constraints.NotNull;

/**
 * DTO pour la bascule du statut SYSTEM_ADMIN d'un utilisateur.
 * Remplace le {@code Map<String, Boolean>} non validé précédent.
 */
public record SetSystemAdminRequest(

        @NotNull(message = "isSystemAdmin est obligatoire.")
        Boolean isSystemAdmin

) {}
