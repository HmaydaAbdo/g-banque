package staport.finance.gBanque.recapitulatif.dtos.request;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.util.List;

/**
 * Critères de recherche pour les endpoints de récapitulatif (décaissement/encaissement).
 * Le {@code banqueId} n'est pas porté par ce DTO : il provient du {@code @PathVariable} de
 * l'endpoint et est transmis séparément au Service (record immuable, pas de mutation possible
 * après le binding {@code @ModelAttribute}).
 */
public record DecaissementSearchDto(
        List<TransactionSource> transactionSources,
        String libelle,
        String observation
) {
}
