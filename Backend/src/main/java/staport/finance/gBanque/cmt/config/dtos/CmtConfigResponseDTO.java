package staport.finance.gBanque.cmt.config.dtos;

import java.time.OffsetDateTime;

public record CmtConfigResponseDTO(
        Long id,
        Integer alerteEcheanceJoursCourtTerme,
        Integer alerteEcheanceJoursLongTerme,
        OffsetDateTime dateModification
) {}
