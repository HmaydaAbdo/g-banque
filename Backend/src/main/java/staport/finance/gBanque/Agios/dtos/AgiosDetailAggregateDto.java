package staport.finance.gBanque.Agios.dtos;

import java.math.BigDecimal;

public record AgiosDetailAggregateDto(

        BigDecimal totalAgiosHT,
        BigDecimal totalTVA,
        BigDecimal totalAgiosTTC,
        int nbJoursDecouvert,
        int nbJoursTotal

) {}
