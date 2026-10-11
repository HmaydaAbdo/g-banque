package staport.finance.gBanque.rapprochement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import staport.finance.gBanque.AbstractMigrationTest;

import java.sql.Statement;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Migration V5 : le virement 1814 (réglé, sans dates) reprend les dates de la transaction 250. Rejouée sur une base
 * jetable amenée en V4, avec les deux lignes réelles ; puis sur une base où le cas n'existe pas (sans effet).
 */
class Virement1814MigrationTest extends AbstractMigrationTest {

    @Test
    @DisplayName("V5 : les dates du virement 1814 viennent de la transaction 250 ; le statut ne change pas")
    void datesReprisesDeLaTransaction() throws Exception {
        String url = baseJetable();
        migrer(url, "4");
        try (var c = connexion(url); Statement st = c.createStatement()) {
            st.execute("SET session_replication_role = replica");
            st.execute("INSERT INTO virements (id, date_emission, montant, beneficiaire, libelle, observation, statut, type) "
                    + "VALUES (1814, '2026-01-19', 30000.00, 'AGNI BTP', 'AGNI BTP', ' VIREMENT N° 008981', 'REGLE', 'DECAISSEMENT')");
            st.execute("INSERT INTO transaction (id, rapprochement_id, libelle, observation, transaction_source, referent, "
                    + "date_operations, date_valeur) VALUES (250, 5, 'AGNI BTP', ' VIREMENT N° 008981', 'Virement', 1814, "
                    + "'2026-01-19', '2026-01-19')");
        }

        migrer(url, null);

        try (var c = connexion(url); Statement st = c.createStatement()) {
            assertThat(dates(st, "SELECT date_operation FROM virements WHERE id = 1814")).containsExactly(LocalDate.of(2026, 1, 19));
            assertThat(dates(st, "SELECT date_valeur FROM virements WHERE id = 1814")).containsExactly(LocalDate.of(2026, 1, 19));
            assertThat(texte(st, "SELECT statut FROM virements WHERE id = 1814")).isEqualTo("REGLE");
        }
    }

    @Test
    @DisplayName("V5 : sans effet sur une base où le virement 1814 n'existe pas")
    void sansEffetSurBaseVierge() throws Exception {
        String url = baseJetable();
        migrer(url, null);
        try (var c = connexion(url); Statement st = c.createStatement()) {
            assertThat(texte(st, "SELECT count(*) FROM virements")).isEqualTo("0");
        }
    }
}
