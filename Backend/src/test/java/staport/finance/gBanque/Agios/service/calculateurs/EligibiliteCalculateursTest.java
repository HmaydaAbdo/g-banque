package staport.finance.gBanque.Agios.service.calculateurs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.INatureLigneFonctionnelleService;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.services.LigneFonctionnelleReader;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.services.LigneFonctionnelleSpecifiqueReader;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.droitConstate.service.DroitConstateReader;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.factoring.service.FactoringReader;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.service.IMarcheService;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.services.PrefinancementReader;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Éligibilité des calculateurs d'agios multi-marchés (droits constatés, préfinancement, factoring) : une banque n'est
 * calculée que si un de ses marchés a une ligne active ET au moins une opération payée. Sans cela, le planificateur créait
 * des périodes vides (0 jour, 0 MAD).
 */
class EligibiliteCalculateursTest {

    private final IMarcheService marcheService = mock(IMarcheService.class);
    private final LigneFonctionnelleSpecifiqueReader specifiqueReader = mock(LigneFonctionnelleSpecifiqueReader.class);
    private final LigneFonctionnelleReader generaleReader = mock(LigneFonctionnelleReader.class);
    private final INatureLigneFonctionnelleService natureService = mock(INatureLigneFonctionnelleService.class);
    private final Banque banque = mock(Banque.class);
    private final Marche marche = mock(Marche.class);

    EligibiliteCalculateursTest() {
        when(banque.getId()).thenReturn(1L);
        when(banque.getNom()).thenReturn("BMCI");
        when(marche.getId()).thenReturn(10L);
        when(marche.getNom()).thenReturn("GUERCIF-NADOR");
        when(marcheService.findByBanqueId(1L)).thenReturn(List.of(marche));
        NatureLigneFonctionnelle nature = mock(NatureLigneFonctionnelle.class);
        when(nature.getId()).thenReturn(5L);
        when(natureService.findByCode(anyString())).thenReturn(Optional.of(nature));
        when(generaleReader.findActiveLineForMarcheSansVerrou(any(), any())).thenReturn(Optional.empty());
    }

    /** Active (ou non) une ligne spécifique à 5 % sur le marché pour la nature résolue. */
    private void ligneActive(boolean active) {
        LigneFonctionnelleSpecifique ligne = mock(LigneFonctionnelleSpecifique.class);
        when(ligne.getTaux()).thenReturn(new BigDecimal("5.00"));
        when(specifiqueReader.findActiveSansVerrou(any(), any()))
                .thenReturn(active ? Optional.of(ligne) : Optional.empty());
    }

    private DroitConstate droitConstate(Status status) {
        DroitConstate o = new DroitConstate();
        o.setStatus(status);
        return o;
    }

    private Prefinancement prefinancement(Status status) {
        Prefinancement o = new Prefinancement();
        o.setStatus(status);
        return o;
    }

    private Factoring factoring(Status status) {
        Factoring o = new Factoring();
        o.setStatus(status);
        return o;
    }

    private DroitConstateAgiosCalculateur calculateurDc(List<DroitConstate> operations) {
        DroitConstateReader reader = mock(DroitConstateReader.class);
        when(reader.findByMarche(marche)).thenReturn(operations);
        return new DroitConstateAgiosCalculateur(marcheService, specifiqueReader, generaleReader, natureService, reader);
    }

    private PrefinancementAgiosCalculateur calculateurPrefi(List<Prefinancement> operations) {
        PrefinancementReader reader = mock(PrefinancementReader.class);
        when(reader.findByMarche(marche)).thenReturn(operations);
        return new PrefinancementAgiosCalculateur(marcheService, specifiqueReader, generaleReader, natureService, reader);
    }

    private FactoringAgiosCalculateur calculateurFactoring(List<Factoring> operations) {
        FactoringReader reader = mock(FactoringReader.class);
        when(reader.findByMarche(marche)).thenReturn(operations);
        return new FactoringAgiosCalculateur(marcheService, specifiqueReader, generaleReader, natureService, reader);
    }

    @ParameterizedTest(name = "opération {0} : éligible = {1}")
    @CsvSource({"PAYE,true", "PREVU,false"})
    @DisplayName("Avec une ligne active, seule une opération PAYE rend la banque éligible (3 types)")
    void ligneActiveEtStatutDeLOperation(Status statut, boolean attendu) {
        ligneActive(true);

        assertThat(calculateurDc(List.of(droitConstate(statut))).estEligible(banque)).isEqualTo(attendu);
        assertThat(calculateurPrefi(List.of(prefinancement(statut))).estEligible(banque)).isEqualTo(attendu);
        assertThat(calculateurFactoring(List.of(factoring(statut))).estEligible(banque)).isEqualTo(attendu);
    }

    @Test
    @DisplayName("Avec une ligne active mais aucune opération, la banque n'est pas calculée (3 types)")
    void aucuneOperation() {
        ligneActive(true);

        assertThat(calculateurDc(List.of()).estEligible(banque)).isFalse();
        assertThat(calculateurPrefi(List.of()).estEligible(banque)).isFalse();
        assertThat(calculateurFactoring(List.of()).estEligible(banque)).isFalse();
    }

    @Test
    @DisplayName("Une opération PAYE sans ligne active ne rend pas la banque éligible (3 types)")
    void aucuneLigne() {
        ligneActive(false);

        assertThat(calculateurDc(List.of(droitConstate(Status.PAYE))).estEligible(banque)).isFalse();
        assertThat(calculateurPrefi(List.of(prefinancement(Status.PAYE))).estEligible(banque)).isFalse();
        assertThat(calculateurFactoring(List.of(factoring(Status.PAYE))).estEligible(banque)).isFalse();
    }
}
