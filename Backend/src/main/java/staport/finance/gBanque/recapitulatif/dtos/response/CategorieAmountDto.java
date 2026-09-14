package staport.finance.gBanque.recapitulatif.dtos.response;

import java.math.BigDecimal;

/**
 * Montant agrégé pour une catégorie du récapitulatif général détaillé (ex. "Leasing", "Agios").
 */
public record CategorieAmountDto(
        String categorie,
        BigDecimal montant
) {
}
