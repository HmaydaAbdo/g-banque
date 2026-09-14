package staport.finance.gBanque.recapitulatif.dtos.response;

import java.math.BigDecimal;
import java.util.List;

public record DecaissementSectionDto(
        List<CategorieAmountDto> categories,
        BigDecimal total
) {
}
