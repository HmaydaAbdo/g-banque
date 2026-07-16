package staport.finance.gBanque.logs.dtos;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

/**
 * Critères de recherche pour l'écran de consultation des journaux d'audit (endpoint {@code /logs}).
 * Lié en paramètres de requête GET (query params), pas en {@code @RequestBody}.
 */
public record LogSearchCriteria(

        String userName,
        String operationType,
        String entityName,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime timestampStart,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime timestampEnd

) {}
