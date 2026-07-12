package staport.finance.gBanque.cautions.commissions.config.dtos.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO de création/modification des paramètres de commission caution d'une banque.
 */
public record CautionCommissionConfigRequestDTO(

    @NotNull(message = "L'identifiant de la banque est obligatoire")
    Long banqueId,

    @NotNull(message = "Le montant minimum est obligatoire")
    @DecimalMin(value = "0.00", message = "Le montant minimum ne peut pas être négatif")
    BigDecimal montantMinimum,

    @NotNull(message = "Le diviseur est obligatoire")
    @DecimalMin(value = "0.01", message = "Le diviseur doit être strictement positif")
    BigDecimal diviseur

) {}
