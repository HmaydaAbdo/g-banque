package staport.finance.gBanque.commision.dtos;

import staport.finance.gBanque.commision.enums.CommissionCondition;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;

public record CommissionConfigResponseDTO(
        Long id,
        Long banqueId,
        String banqueNom,
        String organismeNom,
        BigDecimal montant,
        TransactionSource type,
        CommissionCondition condition
) {}
