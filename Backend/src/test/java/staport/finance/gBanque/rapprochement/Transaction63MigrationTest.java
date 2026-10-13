package staport.finance.gBanque.rapprochement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import staport.finance.gBanque.AbstractMigrationTest;

import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Migration V7 : la transaction 63 (AWB) quitte l'agios 4 (SAHAM) pour l'agios 16 (AWB). Rejouée sur une base jetable
 * amenée en V6, avec les lignes réelles ; puis sur une base où le cas n'existe pas (sans effet).
 */
class Transaction63MigrationTest extends AbstractMigrationTest {

    @Test
    @DisplayName("V7 : la transaction 63 pointe sur l'agios 16 ; la transaction 20 reste sur l'agios 4 ; rien d'autre ne bouge")
    void transactionRattacheeAuBonAgios() throws Exception {
        String url = baseJetable();
        migrer(url, "6");
        try (var c = connexion(url); Statement st = c.createStatement()) {
            st.execute("SET session_replication_role = replica");
            st.execute("INSERT INTO agios (id, banque_id, montant, status, libelle, observation) VALUES "
                    + "(4, 5, 22856.98, 'PAYE', 'INTERETS DEBITEURS AU 31/12/25', 'INTERETS DEBITEURS AU 31/12/25'), "
                    + "(16, 1, 85784.02, 'PAYE', 'INTERET DEBITEURS', 'INTERET DEBITEURS')");
            st.execute("INSERT INTO transaction (id, rapprochement_id, banque_id, libelle, observation, transaction_source, referent, "
                    + "debit, credit) VALUES "
                    + "(20, 3, 5, 'INTERETS DEBITEURS AU 31/12/25', 'INTERETS DEBITEURS AU 31/12/25', 'AGIOS', 4, 22856.98, 0.00), "
                    + "(63, 2, 1, 'INTERET DEBITEURS', 'INTERET DEBITEURS', 'AGIOS', 4, 85784.02, 0.00)");
        }

        migrer(url, null);

        try (var c = connexion(url); Statement st = c.createStatement()) {
            assertThat(texte(st, "SELECT referent FROM transaction WHERE id = 63")).isEqualTo("16");
            assertThat(texte(st, "SELECT referent FROM transaction WHERE id = 20")).isEqualTo("4");
            assertThat(texte(st, "SELECT debit FROM transaction WHERE id = 63")).isEqualTo("85784.02");
            assertThat(texte(st, "SELECT rapprochement_id FROM transaction WHERE id = 63")).isEqualTo("2");
        }
    }

    @Test
    @DisplayName("V7 : sans effet sur une base où la transaction 63 n'existe pas")
    void sansEffetSurBaseVierge() throws Exception {
        String url = baseJetable();
        migrer(url, null);
        try (var c = connexion(url); Statement st = c.createStatement()) {
            assertThat(texte(st, "SELECT count(*) FROM transaction")).isEqualTo("0");
        }
    }
}
