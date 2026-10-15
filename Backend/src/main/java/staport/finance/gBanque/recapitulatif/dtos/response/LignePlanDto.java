package staport.finance.gBanque.recapitulatif.dtos.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ligne d'une catégorie du plan de trésorerie (ex. « Leasing »), rattachée à sa famille (ex.
 * « Financements ») : un montant par colonne, dans l'ordre des colonnes du plan, et leur total.
 * {@code certain} distingue les montants contractuels ou déjà émis des estimations.
 */
public record LignePlanDto(
        String famille,
        String categorie,
        boolean certain,
        List<BigDecimal> montants,
        BigDecimal total
) {
}
