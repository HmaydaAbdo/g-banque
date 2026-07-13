package staport.finance.gBanque.prefinancement.dtos.request;

import java.math.BigDecimal;

/**
 * Critères de recherche pour la consultation de l'encours de préfinancement (déblocage ou
 * remboursement) d'une banque.
 */
public record EncoursPrefinancementSearchDto(
        String marcheName,
        String marcheNumero,
        BigDecimal montant
) {
}
