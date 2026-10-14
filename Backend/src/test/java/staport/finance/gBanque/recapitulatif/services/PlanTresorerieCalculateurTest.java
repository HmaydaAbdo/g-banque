package staport.finance.gBanque.recapitulatif.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.recapitulatif.dtos.response.ColonnePlanDto;
import staport.finance.gBanque.recapitulatif.dtos.response.LignePlanDto;
import staport.finance.gBanque.recapitulatif.dtos.response.PlanBanqueDto;
import staport.finance.gBanque.recapitulatif.dtos.response.PlanTresorerieDto;
import staport.finance.gBanque.recapitulatif.enums.PeriodeRecap;
import staport.finance.gBanque.recapitulatif.services.PlanTresorerieCalculateur.FluxPrevu;
import staport.finance.gBanque.recapitulatif.services.PlanTresorerieCalculateur.SoldeBanque;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static staport.finance.gBanque.moyensDePaiements.virements.enums.Type.DECAISSEMENT;
import static staport.finance.gBanque.moyensDePaiements.virements.enums.Type.ENCAISSEMENT;

/**
 * Plan de trésorerie calculé le mercredi 14/10/2026.
 *
 * <p>Banque A : solde actuel 100 000, plafond FC 50 000. Encours non validés :</p>
 * <ul>
 *   <li>10/10 (en retard) Leasing −30 000 ; 14/10 Chèque +5 000 → colonne « À ce jour »</li>
 *   <li>16/10 LCN −120 000 (semaine du 15 au 18)</li>
 *   <li>20/10 Virement +80 000 (semaine du 19 au 25)</li>
 *   <li>28/10 Décompte −2 000 (hors catégorie de décaissement → « Autres ») ; 31/10 Agios −10 000</li>
 *   <li>05/11 Leasing −999 999 : après la fin du mois</li>
 * </ul>
 */
class PlanTresorerieCalculateurTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 10, 14);
    private static final Long A = 1L;
    private static final Long B = 2L;

    private static BigDecimal bd(String valeur) {
        return new BigDecimal(valeur);
    }

    private static FluxPrevu flux(Long banque, TransactionSource source, Type sens, String date, String montant) {
        return new FluxPrevu(banque, source, sens, LocalDate.parse(date), bd(montant));
    }

    private static List<FluxPrevu> fluxBanqueA() {
        return List.of(
                flux(A, TransactionSource.LEASING, DECAISSEMENT, "2026-10-10", "30000"),
                flux(A, TransactionSource.CHEQUE, ENCAISSEMENT, "2026-10-14", "5000"),
                flux(A, TransactionSource.LCN_DECAISSEMENT, DECAISSEMENT, "2026-10-16", "120000"),
                flux(A, TransactionSource.Virement, ENCAISSEMENT, "2026-10-20", "80000"),
                flux(A, TransactionSource.DECOMPTES, DECAISSEMENT, "2026-10-28", "2000"),
                flux(A, TransactionSource.AGIOS, DECAISSEMENT, "2026-10-31", "10000"),
                flux(A, TransactionSource.LEASING, DECAISSEMENT, "2026-11-05", "999999"));
    }

    private static PlanBanqueDto planA(String plafond) {
        PlanTresorerieDto plan = PlanTresorerieCalculateur.calculer(PeriodeRecap.FIN_DE_MOIS, AUJOURDHUI,
                List.of(new SoldeBanque(A, "Banque A", bd("100000"), bd(plafond))), fluxBanqueA());
        return plan.banques().get(0);
    }

    private static void assertMontants(List<BigDecimal> reel, String... attendus) {
        assertThat(reel).hasSize(attendus.length);
        for (int i = 0; i < attendus.length; i++) {
            assertThat(reel.get(i)).as("colonne %d", i).isEqualByComparingTo(attendus[i]);
        }
    }

    @Test
    @DisplayName("Fin de mois : « À ce jour » puis une colonne par semaine (lundi-dimanche), la première incomplète")
    void colonnesFinDeMois() {
        List<ColonnePlanDto> colonnes = PlanTresorerieCalculateur.colonnes(
                PeriodeRecap.FIN_DE_MOIS, AUJOURDHUI, LocalDate.of(2026, 10, 31));

        assertThat(colonnes).extracting(ColonnePlanDto::code).containsExactly("ECHU", "P1", "P2", "P3");
        assertThat(colonnes.get(0).du()).isNull();
        assertThat(colonnes.get(0).au()).isEqualTo(AUJOURDHUI);
        assertThat(colonnes.get(1).du()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(colonnes.get(1).au()).isEqualTo(LocalDate.of(2026, 10, 18));
        assertThat(colonnes.get(2).libelle()).isEqualTo("Du 19/10 au 25/10");
        assertThat(colonnes.get(3).au()).isEqualTo(LocalDate.of(2026, 10, 31));
    }

    @Test
    @DisplayName("Fin de trimestre : une colonne par mois civil jusqu'au 31/12")
    void colonnesFinDeTrimestre() {
        List<ColonnePlanDto> colonnes = PlanTresorerieCalculateur.colonnes(
                PeriodeRecap.FIN_DE_TRIMESTRE, AUJOURDHUI, LocalDate.of(2026, 12, 31));

        assertThat(colonnes).extracting(ColonnePlanDto::libelle)
                .containsExactly("À ce jour", "Octobre 2026", "Novembre 2026", "Décembre 2026");
        assertThat(colonnes.get(1).du()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(colonnes.get(2).du()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(colonnes.get(3).au()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    @DisplayName("Dernier jour du mois : seule la colonne « À ce jour » reste")
    void dernierJourDuMois() {
        assertThat(PlanTresorerieCalculateur.colonnes(
                PeriodeRecap.FIN_DE_MOIS, LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 31)))
                .extracting(ColonnePlanDto::code).containsExactly("ECHU");
    }

    @Test
    @DisplayName("Totaux par colonne et soldes cumulés ; les encours en retard comptent dès aujourd'hui")
    void soldesCumules() {
        PlanBanqueDto a = planA("50000");

        assertMontants(a.totalEncaissements(), "5000", "0", "80000", "0");
        assertMontants(a.totalDecaissements(), "30000", "120000", "0", "12000");
        assertMontants(a.soldesFin(), "75000", "-45000", "35000", "23000");
        assertMontants(a.disponibilites(), "125000", "5000", "85000", "73000");
        assertThat(a.soldeFinal()).isEqualByComparingTo("23000");
        assertThat(a.disponibiliteFinale()).isEqualByComparingTo("73000");
        // Le leasing du 05/11 est au-delà de la fin du mois : absent du plan
        assertThat(a.decaissements()).filteredOn(l -> l.categorie().equals("Leasing"))
                .singleElement().extracting(LignePlanDto::total).satisfies(t -> assertThat(t).isEqualByComparingTo("30000"));
    }

    @Test
    @DisplayName("Lignes par catégorie dans l'ordre du récapitulatif détaillé, « Autres » pour une source hors catégorie")
    void lignesParCategorie() {
        PlanBanqueDto a = planA("50000");

        assertThat(a.decaissements()).extracting(LignePlanDto::categorie)
                .containsExactly("Leasing", "REGLEMENT FRNS", "Agios", "Autres");
        assertThat(a.encaissements()).extracting(LignePlanDto::categorie).containsExactly("REGLEMENT FRNS");
        assertMontants(a.encaissements().get(0).montants(), "5000", "0", "80000", "0");
        assertThat(a.encaissements().get(0).total()).isEqualByComparingTo("85000");
    }

    @Test
    @DisplayName("Point bas : jour du minimum de la courbe, jours négatifs, aucun besoin si la facilité couvre")
    void pointBasCouvertParLaFacilite() {
        PlanBanqueDto a = planA("50000");

        assertThat(a.courbe()).hasSize(18);
        assertThat(a.courbe().get(0).solde()).isEqualByComparingTo("75000");
        assertThat(a.pointBas().date()).isEqualTo(LocalDate.of(2026, 10, 16));
        assertThat(a.pointBas().solde()).isEqualByComparingTo("-45000");
        assertThat(a.pointBas().disponibilite()).isEqualByComparingTo("5000");
        assertThat(a.besoinFinancement()).isEqualByComparingTo("0");
        // 16, 17, 18 et 19 octobre : le virement du 20 ramène le solde au-dessus de zéro
        assertThat(a.joursSoldeNegatif()).isEqualTo(4);
    }

    @Test
    @DisplayName("Besoin de financement : ce qui manque au point bas une fois la facilité de caisse épuisée")
    void besoinDeFinancement() {
        PlanBanqueDto a = planA("20000");

        assertThat(a.pointBas().disponibilite()).isEqualByComparingTo("-25000");
        assertThat(a.besoinFinancement()).isEqualByComparingTo("25000");
    }

    @Test
    @DisplayName("Consolidé : somme des banques, point bas pris sur la courbe consolidée et non sur la somme des points bas")
    void consolide() {
        List<FluxPrevu> flux = new ArrayList<>(fluxBanqueA());
        flux.add(flux(B, TransactionSource.Virement, ENCAISSEMENT, "2026-10-16", "60000"));
        flux.add(flux(B, TransactionSource.Virement, DECAISSEMENT, "2026-10-20", "50000"));
        flux.add(flux(99L, TransactionSource.CHEQUE, ENCAISSEMENT, "2026-10-16", "1000000"));

        PlanTresorerieDto plan = PlanTresorerieCalculateur.calculer(PeriodeRecap.FIN_DE_MOIS, AUJOURDHUI, List.of(
                new SoldeBanque(A, "Banque A", bd("100000"), bd("50000")),
                new SoldeBanque(B, "Banque B", bd("10000"), null)), flux);
        PlanBanqueDto total = plan.consolide();

        assertThat(total.banqueId()).isNull();
        assertThat(total.soldeActuel()).isEqualByComparingTo("110000");
        assertThat(total.plafondFC()).isEqualByComparingTo("50000");
        // 14/10 : 85 000 ; 16/10 : 85 000 − 120 000 + 60 000 = 25 000 ; 20/10 : 55 000
        assertThat(total.pointBas().date()).isEqualTo(LocalDate.of(2026, 10, 16));
        assertThat(total.pointBas().solde()).isEqualByComparingTo("25000");
        assertThat(total.joursSoldeNegatif()).isZero();
        // La banque 99, inconnue, n'entre ni dans le détail ni dans le consolidé
        assertThat(total.soldeFinal()).isEqualByComparingTo("43000");
        assertThat(plan.banques()).extracting(PlanBanqueDto::banqueNom).containsExactly("Banque A", "Banque B");
        assertThat(plan.banques().get(1).plafondFC()).isEqualByComparingTo("0");
    }
}
