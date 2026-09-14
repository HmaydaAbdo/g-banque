package staport.finance.gBanque.chargesFixes.config.dtos.request;

import staport.finance.gBanque.chargesFixes.TypeChargeFixe;

/**
 * Critères de recherche optionnels pour l'écran d'administration des charges fixes.
 * Liés en query params (GET) via {@code @ModelAttribute}, comme {@code EncoursFraisBancaireSearchDto}.
 */
public record ChargeFixeConfigSearchCriteria(
        Long banqueId,
        TypeChargeFixe type,
        String search
) {}
