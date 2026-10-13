package staport.finance.gBanque.rapprochement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import staport.finance.gBanque.AbstractMigrationTest;

import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Migration V8 : les liens {@code agios_definitif_id} vers des agios supprimés passent à NULL ; un lien vers un agios
 * existant est conservé. Rejouée sur une base jetable amenée en V7, puis sur une base vierge (sans effet).
 */
class AgiosDefinitifOrphelinMigrationTest extends AbstractMigrationTest {

    private static String periode(int id, int trimestre, String definitif) {
        return "INSERT INTO agios_trimestriel (id, banque_id, annee, trimestre, date_debut, date_fin, nb_jours_calcules, "
                + "nb_jours_total, total_agios_ht, total_agios_ttc, total_tva, statut, type_agios, agios_definitif_id) VALUES ("
                + id + ", 1, 2026, " + trimestre + ", '2026-01-01', '2026-03-31', 90, 90, 100.00, 110.00, 10.00, 'CLOTURE', "
                + "'FACILITE_CAISSE', " + definitif + ")";
    }

    @Test
    @DisplayName("V8 : le lien vers un agios supprimé passe à NULL, le lien vers un agios existant est conservé")
    void lienOrphelinSupprime() throws Exception {
        String url = baseJetable();
        migrer(url, "7");
        try (var c = connexion(url); Statement st = c.createStatement()) {
            st.execute("SET session_replication_role = replica");
            st.execute("INSERT INTO agios (id, banque_id, montant, status, libelle, observation) "
                    + "VALUES (500, 1, 110.00, 'PREVU', 'Agios', 'Agios')");
            st.execute(periode(1, 1, "40"));
            st.execute(periode(2, 2, "500"));
            st.execute(periode(3, 3, "NULL"));
        }

        migrer(url, null);

        try (var c = connexion(url); Statement st = c.createStatement()) {
            assertThat(texte(st, "SELECT count(*) FROM agios_trimestriel WHERE id = 1 AND agios_definitif_id IS NULL")).isEqualTo("1");
            assertThat(texte(st, "SELECT agios_definitif_id FROM agios_trimestriel WHERE id = 2")).isEqualTo("500");
            assertThat(texte(st, "SELECT total_agios_ttc FROM agios_trimestriel WHERE id = 1")).isEqualTo("110.00");
        }
    }

    @Test
    @DisplayName("V8 : sans effet sur une base vierge")
    void sansEffetSurBaseVierge() throws Exception {
        String url = baseJetable();
        migrer(url, null);
        try (var c = connexion(url); Statement st = c.createStatement()) {
            assertThat(texte(st, "SELECT count(*) FROM agios_trimestriel")).isEqualTo("0");
        }
    }
}
