package staport.finance.gBanque.cautions.commissions.config.dtos.request;

/**
 * Critères de recherche optionnels pour l'écran d'administration des configurations de
 * commission caution. Liés en query params (GET) via {@code @ModelAttribute}.
 */
public record CautionCommissionConfigSearchCriteria(
        Long banqueId,
        String search
) {}
