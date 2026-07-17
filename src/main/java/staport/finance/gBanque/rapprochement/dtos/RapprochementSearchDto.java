package staport.finance.gBanque.rapprochement.dtos;

import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * Critères de recherche des rapprochements (liés depuis les query params GET — binding par
 * constructeur canonique du record, pattern déjà éprouvé sur {@code EncoursImpayeeSearchDto}/
 * {@code LogSearchCriteria}).
 *
 * <p>{@code dateRapprochement} reste une {@code String} brute sur le DTO (format "M/yyyy" ou
 * "MM/yyyy", ex. "1/2026" ou "01/2026") plutôt qu'un {@link YearMonth} typé : un second
 * constructeur délégant aurait introduit une ambiguïté pour le data binder Spring (deux
 * constructeurs candidats). Le parsing est fait à la demande via {@link #parsedDateRapprochement()},
 * appelé uniquement par {@code RapprochementSpecifications}.
 */
public record RapprochementSearchDto(
        Long banqueId,
        String dateRapprochement,
        StatutRapprochement statutRapprochement
) {

    private static final DateTimeFormatter DATE_RAPPROCHEMENT_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.MONTH_OF_YEAR)   // 1 ou 2 chiffres
            .appendLiteral('/')
            .appendValue(ChronoField.YEAR, 4)
            .toFormatter();

    /** Parse {@link #dateRapprochement} ("M/yyyy" ou "MM/yyyy") — {@code null} si absent. */
    public YearMonth parsedDateRapprochement() {
        if (dateRapprochement == null || dateRapprochement.isBlank()) {
            return null;
        }
        return YearMonth.parse(dateRapprochement.trim(), DATE_RAPPROCHEMENT_FORMATTER);
    }
}
