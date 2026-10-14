package staport.finance.gBanque.recapitulatif.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PeriodeRecapTest {

    @Test
    @DisplayName("À ce jour : l'horizon est le jour même")
    void aCeJour() {
        assertThat(PeriodeRecap.A_CE_JOUR.horizon(LocalDate.of(2026, 10, 14))).isEqualTo(LocalDate.of(2026, 10, 14));
    }

    @ParameterizedTest(name = "{0} → fin de mois {1}")
    @CsvSource({
            "2026-10-14, 2026-10-31",
            "2026-10-31, 2026-10-31",
            "2028-02-03, 2028-02-29",
            "2026-02-10, 2026-02-28",
    })
    @DisplayName("Fin de mois : dernier jour du mois en cours (années bissextiles comprises)")
    void finDeMois(LocalDate aujourdHui, LocalDate attendu) {
        assertThat(PeriodeRecap.FIN_DE_MOIS.horizon(aujourdHui)).isEqualTo(attendu);
    }

    @ParameterizedTest(name = "{0} → fin de trimestre {1}")
    @CsvSource({
            "2026-01-01, 2026-03-31",
            "2026-03-31, 2026-03-31",
            "2026-04-01, 2026-06-30",
            "2026-08-15, 2026-09-30",
            "2026-10-14, 2026-12-31",
            "2026-12-31, 2026-12-31",
    })
    @DisplayName("Fin de trimestre : dernier jour du trimestre civil en cours")
    void finDeTrimestre(LocalDate aujourdHui, LocalDate attendu) {
        assertThat(PeriodeRecap.FIN_DE_TRIMESTRE.horizon(aujourdHui)).isEqualTo(attendu);
    }

    @Test
    @DisplayName("Libellés des rapports")
    void libelles() {
        assertThat(PeriodeRecap.FIN_DE_TRIMESTRE.libelle()).isEqualTo("jusqu'à fin de trimestre");
        assertThat(PeriodeRecap.FIN_DE_MOIS.libelleCourt()).isEqualTo("fin du mois");
    }
}
