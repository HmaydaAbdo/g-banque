package staport.finance.gBanque.factoring.dtos.request;

import java.math.BigDecimal;

/**
 * Critères de recherche pour la consultation de l'encours de factoring (déblocage ou
 * remboursement) d'une banque.
 */
public record EncoursFactoringSearchDto(
        String marcheName,
        String marcheNumero,
        BigDecimal montant
) {
}
