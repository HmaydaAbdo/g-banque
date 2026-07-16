package staport.finance.gBanque.chargesFixes.config.dtos.response;

import staport.finance.gBanque.chargesFixes.TypeChargeFixe;
import staport.finance.gBanque.impot_taxes.enums.Motif;

import java.math.BigDecimal;

public record ChargeFixeConfigResponseDTO(
        Long id,
        Long banqueId,
        String banqueNom,
        String libelle,
        BigDecimal montant,
        TypeChargeFixe type,
        Motif motif,
        int jourEmission,
        boolean appendPeriode,
        boolean moisPrecedent
) {}
