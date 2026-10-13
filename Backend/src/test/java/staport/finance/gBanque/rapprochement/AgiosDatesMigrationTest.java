package staport.finance.gBanque.rapprochement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import staport.finance.gBanque.AbstractMigrationTest;

import java.sql.Statement;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Migration V11 : les dates d'opération et de valeur de huit agios sont reprises de leur transaction. Rejouée sur une base
 * jetable amenée en V10 ; un agios hors liste, aux dates divergentes, n'est pas touché ; puis base vierge (sans effet).
 */
class AgiosDatesMigrationTest extends AbstractMigrationTest {

    private static String agios(int id, String operation, String valeur) {
        return "INSERT INTO agios (id, banque_id, status, montant, date_emission, date_operation, date_valeur, libelle, observation) "
                + "VALUES (" + id + ", 1, 'PAYE', 100.00, '2026-04-01', '" + operation + "', '" + valeur + "', 'AGIOS', 'AGIOS')";
    }

    private static String transaction(int id, int referent) {
        return "INSERT INTO transaction (id, rapprochement_id, banque_id, transaction_source, referent, debit, credit, "
                + "date_emission, date_operations, date_valeur, libelle, observation) VALUES (" + id + ", 1, 1, 'AGIOS', " + referent
                + ", 100.00, 0.00, '2026-04-01', '2026-03-31', '2026-03-30', 'AGIOS', 'AGIOS')";
    }

    @Test
    @DisplayName("V11 : l'agios 8 reprend les dates de sa transaction ; un agios hors liste n'est pas modifié")
    void datesReprisesDeLaTransaction() throws Exception {
        String url = baseJetable();
        migrer(url, "10");
        try (var c = connexion(url); Statement st = c.createStatement()) {
            st.execute("SET session_replication_role = replica");
            st.execute(agios(8, "2026-04-05", "2026-04-06"));
            st.execute(transaction(125, 8));
            st.execute(agios(999, "2026-04-05", "2026-04-06"));
            st.execute(transaction(126, 999));
        }

        migrer(url, null);

        try (var c = connexion(url); Statement st = c.createStatement()) {
            assertThat(dates(st, "SELECT date_operation FROM agios WHERE id = 8")).containsExactly(LocalDate.of(2026, 3, 31));
            assertThat(dates(st, "SELECT date_valeur FROM agios WHERE id = 8")).containsExactly(LocalDate.of(2026, 3, 30));
            assertThat(texte(st, "SELECT montant FROM agios WHERE id = 8")).isEqualTo("100.0000");
            assertThat(dates(st, "SELECT date_operation FROM agios WHERE id = 999")).containsExactly(LocalDate.of(2026, 4, 5));
        }
    }

    @Test
    @DisplayName("V11 : sans effet sur une base vierge")
    void sansEffetSurBaseVierge() throws Exception {
        String url = baseJetable();
        migrer(url, null);
        try (var c = connexion(url); Statement st = c.createStatement()) {
            assertThat(texte(st, "SELECT count(*) FROM agios")).isEqualTo("0");
        }
    }
}
