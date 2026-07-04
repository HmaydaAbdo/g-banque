package staport.finance.gBanque.leasing.config.dtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Requête de mise à jour de la configuration leasing.
 * Les deux taux sont exprimés en fraction (ex. 0.20 = 20%).
 */
public record LeasingConfigRequestDTO(

        @NotNull(message = "La valeur résiduelle est obligatoire")
        @DecimalMin(value = "0.0", message = "La valeur résiduelle ne peut pas être négative")
        @DecimalMax(value = "1.0", message = "La valeur résiduelle doit être une fraction entre 0 et 1")
        BigDecimal valeurResiduelle,

        @NotNull(message = "Le taux de TVA est obligatoire")
        @DecimalMin(value = "0.0", message = "Le taux de TVA ne peut pas être négatif")
        @DecimalMax(value = "1.0", message = "Le taux de TVA doit être une fraction entre 0 et 1")
        BigDecimal tauxTva

) {}
