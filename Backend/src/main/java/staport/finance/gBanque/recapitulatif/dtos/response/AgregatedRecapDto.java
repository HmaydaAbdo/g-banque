package staport.finance.gBanque.recapitulatif.dtos.response;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public record AgregatedRecapDto(
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        Page<RecaputilatifItem> page
) {
}
