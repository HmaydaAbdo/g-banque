package staport.finance.gBanque.cautions.commissions.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RattrapageCommissionCautionRequest(
        @NotNull @Min(2000) @Max(2100) Integer annee,
        @NotNull @Min(1) @Max(12) Integer mois,
        @NotNull Boolean debutMois,
        List<Long> banqueIds,
        List<String> lignesSelectionnees
) {}
