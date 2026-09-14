package staport.finance.gBanque.chargesFixes.config.dtos.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import staport.finance.gBanque.chargesFixes.TypeChargeFixe;
import staport.finance.gBanque.impot_taxes.enums.Motif;

import java.math.BigDecimal;

/**
 * @param motif Obligatoire si {@code type == IMPOT_TAXE}, doit être null sinon (validé en service).
 * @param jourEmission {@code > 0} jour fixe du mois ; {@code -1} dernier jour du mois ;
 *                     {@code -2} dernier jour du trimestre (validé en service).
 * @param moisPrecedent Ne peut être {@code true} que si {@code appendPeriode} est {@code true}
 *                      (validé en service).
 */
public record ChargeFixeConfigRequestDTO(
        @NotNull Long banqueId,
        @NotBlank String libelle,
        @NotNull @DecimalMin(value = "0.01") BigDecimal montant,
        @NotNull TypeChargeFixe type,
        Motif motif,
        int jourEmission,
        boolean appendPeriode,
        boolean moisPrecedent
) {}
