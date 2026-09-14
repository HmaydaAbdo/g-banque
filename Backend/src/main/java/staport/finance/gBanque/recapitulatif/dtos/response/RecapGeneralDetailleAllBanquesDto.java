package staport.finance.gBanque.recapitulatif.dtos.response;

import java.time.LocalDate;
import java.util.List;

public record RecapGeneralDetailleAllBanquesDto(
        LocalDate dateRecap,
        List<RecapGeneralDetailleDto> banques
) {
}
