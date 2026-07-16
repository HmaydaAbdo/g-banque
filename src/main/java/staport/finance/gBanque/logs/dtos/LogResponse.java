package staport.finance.gBanque.logs.dtos;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Ligne du journal d'audit (une opération métier historisée).
 */
public record LogResponse(

        Long id,
        OffsetDateTime timestamp,
        String userName,
        String operationType,
        String entityName,
        Long entityId,
        List<LogEntryDetailResponse> logDetails

) {}
