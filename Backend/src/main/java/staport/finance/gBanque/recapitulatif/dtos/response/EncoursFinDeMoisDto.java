package staport.finance.gBanque.recapitulatif.dtos.response;

import java.math.BigDecimal;

public record EncoursFinDeMoisDto(BigDecimal encoursPlus, BigDecimal encoursMoins) {

    public static final EncoursFinDeMoisDto ZERO = new EncoursFinDeMoisDto(BigDecimal.ZERO, BigDecimal.ZERO);

    public EncoursFinDeMoisDto {
        encoursPlus  = encoursPlus  != null ? encoursPlus  : BigDecimal.ZERO;
        encoursMoins = encoursMoins != null ? encoursMoins : BigDecimal.ZERO;
    }
}