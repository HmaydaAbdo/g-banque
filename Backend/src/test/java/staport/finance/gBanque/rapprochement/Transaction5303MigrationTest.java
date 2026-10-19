package staport.finance.gBanque.rapprochement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import staport.finance.gBanque.AbstractMigrationTest;

import java.sql.Statement;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Migration V15 : la transaction 5303 passe du factoring 72 à un nouveau décompte, le factoring 72 est supprimé. Rejouée
 * sur une base jetable amenée en V14, avec les lignes réelles ; puis sur une base où le cas n'existe pas (sans effet).
 */
class Transaction5303MigrationTest extends AbstractMigrationTest {

    @Test
    @DisplayName("V15 : la transaction 5303 pointe sur un décompte payé, le factoring 72 disparaît, les montants ne bougent pas")
    void transactionReaffecteeAuDecompte() throws Exception {
        String url = baseJetable();
        migrer(url, "14");
        try (var c = connexion(url); Statement st = c.createStatement()) {
            st.execute("SET session_replication_role = replica");
            st.execute("INSERT INTO banque (id, date_creation, date_modification, numero_compte, agence, nom, address, devise, logo_data) "
                    + "VALUES (3, now(), now(), 'IT-5303', 'AGENCE', 'BANQUE 3', 'ADRESSE', 'MAD', decode('00', 'hex'))");
            st.execute("INSERT INTO marches (id, banque_id, actif, created_at, modified_at, montant, numero, maitre_douvrage, nom) "
                    + "VALUES (2, 3, true, now(), now(), 10000000.00, '04/2023/DRETLO', 'MO', 'GUERCIF-NADOR')");
            st.execute("INSERT INTO factoring (id, marche_id, montant, date_emission, date_operation, date_valeur, "
                    + "libelle, observation, status, type) VALUES (72, 2, 6007550.15, '2026-09-07', '2026-09-07', '2026-09-08', "
                    + "'Déblocage factoring marché GUERCIF-NADOR', 'Marche N° 04/2023/DRETLO', 'PAYE', 'DEBLOCAGE')");
            st.execute("INSERT INTO transaction (id, rapprochement_id, banque_id, libelle, observation, transaction_source, referent, "
                    + "credit, debit, tva_collectee, date_emission, date_operations, date_valeur) VALUES (5303, 62, 3, "
                    + "'Déblocage factoring marché GUERCIF-NADOR', 'Marche N° 04/2023/DRETLO', 'FACTORING', 72, "
                    + "6007550.15, 0.00, 0.00, '2026-09-07', '2026-09-07', '2026-09-08')");
        }

        migrer(url, "15");

        try (var c = connexion(url); Statement st = c.createStatement()) {
            assertThat(texte(st, "SELECT transaction_source FROM transaction WHERE id = 5303")).isEqualTo("DECOMPTES");
            assertThat(texte(st, "SELECT libelle FROM transaction WHERE id = 5303")).isEqualTo("DECOMPTE GUERCIF-NADOR");
            assertThat(texte(st, "SELECT observation FROM transaction WHERE id = 5303")).isEqualTo("Marche: 04/2023/DRETLO");
            assertThat(texte(st, "SELECT tva_collectee FROM transaction WHERE id = 5303")).isEqualTo("1001258.36");
            assertThat(texte(st, "SELECT credit FROM transaction WHERE id = 5303")).isEqualTo("6007550.15");
            assertThat(texte(st, "SELECT rapprochement_id FROM transaction WHERE id = 5303")).isEqualTo("62");

            assertThat(texte(st, "SELECT count(*) FROM decompte")).isEqualTo("1");
            String decompte = "SELECT %s FROM decompte WHERE id = (SELECT referent FROM transaction WHERE id = 5303)";
            assertThat(texte(st, decompte.formatted("status"))).isEqualTo("PAYE");
            assertThat(texte(st, decompte.formatted("montant"))).isEqualTo("6007550.15");
            assertThat(texte(st, decompte.formatted("banque_encaissement_id"))).isEqualTo("3");
            assertThat(texte(st, decompte.formatted("marche_id"))).isEqualTo("2");
            assertThat(dates(st, decompte.formatted("date_emission"))).containsExactly(LocalDate.of(2026, 9, 7));
            assertThat(dates(st, decompte.formatted("date_operation"))).containsExactly(LocalDate.of(2026, 9, 7));
            assertThat(dates(st, decompte.formatted("date_valeur"))).containsExactly(LocalDate.of(2026, 9, 8));

            assertThat(texte(st, "SELECT count(*) FROM factoring WHERE id = 72")).isEqualTo("0");
        }
    }

    @Test
    @DisplayName("V15 : sans effet sur une base où le factoring 72 n'existe pas")
    void sansEffetSurBaseVierge() throws Exception {
        String url = baseJetable();
        migrer(url, null);
        try (var c = connexion(url); Statement st = c.createStatement()) {
            assertThat(texte(st, "SELECT count(*) FROM transaction")).isEqualTo("0");
            assertThat(texte(st, "SELECT count(*) FROM decompte")).isEqualTo("0");
        }
    }
}
