package staport.finance.gBanque.Agios.service.calculateurs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.INatureLigneFonctionnelleService;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.services.LigneFonctionnelleReader;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.services.LigneFonctionnelleSpecifiqueReader;
import staport.finance.gBanque.droitConstate.service.DroitConstateReader;
import staport.finance.gBanque.factoring.service.FactoringReader;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.service.IMarcheService;
import staport.finance.gBanque.prefinancement.enums.Type;
import staport.finance.gBanque.prefinancement.services.PrefinancementReader;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Règle du 13/10/2026 : un remboursement n'agit sur l'encours que le lendemain de sa date de valeur (le jour de
 * remboursement est encore facturé), alors qu'un déblocage agit dès sa date de valeur. Valable pour les droits
 * constatés, le préfinancement et le factoring (pas pour la facilité de caisse, qui part du solde comptable).
 *
 * <p>Scénario commun : déblocage de 1 000 000 valeur le 01/08, remboursement de 1 000 000 valeur le 05/08, taux 3,60 %
 * (soit 100,00 d'agios HT par jour pour 1 000 000 d'encours, base 360).</p>
 */
class EncoursRemboursementLendemainTest {

    private static final BigDecimal MONTANT = new BigDecimal("1000000.00");
    private static final LocalDate DEBLOCAGE = LocalDate.of(2026, 8, 1);
    private static final LocalDate REMBOURSEMENT = LocalDate.of(2026, 8, 5);

    private final IMarcheService marcheService = mock(IMarcheService.class);
    private final LigneFonctionnelleSpecifiqueReader specifiqueReader = mock(LigneFonctionnelleSpecifiqueReader.class);
    private final LigneFonctionnelleReader generaleReader = mock(LigneFonctionnelleReader.class);
    private final INatureLigneFonctionnelleService natureService = mock(INatureLigneFonctionnelleService.class);
    private final Banque banque = mock(Banque.class);
    private final Marche marche = mock(Marche.class);

    EncoursRemboursementLendemainTest() {
        when(banque.getId()).thenReturn(1L);
        when(banque.getNom()).thenReturn("BMCE");
        when(marche.getId()).thenReturn(10L);
        when(marche.getNom()).thenReturn("38/2025/CM");
        when(marcheService.findByBanqueId(1L)).thenReturn(List.of(marche));
        NatureLigneFonctionnelle nature = mock(NatureLigneFonctionnelle.class);
        when(nature.getId()).thenReturn(5L);
        when(natureService.findByCode(anyString())).thenReturn(Optional.of(nature));
        when(generaleReader.findActiveLineForMarcheSansVerrou(any(), any())).thenReturn(Optional.empty());
        LigneFonctionnelleSpecifique ligne = mock(LigneFonctionnelleSpecifique.class);
        when(ligne.getTaux()).thenReturn(new BigDecimal("3.60"));
        when(specifiqueReader.findActiveSansVerrou(any(), any())).thenReturn(Optional.of(ligne));
    }

    /** Somme payée jusqu'au jour demandé (inclus) : le déblocage vaut MONTANT à partir du 01/08, le remboursement à partir du 05/08. */
    private static Answer<BigDecimal> sommeJusquAu(LocalDate dateValeur) {
        return invocation -> {
            LocalDate jusquAu = invocation.getArgument(2);
            return jusquAu.isBefore(dateValeur) ? BigDecimal.ZERO : MONTANT;
        };
    }

    private BigDecimal encoursDuJour(AgiosCalculateurMultiMarche calculateur, LocalDate jour) {
        return calculateur.calculerJourParMarche(banque, jour).get(0).resultat().baseCalcul();
    }

    private BigDecimal agiosHtDuJour(AgiosCalculateurMultiMarche calculateur, LocalDate jour) {
        return calculateur.calculerJourParMarche(banque, jour).get(0).resultat().agiosHT();
    }

    private DroitConstateAgiosCalculateur calculateurDroitConstate() {
        DroitConstateReader reader = mock(DroitConstateReader.class);
        when(reader.sumMontantPaye(eq(marche), eq(Type.DEBLOCAGE), any())).thenAnswer(sommeJusquAu(DEBLOCAGE));
        when(reader.sumMontantPaye(eq(marche), eq(Type.REMBOURSEMENT), any())).thenAnswer(sommeJusquAu(REMBOURSEMENT));
        return new DroitConstateAgiosCalculateur(marcheService, specifiqueReader, generaleReader, natureService, reader);
    }

    private PrefinancementAgiosCalculateur calculateurPrefinancement() {
        PrefinancementReader reader = mock(PrefinancementReader.class);
        when(reader.sumMontantPaye(eq(marche), eq(Type.DEBLOCAGE), any())).thenAnswer(sommeJusquAu(DEBLOCAGE));
        when(reader.sumMontantPaye(eq(marche), eq(Type.REMBOURSEMENT), any())).thenAnswer(sommeJusquAu(REMBOURSEMENT));
        return new PrefinancementAgiosCalculateur(marcheService, specifiqueReader, generaleReader, natureService, reader);
    }

    private FactoringAgiosCalculateur calculateurFactoring() {
        FactoringReader reader = mock(FactoringReader.class);
        when(reader.sumMontantPaye(eq(marche), eq(Type.DEBLOCAGE), any())).thenAnswer(sommeJusquAu(DEBLOCAGE));
        when(reader.sumMontantPaye(eq(marche), eq(Type.REMBOURSEMENT), any())).thenAnswer(sommeJusquAu(REMBOURSEMENT));
        return new FactoringAgiosCalculateur(marcheService, specifiqueReader, generaleReader, natureService, reader);
    }

    private void verifierRegle(AgiosCalculateurMultiMarche calculateur) {
        // Un déblocage agit dès sa date de valeur : le 01/08, l'encours est déjà de 1 000 000
        assertThat(encoursDuJour(calculateur, DEBLOCAGE)).isEqualByComparingTo(MONTANT);
        // La veille du remboursement : encours inchangé
        assertThat(encoursDuJour(calculateur, REMBOURSEMENT.minusDays(1))).isEqualByComparingTo(MONTANT);
        // Le jour du remboursement : encore facturé sur l'ancien encours
        assertThat(encoursDuJour(calculateur, REMBOURSEMENT)).isEqualByComparingTo(MONTANT);
        assertThat(agiosHtDuJour(calculateur, REMBOURSEMENT)).isEqualByComparingTo("100.00");
        // Le lendemain : le remboursement a produit son effet, plus d'encours donc plus d'agios
        assertThat(encoursDuJour(calculateur, REMBOURSEMENT.plusDays(1))).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(agiosHtDuJour(calculateur, REMBOURSEMENT.plusDays(1))).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Droits constatés : le jour du remboursement est encore facturé, le déblocage agit le jour même")
    void droitsConstates() {
        verifierRegle(calculateurDroitConstate());
    }

    @Test
    @DisplayName("Préfinancement : le jour du remboursement est encore facturé, le déblocage agit le jour même")
    void prefinancement() {
        verifierRegle(calculateurPrefinancement());
    }

    @Test
    @DisplayName("Factoring : le jour du remboursement est encore facturé, le déblocage agit le jour même")
    void factoring() {
        verifierRegle(calculateurFactoring());
    }
}
