package staport.finance.gBanque.cautions.commissions.service;

import java.time.YearMonth;
import java.util.List;

public record RattrapageCommissionCautionResultDto(
        YearMonth moisReference,
        boolean debutMois,
        int candidates,
        int generees,
        int dejaPresentees,
        int commissionsSansProvenanceAControler,
        long cautionsInactivesNonAnalysees,
        List<RattrapageCommissionCautionLineDto> lignes
) {}
