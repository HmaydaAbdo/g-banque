package staport.finance.gBanque.recapitulatif.services;

import org.junit.jupiter.api.Test;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.recapitulatif.dtos.response.PlanBanqueDto;
import staport.finance.gBanque.recapitulatif.enums.PeriodeRecap;
import staport.finance.gBanque.recapitulatif.repositories.SuiviBesoinBanqueRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SuiviBesoinServiceTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 10, 15);

    private final SuiviBesoinBanqueRepository repository = mock(SuiviBesoinBanqueRepository.class);
    private final SuiviBesoinService service = new SuiviBesoinService(repository, mock(BanqueRepository.class));

    private static PlanBanqueDto banque(long id, String besoin) {
        return new PlanBanqueDto(id, "B" + id, "MAD", BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), BigDecimal.ZERO, BigDecimal.ZERO, null, new BigDecimal(besoin), 0, List.of(), List.of(), null);
    }

    private static Object[] ligne(long banqueId, int jour, String besoin) {
        return new Object[]{banqueId, LocalDate.of(2026, 10, jour), new BigDecimal(besoin)};
    }

    private static List<Object[]> lignes(Object[]... lignes) {
        return List.of(lignes);
    }

    @Test
    void banqueEnBesoinSansHistoriqueEstEnBesoinDepuisAujourdhui() {
        when(repository.historique(eq(PeriodeRecap.FIN_DE_MOIS), any())).thenReturn(lignes());

        Map<Long, LocalDate> depuis = service.enBesoinDepuis(PeriodeRecap.FIN_DE_MOIS, AUJOURDHUI, List.of(banque(1, "100")));

        assertThat(depuis).containsEntry(1L, AUJOURDHUI);
    }

    @Test
    void leBesoinRemonteAuPremierJourApresLeDernierJourCouvert() {
        when(repository.historique(eq(PeriodeRecap.FIN_DE_MOIS), any())).thenReturn(lignes(
                ligne(1, 10, "0"), ligne(1, 11, "50"), ligne(1, 12, "0"), ligne(1, 13, "70"), ligne(1, 14, "80")));

        Map<Long, LocalDate> depuis = service.enBesoinDepuis(PeriodeRecap.FIN_DE_MOIS, AUJOURDHUI, List.of(banque(1, "100")));

        assertThat(depuis).containsEntry(1L, LocalDate.of(2026, 10, 13));
    }

    @Test
    void banqueRepasseeAZeroLaVeilleEstEnBesoinDepuisAujourdhui() {
        when(repository.historique(eq(PeriodeRecap.FIN_DE_MOIS), any())).thenReturn(lignes(ligne(1, 13, "70"), ligne(1, 14, "0")));

        Map<Long, LocalDate> depuis = service.enBesoinDepuis(PeriodeRecap.FIN_DE_MOIS, AUJOURDHUI, List.of(banque(1, "100")));

        assertThat(depuis).containsEntry(1L, AUJOURDHUI);
    }

    @Test
    void banqueSansBesoinN_aPasDeDate() {
        when(repository.historique(eq(PeriodeRecap.FIN_DE_MOIS), any())).thenReturn(lignes(ligne(1, 13, "70")));

        assertThat(service.enBesoinDepuis(PeriodeRecap.FIN_DE_MOIS, AUJOURDHUI, List.of(banque(1, "0")))).isEmpty();
    }
}
