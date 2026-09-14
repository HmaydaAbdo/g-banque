package staport.finance.gBanque.cmt.config.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Requête de mise à jour de la configuration CMT.
 */
public record CmtConfigRequestDTO(

        @NotNull(message = "Le seuil d'alerte court terme est obligatoire")
        @Min(value = 1, message = "Le seuil d'alerte court terme doit être d'au moins 1 jour")
        Integer alerteEcheanceJoursCourtTerme,

        @NotNull(message = "Le seuil d'alerte long terme est obligatoire")
        @Min(value = 1, message = "Le seuil d'alerte long terme doit être d'au moins 1 jour")
        Integer alerteEcheanceJoursLongTerme

) {}
