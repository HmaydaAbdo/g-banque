package staport.finance.gBanque.recapitulatif.dtos.response;

import java.math.BigDecimal;

public record ResumeSectionDto(
        BigDecimal soldeActuel,
        BigDecimal plafondLigne,
        BigDecimal disponibilite,
        BigDecimal encoursPlus,
        BigDecimal encoursMoins,
        BigDecimal soldeProjete
) {
}
