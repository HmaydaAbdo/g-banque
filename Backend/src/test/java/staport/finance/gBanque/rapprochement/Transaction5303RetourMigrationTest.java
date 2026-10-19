package staport.finance.gBanque.rapprochement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import staport.finance.gBanque.AbstractMigrationTest;

import java.sql.Statement;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Migration V17 : retour arrière de V15. Rejouée sur une base jetable amenée en V15 (état laissé par V15, avec l'historique
 * d'audit du factoring 72 et de la transaction 5303) ; puis sur une base où le cas n'existe pas (sans effet).
 */
class Transaction5303RetourMigrationTest extends AbstractMigrationTest {

    @Test
    @DisplayName("V17 : la transaction 5303 redevient celle du factoring 72, le décompte disparaît, les montants ne bougent pas")
    void transactionRetourneAuFactoring() throws Exception {
        String url = baseJetable();
        migrer(url, "15");
        try (var c = connexion(url); Statement st = c.createStatement()) {
            st.execute("SET session_replication_role = replica");
            st.execute("INSERT INTO banque (id, date_creation, date_modification, numero_compte, agence, nom, address, devise, logo_data) "
                    + "VALUES (3, now(), now(), 'IT-5303R', 'AGENCE', 'BANQUE 3', 'ADRESSE', 'MAD', decode('00', 'hex'))");
            st.execute("INSERT INTO marches (id, banque_id, actif, created_at, modified_at, montant, numero, maitre_douvrage, nom) "
                    + "VALUES (2, 3, true, now(), now(), 10000000.00, '04/2023/DRETLO', 'MO', 'GUERCIF-NADOR')");
            st.execute("INSERT INTO decompte (id, banque_encaissement_id, marche_id, montant, date_emission, date_operation, "
                    + "date_valeur, libelle, observation, status) VALUES (63, 3, 2, 6007550.15, '2026-09-07', '2026-09-07', "
                    + "'2026-09-08', 'DECOMPTE GUERCIF-NADOR', 'Marche: 04/2023/DRETLO', 'PAYE')");
            st.execute("INSERT INTO transaction (id, rapprochement_id, banque_id, libelle, observation, transaction_source, referent, "
                    + "credit, debit, tva_collectee, date_emission, date_operations, date_valeur) VALUES (5303, 62, 3, "
                    + "'DECOMPTE GUERCIF-NADOR', 'Marche: 04/2023/DRETLO', 'DECOMPTES', 63, "
                    + "6007550.15, 0.00, 1001258.36, '2026-09-07', '2026-09-07', '2026-09-08')");
            st.execute("INSERT INTO factoring_aud (rev, revtype, id, marche_id, montant, libelle, observation, status, type) "
                    + "VALUES (1, 0, 72, 2, 6007550.15, 'Déblocage factoring marché GUERCIF-NADOR', 'Marche N° 04/2023/DRETLO', "
                    + "'PAYE', 'DEBLOCAGE')");
            st.execute("INSERT INTO transaction_aud (rev, revtype, id, rapprochement_id, transaction_source, referent, libelle, "
                    + "observation, tva_collectee) VALUES (1, 0, 5303, 62, 'FACTORING', 72, "
                    + "'Déblocage factoring marché GUERCIF-NADOR', 'Marche N° 04/2023/DRETLO', 0.00)");
        }

        migrer(url, null);

        try (var c = connexion(url); Statement st = c.createStatement()) {
            assertThat(texte(st, "SELECT transaction_source FROM transaction WHERE id = 5303")).isEqualTo("FACTORING");
            assertThat(texte(st, "SELECT referent FROM transaction WHERE id = 5303")).isEqualTo("72");
            assertThat(texte(st, "SELECT libelle FROM transaction WHERE id = 5303")).isEqualTo("Déblocage factoring marché GUERCIF-NADOR");
            assertThat(texte(st, "SELECT observation FROM transaction WHERE id = 5303")).isEqualTo("Marche N° 04/2023/DRETLO");
            assertThat(texte(st, "SELECT tva_collectee FROM transaction WHERE id = 5303")).isEqualTo("0.00");
            assertThat(texte(st, "SELECT credit FROM transaction WHERE id = 5303")).isEqualTo("6007550.15");
            assertThat(texte(st, "SELECT rapprochement_id FROM transaction WHERE id = 5303")).isEqualTo("62");

            assertThat(texte(st, "SELECT type FROM factoring WHERE id = 72")).isEqualTo("DEBLOCAGE");
            assertThat(texte(st, "SELECT status FROM factoring WHERE id = 72")).isEqualTo("PAYE");
            assertThat(texte(st, "SELECT montant FROM factoring WHERE id = 72")).isEqualTo("6007550.15");
            assertThat(texte(st, "SELECT marche_id FROM factoring WHERE id = 72")).isEqualTo("2");
            assertThat(texte(st, "SELECT libelle FROM factoring WHERE id = 72")).isEqualTo("Déblocage factoring marché GUERCIF-NADOR");
            assertThat(dates(st, "SELECT date_emission FROM factoring WHERE id = 72")).containsExactly(LocalDate.of(2026, 9, 7));
            assertThat(dates(st, "SELECT date_operation FROM factoring WHERE id = 72")).containsExactly(LocalDate.of(2026, 9, 7));
            assertThat(dates(st, "SELECT date_valeur FROM factoring WHERE id = 72")).containsExactly(LocalDate.of(2026, 9, 8));

            assertThat(texte(st, "SELECT count(*) FROM decompte")).isEqualTo("0");
        }
    }

    @Test
    @DisplayName("V17 : sans effet sur une base où le décompte de V15 n'existe pas")
    void sansEffetSurBaseVierge() throws Exception {
        String url = baseJetable();
        migrer(url, null);
        try (var c = connexion(url); Statement st = c.createStatement()) {
            assertThat(texte(st, "SELECT count(*) FROM factoring")).isEqualTo("0");
            assertThat(texte(st, "SELECT count(*) FROM decompte")).isEqualTo("0");
        }
    }
}
