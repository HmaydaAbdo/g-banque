package staport.finance.gBanque.LigneFonctionnelle.generalLignes.dtos;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Requête de création/mise à jour d'une sous-ligne, dans le cadre de la gestion des sous-lignes
 * d'une ligne mère (ex. Caution Administrative).
 *
 * @param id                            null lors d'une création (cascade ou {@code manageSousLignes}),
 *                                      renseigné lors d'une mise à jour d'une sous-ligne existante.
 * @param plafond                       facultatif pour une sous-ligne.
 * @param natureLigneFonctionnelleId    obligatoire, peut différer de la nature de la ligne mère.
 */
public record SousLigneRequestDto(
        Long id,

        @DecimalMin(value = "0.0", inclusive = false, message = "Le plafond doit être supérieur à zéro.")
        BigDecimal plafond,

        @NotNull(message = "L'ID de la nature de ligne fonctionnelle est obligatoire pour la sous-ligne.")
        Long natureLigneFonctionnelleId
) {
}
