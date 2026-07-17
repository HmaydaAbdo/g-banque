package staport.finance.gBanque.validation.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TVAUtils {

    private static final Logger log = LoggerFactory.getLogger(TVAUtils.class);

    private TVAUtils() {}

    /**
     * Calcule la TVA à partir du montant TTC et du taux de TVA.
     *
     * @param montantTTC le montant TTC
     * @param tauxTVA le taux de TVA en pourcentage (ex : 20 pour 20%)
     * @return la TVA calculée avec 2 décimales
     */
    public static BigDecimal calculerTVA(BigDecimal montantTTC, BigDecimal tauxTVA) {
        if (montantTTC == null || tauxTVA == null) {
            throw new IllegalArgumentException("Le montant et le taux de TVA ne peuvent pas être null");
        }

        log.debug("Calcul TVA : taux={}", tauxTVA);

        // Calcul HT avec précision élevée
        BigDecimal ht = montantTTC.divide(
                BigDecimal.ONE.add(tauxTVA.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)),
                10,
                RoundingMode.HALF_UP
        );

        // Calcul TVA = TTC - HT
        return montantTTC.subtract(ht).setScale(2, RoundingMode.HALF_UP);
    }
}
