package staport.finance.gBanque.recapitulatif.dtos.response;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ligne d'une catégorie du plan de trésorerie (ex. « Leasing »), rattachée à sa famille (ex.
 * « Financements ») : un montant par colonne, dans l'ordre des colonnes du plan, et leur total.
 *
 * <p>{@code sources} : les sources d'encours qui alimentent la ligne (ex. « REGLEMENT FRNS » = LCN, chèque, virement,
 * mise à disposition), de la plus grosse à la plus petite sur l'horizon. La page de trésorerie d'une banque s'en sert pour
 * déplier une catégorie en sources sans dupliquer la règle de regroupement ({@code CategoriesTresorerie}).</p>
 */
public record LignePlanDto(
        String famille,
        String categorie,
        List<BigDecimal> montants,
        BigDecimal total,
        List<TransactionSource> sources
) {
}
