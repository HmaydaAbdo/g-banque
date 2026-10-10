package staport.finance.gBanque.common;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Stocke un {@link YearMonth} dans une colonne {@code date} (1er jour du mois) au lieu de la sérialisation
 * Java binaire que Hibernate applique par défaut, illisible et inutilisable en SQL.
 *
 * <p>Non appliqué automatiquement : chaque attribut concerné le déclare avec
 * {@code @Convert(converter = YearMonthDateConverter.class)}.
 */
@Converter
public class YearMonthDateConverter implements AttributeConverter<YearMonth, LocalDate> {

    @Override
    public LocalDate convertToDatabaseColumn(YearMonth mois) {
        return mois == null ? null : mois.atDay(1);
    }

    @Override
    public YearMonth convertToEntityAttribute(LocalDate date) {
        return date == null ? null : YearMonth.from(date);
    }
}
