package staport.finance.gBanque.cautions.commissions.factory;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.cautions.commissions.strategy.CommissionCautionStrategie;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Factory de résolution des stratégies de commission et tva par caution.
 *
 * <p>Spring injecte automatiquement toutes les implémentations de
 * {@link CommissionCautionStrategie} déclarées comme {@code @Component}.
 * La factory construit une {@code Map} immuable indexée par nom de banque
 * (insensible à la casse) au démarrage de l'application.
 *
 * <p>Pour ajouter une nouvelle banque, il suffit de créer une nouvelle classe
 * annotée {@code @Component} qui implémente {@link CommissionCautionStrategie} —
 * aucune modification de cette factory n'est nécessaire (OCP).
 *
 * <p>Exemple d'ajout futur :
 * <pre>{@code
 * @Component
 * public class CihCommissionCautionStrategie implements CommissionCautionStrategie {
 *     public String nomBanque() { return "CIH"; }
 *     // ...
 * }
 * }</pre>
 */
@Component
public class CommissionCautionStrategieFactory {

    private final Map<String, CommissionCautionStrategie> strategies;

    /**
     * Construit la factory en indexant chaque stratégie par le nom de banque en majuscules.
     *
     * @param strategies liste de toutes les implémentations détectées par Spring
     * @throws IllegalStateException si deux stratégies déclarent le même nom de banque
     */
    public CommissionCautionStrategieFactory(List<CommissionCautionStrategie> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(
                        s -> s.nomBanque().toUpperCase(Locale.ROOT),
                        s -> s,
                        (s1, s2) -> {
                            throw new IllegalStateException(
                                    "Deux stratégies de commission caution déclarent la même banque : "
                                    + s1.nomBanque());
                        }
                ));
    }

    /**
     * Résout la stratégie applicable pour une banque donnée.
     *
     * @param nomBanque nom de la banque (insensible à la casse)
     * @return {@link Optional} contenant la stratégie, ou vide si aucune n'est enregistrée
     */
    public Optional<CommissionCautionStrategie> trouver(String nomBanque) {
        return Optional.ofNullable(strategies.get(nomBanque.toUpperCase(Locale.ROOT)));
    }
}
