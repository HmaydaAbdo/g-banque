package staport.finance.gBanque.moyensDePaiements.recap.dtos.response;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public record AgregatedRecapMoyenPaiementDto(
        BigDecimal total,
        Page<RecapMoyenPaiementItem> page
) {
}
