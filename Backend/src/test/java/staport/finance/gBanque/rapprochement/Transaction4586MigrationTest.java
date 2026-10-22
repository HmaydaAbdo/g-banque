package staport.finance.gBanque.rapprochement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import staport.finance.gBanque.AbstractMigrationTest;

import java.sql.Statement;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Migration V21 : la transaction 4586 passe du frais bancaire 268 à un nouvel agios, le frais bancaire 268 est supprimé.
 * Rejouée sur une base jetable amenée en V20, avec les lignes réelles ; puis sur une base où le cas n'existe pas (sans effet).
 */
class Transaction4586MigrationTest extends AbstractMigrationTest {

    @Test
    @DisplayName("V21 : la transaction 4586 pointe sur un agios payé, le frais bancaire 268 disparaît, les montants ne bougent pas")
    void transactionReaffecteeALAgios() throws Exception {
        String url = baseJetable();
        migrer(url, "20");
        try (var c = connexion(url); Statement st = c.createStatement()) {
            st.execute("SET session_replication_role = replica");
            st.execute("INSERT INTO banque (id, date_creation, date_modification, numero_compte, agence, nom, address, devise, logo_data) "
                    + "VALUES (3, now(), now(), 'IT-4586', 'AGENCE', 'BANQUE 3', 'ADRESSE', 'MAD', decode('00', 'hex'))");
            st.execute("INSERT INTO frais_bancaire (id, banque_id, montant, date_emission, date_operation, date_valeur, "
                    + "libelle, observation, status) VALUES (268, 3, 309724.83, '2026-08-03', '2026-08-04', '2026-08-03', "
                    + "'FRAIS D ENGAGEMENT / MOBILISATION FACTORING', 'FRAIS D ENGAGEMENT / MOBILISATION FACTORING', 'PAYE')");
            st.execute("INSERT INTO transaction (id, rapprochement_id, banque_id, libelle, observation, transaction_source, referent, "
                    + "credit, debit, tva_deductible, date_emission, date_operations, date_valeur) VALUES (4586, 59, 3, "
                    + "'FRAIS D ENGAGEMENT / MOBILISATION FACTORING', 'FRAIS D ENGAGEMENT / MOBILISATION FACTORING', "
                    + "'FRAIS_BANCAIRE', 268, 0.00, 309724.83, 28156.80, '2026-08-03', '2026-08-04', '2026-08-03')");
        }

        migrer(url, "21");

        try (var c = connexion(url); Statement st = c.createStatement()) {
            assertThat(texte(st, "SELECT transaction_source FROM transaction WHERE id = 4586")).isEqualTo("AGIOS");
            assertThat(texte(st, "SELECT libelle FROM transaction WHERE id = 4586")).isEqualTo("AGIOS SUR MOBILISATION FACTORING");
            assertThat(texte(st, "SELECT observation FROM transaction WHERE id = 4586")).isEqualTo("AGIOS SUR MOBILISATION FACTORING");
            assertThat(texte(st, "SELECT tva_deductible FROM transaction WHERE id = 4586")).isEqualTo("0.00");
            assertThat(texte(st, "SELECT debit FROM transaction WHERE id = 4586")).isEqualTo("309724.83");
            assertThat(texte(st, "SELECT rapprochement_id FROM transaction WHERE id = 4586")).isEqualTo("59");

            assertThat(texte(st, "SELECT count(*) FROM agios")).isEqualTo("1");
            String agios = "SELECT %s FROM agios WHERE id = (SELECT referent FROM transaction WHERE id = 4586)";
            assertThat(texte(st, agios.formatted("status"))).isEqualTo("PAYE");
            assertThat(texte(st, agios.formatted("montant"))).isEqualTo("309724.8300");
            assertThat(texte(st, agios.formatted("banque_id"))).isEqualTo("3");
            assertThat(dates(st, agios.formatted("date_emission"))).containsExactly(LocalDate.of(2026, 8, 3));
            assertThat(dates(st, agios.formatted("date_operation"))).containsExactly(LocalDate.of(2026, 8, 4));
            assertThat(dates(st, agios.formatted("date_valeur"))).containsExactly(LocalDate.of(2026, 8, 3));

            assertThat(texte(st, "SELECT count(*) FROM frais_bancaire WHERE id = 268")).isEqualTo("0");
        }
    }

    @Test
    @DisplayName("V21 : sans effet sur une base où le frais bancaire 268 n'existe pas")
    void sansEffetSurBaseVierge() throws Exception {
        String url = baseJetable();
        migrer(url, null);
        try (var c = connexion(url); Statement st = c.createStatement()) {
            assertThat(texte(st, "SELECT count(*) FROM transaction")).isEqualTo("0");
            assertThat(texte(st, "SELECT count(*) FROM agios")).isEqualTo("0");
        }
    }
}
