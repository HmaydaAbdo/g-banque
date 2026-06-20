package staport.finance.gBanque.security.dtos;

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
