package staport.finance.gBanque.recapitulatif.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Solde prévu en fin de journée — un point de la courbe du plan de trésorerie. */
public record SoldeJourDto(
        LocalDate date,
        BigDecimal solde
) {
}
