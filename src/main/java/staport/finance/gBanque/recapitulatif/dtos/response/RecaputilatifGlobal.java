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
        return sum(encaissement, RecaputilatifItem::credit);
    }

    /**
     * Total des décaissements (somme des débits).
     */
    public BigDecimal totalDecaissements() {
        return sum(decaissement, RecaputilatifItem::debit);
    }

    /**
     * Solde fin de mois = solde actuel + total encaissements - total décaissements.
     */
    public BigDecimal soldefin() {
        return soldeActuel.add(totalEncaissements()).subtract(totalDecaissements());
    }

    /**
     * Disponibilité sur la facilité de caisse projetée en fin de mois = solde fin de mois +
     * plafond FC (plafond en positif, comme stocké en base). Positif = marge restante avant
     * d'atteindre le plafond autorisé ; négatif = dépassement projeté du plafond.
     *
     * <p>Même convention de signe que {@code ResumeSectionDto.disponibilite()}
     * (RecapGeneralDetailleServiceImpl : {@code soldeActuel + plafondLigne}), qui calcule la
     * même chose sur le solde du jour plutôt que sur le solde projeté en fin de mois — les deux
     * rapports (Récapitulatif global et Récapitulatif général détaillé) reflètent ainsi la même
     * lecture de la disponibilité (retour utilisateur du 31/08/2026 : l'ancienne méthode
     * {@code reste()} négativait le plafond avant de le comparer au solde, ce qui inversait le
     * signe de cette marge par rapport à cette convention).</p>
     */
    public BigDecimal disponibiliteFinDuMois() {
        return soldefin().add(plafondFC);
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
