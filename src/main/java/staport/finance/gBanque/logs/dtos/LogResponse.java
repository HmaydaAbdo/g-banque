package staport.finance.gBanque.logs.dtos;

import java.time.OffsetDateTime;

/**
 * Ligne du journal d'audit (une opération métier historisée).
 *
 * <p>Ne porte jamais le détail champ-par-champ : celui-ci est reconstruit à la
 * demande depuis la révision Envers et exposé par
 * {@code GET /logs/{id}/details}.
 */
public record LogResponse(

        Long id,
        OffsetDateTime timestamp,
        String userName,
        String operationType,
        String entityName,
        Long entityId

) {}
