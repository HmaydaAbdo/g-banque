package staport.finance.gBanque.recapitulatif.dtos.response;

import java.math.BigDecimal;

/**
 * Encours non encore validés d'une banque jusqu'à un horizon donné (fin de mois ou fin de
 * trimestre, voir {@code PeriodeRecap}) : total des encaissements attendus et total des
 * décaissements attendus.
 */
public record EncoursPrevusDto(BigDecimal encoursPlus, BigDecimal encoursMoins) {

    public static final EncoursPrevusDto ZERO = new EncoursPrevusDto(BigDecimal.ZERO, BigDecimal.ZERO);

    public EncoursPrevusDto {
        encoursPlus  = encoursPlus  != null ? encoursPlus  : BigDecimal.ZERO;
        encoursMoins = encoursMoins != null ? encoursMoins : BigDecimal.ZERO;
    }
}
