package staport.finance.gBanque.recapitulatif.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Point le plus bas de la trésorerie sur la période : premier jour où le solde prévu atteint son
 * minimum, ce solde, et la disponibilité correspondante (solde + plafond de facilité de caisse).
 */
public record PointBasDto(
        LocalDate date,
        BigDecimal solde,
        BigDecimal disponibilite
) {
}
