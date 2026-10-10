package staport.finance.gBanque.common;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class YearMonthDateConverterTest {

    private final YearMonthDateConverter converter = new YearMonthDateConverter();

    @Test
    void moisVersPremierJourDuMois() {
        assertThat(converter.convertToDatabaseColumn(YearMonth.of(2026, 1))).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(converter.convertToDatabaseColumn(YearMonth.of(2024, 2))).isEqualTo(LocalDate.of(2024, 2, 1));
    }

    @Test
    void dateVersMois_quelqueSoitLeJour() {
        assertThat(converter.convertToEntityAttribute(LocalDate.of(2026, 1, 1))).isEqualTo(YearMonth.of(2026, 1));
        assertThat(converter.convertToEntityAttribute(LocalDate.of(2026, 1, 31))).isEqualTo(YearMonth.of(2026, 1));
    }

    @Test
    void nullResteNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
