package staport.finance.gBanque.department.dtos.response;

import java.time.Instant;

/**
 * DTO de sortie pour un département — jamais l'entité {@code Department} brute.
 */
public record DepartmentResponse(
        Long id,
        String name,
        String description,
        Instant createdAt
) {}
