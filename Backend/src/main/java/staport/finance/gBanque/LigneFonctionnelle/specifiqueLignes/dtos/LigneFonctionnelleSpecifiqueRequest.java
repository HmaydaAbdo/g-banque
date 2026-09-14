package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Requête de création/mise à jour d'une ligne fonctionnelle spécifique (rattachée à un marché).
 *
 * @param actif    facultatif ; {@code null} est traité comme {@code true} à la création, ou comme
 *                 "conserver la valeur existante" à la mise à jour.
 */
public record LigneFonctionnelleSpecifiqueRequest(
        @NotNull(message = "L'ID de la nature de ligne fonctionnelle est obligatoire.")
        Long natureLigneFonctionnelleId,

        @NotNull(message = "Le plafond est obligatoire.")
        @DecimalMin(value = "0.0", inclusive = false, message = "Le plafond doit être supérieur à zéro.")
        BigDecimal plafond,

        @NotNull(message = "Le taux est obligatoire.")
        @DecimalMin(value = "0.0", message = "Le taux ne peut pas être négatif.")
        @DecimalMax(value = "100.0", message = "Le taux ne peut pas dépasser 100.")
        BigDecimal taux,

        @NotNull(message = "La date de départ est obligatoire.")
        LocalDate dateDepart,

        @NotNull(message = "La date de fin est obligatoire.")
        LocalDate dateFin,

        Boolean actif,

        @NotNull(message = "L'ID du marché est obligatoire.")
        Long marcheId
) {
}
