package staport.finance.gBanque.chargesFixes;

import java.time.YearMonth;
import java.util.List;

public record GenerationChargeFixeResultDto(
        YearMonth moisGeneration,
        int candidates,
        int generees,
        int dejaPresentes,
        int excluesTrimestrielles,
        List<GenerationChargeFixeLineDto> lignes
) {}
