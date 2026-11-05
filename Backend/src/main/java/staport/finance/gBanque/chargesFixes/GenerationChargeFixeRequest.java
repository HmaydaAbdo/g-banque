package staport.finance.gBanque.chargesFixes;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record GenerationChargeFixeRequest(
        @NotNull @Min(2000) @Max(2100) Integer annee,
        @NotNull @Min(1) @Max(12) Integer mois,
        List<Long> configurationIds
) {}
