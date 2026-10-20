package staport.finance.gBanque.moyensDePaiements.lecture;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;

/**
 * Recherche d'un moyen de paiement par numéro ou par montant, commune aux lecteurs chèque, LCN et virement.
 * Le texte saisi est cherché dans le numéro et dans le montant (« 10680 », « 10 680,00 » ou « 1793.62 » conviennent) ;
 * le montant est comparé sous sa forme « 10680.00 » (fonction {@code to_char} de PostgreSQL).
 */
public final class RechercheMoyenPaiement {

    private RechercheMoyenPaiement() {
    }

    /** Prédicat « numéro contient le texte, ou montant contient le texte » ; {@code null} si le texte est vide. */
    public static Predicate parNumeroOuMontant(CriteriaBuilder cb, Path<String> numero, Path<BigDecimal> montant, String recherche) {
        if (recherche == null || recherche.isBlank()) {
            return null;
        }
        String motifNumero = "%" + recherche.trim().toLowerCase() + "%";
        // Espaces (dont insécables) retirés, virgule décimale ramenée au point : « 10 680,5 » devient « 10680.5 »
        String motifMontant = "%" + recherche.replaceAll("[\\s\\u00A0\\u202F]", "").replace(',', '.') + "%";
        return cb.or(
                cb.like(cb.lower(numero), motifNumero),
                cb.like(cb.function("to_char", String.class, montant, cb.literal("FM99999999999999990.00")), motifMontant));
    }
}
