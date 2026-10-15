package staport.finance.gBanque.recapitulatif.dtos.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ligne d'une catégorie du plan de trésorerie (ex. « Leasing »), rattachée à sa famille (ex.
 * « Financements ») : un montant par colonne, dans l'ordre des colonnes du plan, et leur total.
 */
public record LignePlanDto(
        String famille,
        String categorie,
        List<BigDecimal> montants,
        BigDecimal total
) {
}
