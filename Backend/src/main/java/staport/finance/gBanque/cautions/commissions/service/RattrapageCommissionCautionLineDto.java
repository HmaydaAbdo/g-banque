package staport.finance.gBanque.cautions.commissions.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RattrapageCommissionCautionLineDto(
        Long cautionId,
        String numeroCaution,
        Long banqueId,
        String banque,
        String typeCommission,
        LocalDate dateEmission,
        String periodeCouverte,
        BigDecimal montant,
        String statut,
        String detail
) {}
