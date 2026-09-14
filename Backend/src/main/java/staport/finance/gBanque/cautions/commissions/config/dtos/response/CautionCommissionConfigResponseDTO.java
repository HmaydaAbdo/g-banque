package staport.finance.gBanque.cautions.commissions.config.dtos.response;

import java.math.BigDecimal;

public record CautionCommissionConfigResponseDTO(

    Long id,
    Long banqueId,
    String banqueNom,
    BigDecimal montantMinimum,
    BigDecimal diviseur

) {}
