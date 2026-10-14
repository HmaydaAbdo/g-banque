package staport.finance.gBanque.recapitulatif.dtos.response;

import staport.finance.gBanque.recapitulatif.enums.PeriodeRecap;

import java.time.LocalDate;
import java.util.List;

public record RecapGeneralDetailleAllBanquesDto(
        LocalDate dateRecap,
        PeriodeRecap periode,
        LocalDate dateHorizon,
        List<RecapGeneralDetailleDto> banques
) {
}
