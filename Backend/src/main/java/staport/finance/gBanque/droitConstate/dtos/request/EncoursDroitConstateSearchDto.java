package staport.finance.gBanque.droitConstate.dtos.request;

import java.math.BigDecimal;

/**
 * Critères de recherche pour la consultation de l'encours de droits constatés (déblocage ou
 * remboursement) d'une banque.
 */
public record EncoursDroitConstateSearchDto(
        String marcheName,
        String marcheNumero,
        BigDecimal montant
) {
}
