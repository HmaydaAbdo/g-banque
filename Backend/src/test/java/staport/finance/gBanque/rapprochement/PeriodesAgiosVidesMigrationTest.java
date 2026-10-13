package staport.finance.gBanque.rapprochement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import staport.finance.gBanque.AbstractMigrationTest;

import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Migration V12 : les périodes d'agios vides (0 MAD, 0 jour) d'une banque sans opération payée du type sont supprimées avec
 * leurs lignes journalières nulles ; une période vide d'une banque qui a des opérations payées est conservée ; une période
 * qui porte un montant n'est jamais supprimée. Rejouée sur une base jetable amenée en V11.
 */
class PeriodesAgiosVidesMigrationTest extends AbstractMigrationTest {

    private static String periode(int id, int banque, String type, String montantTtc, int joursCalcules) {
        return "INSERT INTO agios_trimestriel (id, banque_id, annee, trimestre, date_debut, date_fin, nb_jours_calcules, "
                + "nb_jours_total, total_agios_ht, total_agios_ttc, total_tva, statut, type_agios) VALUES (" + id + ", " + banque
                + ", 2026, 3, '2026-07-01', '2026-09-30', " + joursCalcules + ", 92, " + montantTtc + ", " + montantTtc + ", 0.00, "
                + "'OUVERT', '" + type + "')";
    }

    private static String journalier(int periode, int banque, String type) {
        return "INSERT INTO agios_journalier (agios_ht, agios_ttc, base_calcul, date_valeur, nb_recalculs, solde_journalier, "
                + "taux_applique, tva, banque_id, date_calcul, trimestre_id, type_agios) VALUES (0, 0, 0, '2026-07-01', 1, 0, 0, 0, "
                + banque + ", now(), " + periode + ", '" + type + "')";
    }

    @Test
    @DisplayName("V12 : période vide sans opération supprimée avec ses lignes journalières ; période vide d'une banque avec opérations conservée")
    void periodesVides() throws Exception {
        String url = baseJetable();
        migrer(url, "11");
        try (var c = connexion(url); Statement st = c.createStatement()) {
            st.execute("SET session_replication_role = replica");
            // Banque 1 : aucune opération. Banque 3 : un préfinancement payé.
            st.execute("INSERT INTO marches (id, banque_id, actif, created_at, modified_at, montant, numero, maitre_douvrage, nom) "
                    + "VALUES (30, 3, true, now(), now(), 1000.00, 'M1', 'MO', 'GUERCIF-NADOR')");
            st.execute("INSERT INTO prefinancement (id, marche_id, type, status, montant, libelle, observation) "
                    + "VALUES (1, 30, 'DEBLOCAGE', 'PAYE', 100.00, 'Déblocage', 'Marche')");
            st.execute(periode(1, 1, "DROIT_CONSTATE", "0.00", 0));
            st.execute(journalier(1, 1, "DROIT_CONSTATE"));
            st.execute(periode(2, 1, "FACTORING", "0.00", 0));
            st.execute(periode(3, 3, "PREFINANCEMENT", "0.00", 0));
            st.execute(journalier(3, 3, "PREFINANCEMENT"));
            st.execute(periode(4, 3, "DROIT_CONSTATE", "0.00", 0));
        }

        migrer(url, null);

        try (var c = connexion(url); Statement st = c.createStatement()) {
            assertThat(texte(st, "SELECT count(*) FROM agios_trimestriel WHERE id IN (1, 2, 4)")).isEqualTo("0");
            assertThat(texte(st, "SELECT count(*) FROM agios_journalier WHERE trimestre_id = 1")).isEqualTo("0");
            assertThat(texte(st, "SELECT count(*) FROM agios_trimestriel WHERE id = 3")).isEqualTo("1");
            assertThat(texte(st, "SELECT count(*) FROM agios_journalier WHERE trimestre_id = 3")).isEqualTo("1");
        }
    }

    @Test
    @DisplayName("V12 : une période qui porte un montant n'est jamais supprimée")
    void periodeAvecMontantConservee() throws Exception {
        String url = baseJetable();
        migrer(url, "11");
        try (var c = connexion(url); Statement st = c.createStatement()) {
            st.execute("SET session_replication_role = replica");
            st.execute(periode(7, 1, "FACTORING", "110.00", 10));
        }

        migrer(url, null);

        try (var c = connexion(url); Statement st = c.createStatement()) {
            assertThat(texte(st, "SELECT count(*) FROM agios_trimestriel WHERE id = 7")).isEqualTo("1");
        }
    }
}
