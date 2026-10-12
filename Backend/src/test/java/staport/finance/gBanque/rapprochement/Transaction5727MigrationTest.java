package staport.finance.gBanque.rapprochement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import staport.finance.gBanque.AbstractMigrationTest;

import java.sql.Statement;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Migration V6 : la transaction 5727 passe du droit constaté 81 au décompte 61, le DC 81 est supprimé. Rejouée sur une
 * base jetable amenée en V5, avec les lignes réelles ; puis sur une base où le cas n'existe pas (sans effet).
 */
class Transaction5727MigrationTest extends AbstractMigrationTest {

    @Test
    @DisplayName("V6 : la transaction 5727 pointe sur le décompte 61 (payé), le DC 81 disparaît, les montants ne bougent pas")
    void transactionReaffecteeAuDecompte() throws Exception {
        String url = baseJetable();
        migrer(url, "5");
        try (var c = connexion(url); Statement st = c.createStatement()) {
            st.execute("SET session_replication_role = replica");
            st.execute("INSERT INTO droit_constate (id, banque_id, marche_id, montant, date_emission, date_operation, date_valeur, "
                    + "libelle, observation, status, type) VALUES (81, 1, 13, 1307057.94, '2026-09-29', '2026-09-29', '2026-09-30', "
                    + "'Déblocage  droit constaté  marché RABAT-OUJDA', 'Marche N° M0108/25/S', 'PAYE', 'DEBLOCAGE')");
            st.execute("INSERT INTO decompte (id, banque_encaissement_id, marche_id, montant, date_emission, "
                    + "libelle, observation, status) VALUES (61, 1, 13, 1307057.94, '2026-09-29', "
                    + "'ENCAISSEMENT DIRECT / DECOMPTE MARCHÉ RABAT-OUJDA', 'Marche: M0108/25/S', 'PREVU')");
            st.execute("INSERT INTO transaction (id, rapprochement_id, banque_id, libelle, observation, transaction_source, referent, "
                    + "credit, debit, tva_collectee, date_emission, date_operations, date_valeur) VALUES (5727, 65, 1, "
                    + "'Déblocage  droit constaté  marché RABAT-OUJDA', 'Marche N° M0108/25/S', 'DROIT_CONSTATE', 81, "
                    + "1307057.94, 0.00, 0.00, '2026-09-29', '2026-09-29', '2026-09-30')");
        }

        migrer(url, null);

        try (var c = connexion(url); Statement st = c.createStatement()) {
            assertThat(texte(st, "SELECT transaction_source FROM transaction WHERE id = 5727")).isEqualTo("DECOMPTES");
            assertThat(texte(st, "SELECT referent FROM transaction WHERE id = 5727")).isEqualTo("61");
            assertThat(texte(st, "SELECT libelle FROM transaction WHERE id = 5727"))
                    .isEqualTo("ENCAISSEMENT DIRECT / DECOMPTE MARCHÉ RABAT-OUJDA");
            assertThat(texte(st, "SELECT observation FROM transaction WHERE id = 5727")).isEqualTo("Marche: M0108/25/S");
            assertThat(texte(st, "SELECT tva_collectee FROM transaction WHERE id = 5727")).isEqualTo("217842.99");
            assertThat(texte(st, "SELECT credit FROM transaction WHERE id = 5727")).isEqualTo("1307057.94");
            assertThat(texte(st, "SELECT rapprochement_id FROM transaction WHERE id = 5727")).isEqualTo("65");

            assertThat(texte(st, "SELECT status FROM decompte WHERE id = 61")).isEqualTo("PAYE");
            assertThat(dates(st, "SELECT date_operation FROM decompte WHERE id = 61")).containsExactly(LocalDate.of(2026, 9, 29));
            assertThat(dates(st, "SELECT date_valeur FROM decompte WHERE id = 61")).containsExactly(LocalDate.of(2026, 9, 30));
            assertThat(texte(st, "SELECT libelle FROM decompte WHERE id = 61"))
                    .isEqualTo("ENCAISSEMENT DIRECT / DECOMPTE MARCHÉ RABAT-OUJDA");

            assertThat(texte(st, "SELECT count(*) FROM droit_constate WHERE id = 81")).isEqualTo("0");
        }
    }

    @Test
    @DisplayName("V6 : sans effet sur une base où la transaction 5727 n'existe pas")
    void sansEffetSurBaseVierge() throws Exception {
        String url = baseJetable();
        migrer(url, null);
        try (var c = connexion(url); Statement st = c.createStatement()) {
            assertThat(texte(st, "SELECT count(*) FROM transaction")).isEqualTo("0");
            assertThat(texte(st, "SELECT count(*) FROM decompte")).isEqualTo("0");
        }
    }
}
