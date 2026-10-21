package staport.finance.gBanque.cautions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.cautions.commissions.config.dtos.response.CautionCommissionConfigResponseDTO;
import staport.finance.gBanque.cautions.commissions.config.services.interfaces.ICautionCommissionConfigService;
import staport.finance.gBanque.cautions.commissions.strategy.impl.BpCommissionCautionStrategie;
import staport.finance.gBanque.cautions.entities.Caution;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.commision.enums.TypeCommission;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Calculs de la stratégie BP : trimestres civils, prorata d'entrée et de sortie, aucun minimum. */
class BpCommissionCautionStrategieTest {

    private final ICautionCommissionConfigService config = mock(ICautionCommissionConfigService.class);
    private final BpCommissionCautionStrategie strategie = new BpCommissionCautionStrategie(config);

    private Caution caution(LocalDate dateDePrise) {
        Banque banque = new Banque();
        banque.setNom("BP");
        NatureLigneFonctionnelle nature = new NatureLigneFonctionnelle();
        nature.setCode("CAUT");
        Caution caution = new Caution();
        caution.setBanque(banque);
        caution.setNature(nature);
        caution.setNumeroCaution("N-1");
        caution.setMontant(new BigDecimal("1000000.00"));
        caution.setTaux(new BigDecimal("1.2"));
        caution.setDateDePrise(dateDePrise);
        when(config.resolveOuLeverException("BP")).thenReturn(
                new CautionCommissionConfigResponseDTO(1L, 1L, "BP", BigDecimal.ZERO, new BigDecimal("36000.00")));
        return caution;
    }

    @Test
    @DisplayName("BP est trimestrielle et déclare le type TRIMESTRIELLE")
    void periodicite() {
        assertThat(strategie.periodiciteMois()).isEqualTo(3);
        assertThat(strategie.typeCommissionPeriodique()).isEqualTo(TypeCommission.TRIMESTRIELLE);
        assertThat(strategie.supporteProrataEntree()).isTrue();
        assertThat(strategie.supporteProrataSortie()).isTrue();
        assertThat(strategie.genererTva()).isFalse();
        assertThat(strategie.executerEnDebutMois()).isFalse();
    }

    @Test
    @DisplayName("Trimestre civil entier : jours du trimestre (T4 = 92 j), émise le 31/12")
    void trimestreEntier() {
        Commission c = strategie.calculer(caution(LocalDate.of(2026, 8, 10)), YearMonth.of(2026, 12));

        assertThat(c.getMontant()).isEqualByComparingTo("3066.67");   // 1 000 000 × 1,2 × 92 / 36 000
        assertThat(c.getDateEmission()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(c.getLibelle()).isEqualTo("caution CAUT N° N-1 — Trimestre : 92 j");
        assertThat(c.getObservation()).isEqualTo("CAUTION");
    }

    @Test
    @DisplayName("Le trimestre compte ses jours réels : T1 2026 = 90 j, T2 = 91 j")
    void joursReels() {
        assertThat(strategie.calculer(caution(LocalDate.of(2026, 1, 1)), YearMonth.of(2026, 3)).getLibelle()).endsWith("90 j");
        assertThat(strategie.calculer(caution(LocalDate.of(2026, 1, 1)), YearMonth.of(2026, 6)).getLibelle()).endsWith("91 j");
    }

    @Test
    @DisplayName("Prorata d'entrée : du jour de prise (inclus) à la fin du trimestre, daté du jour de prise")
    void prorataEntree() {
        Commission c = strategie.calculerProrataEntree(caution(LocalDate.of(2026, 11, 15)));

        assertThat(c.getMontant()).isEqualByComparingTo("1566.67");   // 47 jours : du 15/11 au 31/12 inclus
        assertThat(c.getDateEmission()).isEqualTo(LocalDate.of(2026, 11, 15));
        assertThat(c.getLibelle()).endsWith("Prorata entrée : 47 j");
    }

    @Test
    @DisplayName("Prorata de sortie : du début du trimestre à la main levée incluse, daté de la main levée")
    void prorataSortie() {
        Commission c = strategie.calculerMainLevee(caution(LocalDate.of(2026, 3, 5)), LocalDate.of(2026, 11, 20));

        assertThat(c.getMontant()).isEqualByComparingTo("1700.00");   // 51 jours : du 01/10 au 20/11 inclus
        assertThat(c.getDateEmission()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(c.getLibelle()).endsWith("Prorata sortie : 51 j");
    }

    @Test
    @DisplayName("Prise et main levée dans le même trimestre : on compte à partir du jour de prise")
    void priseEtSortieMemeTrimestre() {
        Commission c = strategie.calculerMainLevee(caution(LocalDate.of(2026, 11, 15)), LocalDate.of(2026, 11, 20));

        assertThat(c.getLibelle()).endsWith("Prorata sortie : 6 j");   // 15 au 20 novembre inclus
        assertThat(c.getMontant()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("Aucun minimum : une petite commission n'est pas relevée")
    void aucunMinimum() {
        Caution petite = caution(LocalDate.of(2026, 12, 30));
        petite.setMontant(new BigDecimal("1000.00"));

        Commission c = strategie.calculerProrataEntree(petite);   // 2 jours : 1 000 × 1,2 × 2 / 36 000 = 0,07

        assertThat(c.getMontant()).isEqualByComparingTo("0.07");
    }
}
