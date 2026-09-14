package staport.finance.gBanque.LigneFonctionnelle.config.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Requête de mise à jour de la configuration LigneFonctionnelle.
 */
public record LigneFonctionnelleConfigRequestDTO(

        @NotNull(message = "Le seuil d'alerte est obligatoire")
        @Min(value = 1, message = "Le seuil d'alerte doit être d'au moins 1 mois")
        Integer alertThresholdMonths,

        @NotNull(message = "Le seuil d'avertissement est obligatoire")
        @Min(value = 1, message = "Le seuil d'avertissement doit être d'au moins 1 mois")
        Integer warningThresholdMonths

) {}
