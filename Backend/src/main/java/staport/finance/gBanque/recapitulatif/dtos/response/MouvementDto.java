package staport.finance.gBanque.recapitulatif.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Un des plus gros mouvements à venir d'une banque : somme, pour un jour, des encours d'une même
 * catégorie dans un même sens. {@code sens} vaut {@code ENCAISSEMENT} ou {@code DECAISSEMENT} ;
 * {@code montant} est toujours positif.
 */
public record MouvementDto(
        LocalDate date,
        String categorie,
        String famille,
        String sens,
        BigDecimal montant
) {
}
