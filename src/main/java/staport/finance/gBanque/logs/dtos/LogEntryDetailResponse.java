package staport.finance.gBanque.logs.dtos;

/**
 * Détail champ-par-champ (avant/après) d'une ligne de journal d'audit.
 */
public record LogEntryDetailResponse(

        Long id,
        String fieldName,
        String oldValue,
        String newValue

) {}
