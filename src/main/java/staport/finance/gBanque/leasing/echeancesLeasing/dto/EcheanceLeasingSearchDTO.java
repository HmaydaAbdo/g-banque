package staport.finance.gBanque.leasing.echeancesLeasing.dto;

import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;

/**
 * DTO utilisé pour les critères de recherche avancée des échéances.
 * Peut être utilisé pour filtrer le tableau d'amortissement ou pour une recherche globale.
 */
public record EcheanceLeasingSearchDTO(
        Long contratLeasingId,
        Integer numeroEcheance,
        Integer month,
        Integer year,
        StatutEcheanceLeasing statutPaiement
) {}
