package staport.finance.gBanque.commision.dtos;

import staport.finance.gBanque.commision.enums.CommissionCondition;
import staport.finance.gBanque.transactions.enums.TransactionSource;

/**
 * Critères de recherche optionnels pour l'écran d'administration des règles de commission.
 * Liés en query params (GET) via {@code @ModelAttribute}.
 */
public record CommissionConfigSearchCriteria(
        Long banqueId,
        TransactionSource type,
        CommissionCondition condition,
        String search
) {}
