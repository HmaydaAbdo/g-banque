package staport.finance.gBanque.recapitulatif.dtos.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Récapitulatif global d'une banque : informations banque, encaissements/décaissements
 * du mois, et totaux/soldes dérivés — calculés à la demande via des méthodes plutôt que
 * stockés (record immuable, pas de recalcul-sur-mutation).
 */
public record RecaputilatifGlobal(
        Long banqueId,
        String banqueName,
        BigDecimal soldeActuel,
        BigDecimal plafondFC,
        List<RecaputilatifItem> encaissement,
        List<RecaputilatifItem> decaissement
) {
    public RecaputilatifGlobal {
        soldeActuel = soldeActuel != null ? soldeActuel : BigDecimal.ZERO;
        plafondFC = plafondFC != null ? plafondFC : BigDecimal.ZERO;
    }

    /**
     * Total des encaissements (somme des crédits).
     */
    public BigDecimal totalEncaissements() {
        return sum(encaissement, RecaputilatifItem::getCredit);
    }

    /**
     * Total des décaissements (somme des débits).
     */
    public BigDecimal totalDecaissements() {
        return sum(decaissement, RecaputilatifItem::getDebit);
    }

    /**
     * Solde fin de mois = solde actuel + total encaissements - total décaissements.
     */
    public BigDecimal soldefin() {
        return soldeActuel.add(totalEncaissements()).subtract(totalDecaissements());
    }

    /**
     * Reste disponible = plafond FC - solde fin de mois.
     */
    public BigDecimal reste() {
        return plafondFC.subtract(soldefin());
    }

    private static BigDecimal sum(List<RecaputilatifItem> items, Function<RecaputilatifItem, BigDecimal> extractor) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
