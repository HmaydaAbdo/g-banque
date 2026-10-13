package staport.finance.gBanque.rapprochement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import staport.finance.gBanque.AbstractMigrationTest;

import java.sql.Statement;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Migrations V9 (scission du remboursement de préfinancement 34 en capital + agios) et V10 (dates des préfinancements 36 et
 * 38 reprises de leur transaction). Rejouées sur une base jetable amenée en V8, avec les lignes réelles ; puis sur une base
 * où le cas n'existe pas (sans effet).
 */
class Prefinancement34MigrationTest extends AbstractMigrationTest {

    private static final String LIBELLE_AGIOS = "AGIOS / UTILISATION PREFINANCEMENT DU MARCHE GUERCIF-NADOR ";

    private static void inserer(Statement st, String sql) throws Exception {
        st.execute(sql);
    }

    private static String prefinancement(int id, String montant, String emission, String operation, String valeur) {
        return "INSERT INTO prefinancement (id, marche_id, type, status, montant, date_emission, date_operation, date_valeur, "
                + "libelle, observation) VALUES (" + id + ", 2, 'REMBOURSEMENT', 'PAYE', " + montant + ", '" + emission + "', '"
                + operation + "', '" + valeur + "', 'Remboursement', 'Marche N° 04/2023/DRETLO')";
    }

    private static String transaction(int id, int rapprochement, String debit, int referent, String emission, String operation,
                                      String valeur) {
        return "INSERT INTO transaction (id, rapprochement_id, banque_id, transaction_source, referent, debit, credit, "
                + "date_emission, date_operations, date_valeur, libelle, observation) VALUES (" + id + ", " + rapprochement
                + ", 3, 'PREFINANCEMENT', " + referent + ", " + debit + ", 0.00, '" + emission + "', '" + operation + "', '"
                + valeur + "', 'Remboursement', 'Marche N° 04/2023/DRETLO')";
    }

    @Test
    @DisplayName("V9 : le remboursement 34 est scindé en capital + agios validé, total débité inchangé ; V10 aligne les dates de 36 et 38")
    void scissionEtDates() throws Exception {
        String url = baseJetable();
        migrer(url, "8");
        try (var c = connexion(url); Statement st = c.createStatement()) {
            st.execute("SET session_replication_role = replica");
            // Le nouvel agios est inséré hors du mode « replica » (la migration vérifie la clé étrangère) : la banque doit exister.
            inserer(st, "INSERT INTO banque (id, date_creation, date_modification, numero_compte, agence, nom, address, devise, "
                    + "logo_data) VALUES (3, '2026-01-01', '2026-01-01', '000', 'AG', 'BMCI', 'Adresse', 'MAD', '\\x00')");
            inserer(st, "INSERT INTO rapprochement (id, banque_id, created_at, updated_at, statut, date_rapprochement) VALUES "
                    + "(18, 3, now(), now(), 'CLOTURE_DEFINITIVE', '2026-03-01'), (20, 3, now(), now(), 'CLOTURE_DEFINITIVE', '2026-03-01')");
            inserer(st, "INSERT INTO agios (id, banque_id, status, montant, date_emission, date_operation, date_valeur, libelle, "
                    + "observation) VALUES (22, 3, 'PAYE', 8950.86, '2026-03-16', '2026-03-18', '2026-03-16', '" + LIBELLE_AGIOS
                    + "', '" + LIBELLE_AGIOS + "')");
            inserer(st, prefinancement(34, "3993140.60", "2026-03-12", "2026-02-18", "2026-03-13"));
            inserer(st, transaction(1187, 18, "3993140.60", 34, "2026-03-12", "2026-03-13", "2026-02-18"));
            inserer(st, prefinancement(36, "2048512.49", "2026-03-16", "2026-03-16", "2026-03-18"));
            inserer(st, transaction(1305, 18, "2048512.49", 36, "2026-03-16", "2026-03-18", "2026-03-16"));
            inserer(st, prefinancement(38, "327200.00", "2026-03-19", "2026-03-23", "2026-03-19"));
            inserer(st, transaction(1450, 20, "327200.00", 38, "2026-03-19", "2026-03-19", "2026-03-24"));
        }

        migrer(url, null);

        try (var c = connexion(url); Statement st = c.createStatement()) {
            // Remboursement 34 : capital seul, dates de la transaction.
            assertThat(texte(st, "SELECT montant FROM prefinancement WHERE id = 34")).isEqualTo("3979764.15");
            assertThat(texte(st, "SELECT debit FROM transaction WHERE id = 1187")).isEqualTo("3979764.15");
            assertThat(dates(st, "SELECT date_operation FROM prefinancement WHERE id = 34")).containsExactly(LocalDate.of(2026, 3, 13));
            assertThat(dates(st, "SELECT date_valeur FROM prefinancement WHERE id = 34")).containsExactly(LocalDate.of(2026, 2, 18));

            // Nouvel agios validé et sa transaction, mêmes dates et textes de l'agios 22.
            assertThat(texte(st, "SELECT count(*) FROM agios WHERE id <> 22")).isEqualTo("1");
            assertThat(texte(st, "SELECT montant FROM agios WHERE id <> 22")).isEqualTo("13376.4500");
            assertThat(texte(st, "SELECT status FROM agios WHERE id <> 22")).isEqualTo("PAYE");
            assertThat(texte(st, "SELECT libelle FROM agios WHERE id <> 22")).isEqualTo(LIBELLE_AGIOS);
            assertThat(texte(st, "SELECT count(*) FROM transaction t JOIN agios a ON a.id = t.referent "
                    + "WHERE t.transaction_source = 'AGIOS' AND a.id <> 22 AND t.rapprochement_id = 18 AND t.debit = 13376.45 "
                    + "AND t.tva_deductible = 1216.04 AND t.date_operations = a.date_operation AND t.date_valeur = a.date_valeur "
                    + "AND t.date_emission = a.date_emission AND t.banque_id = a.banque_id")).isEqualTo("1");

            // Total débité du rapprochement 18 inchangé (3 993 140,60 + 2 048 512,49).
            assertThat(texte(st, "SELECT sum(debit) FROM transaction WHERE rapprochement_id = 18")).isEqualTo("6041653.09");

            // V10 : dates de 36 et 38 alignées sur leur transaction.
            assertThat(dates(st, "SELECT date_operation FROM prefinancement WHERE id = 36")).containsExactly(LocalDate.of(2026, 3, 18));
            assertThat(dates(st, "SELECT date_valeur FROM prefinancement WHERE id = 36")).containsExactly(LocalDate.of(2026, 3, 16));
            assertThat(dates(st, "SELECT date_operation FROM prefinancement WHERE id = 38")).containsExactly(LocalDate.of(2026, 3, 19));
            assertThat(dates(st, "SELECT date_valeur FROM prefinancement WHERE id = 38")).containsExactly(LocalDate.of(2026, 3, 24));
        }
    }

    @Test
    @DisplayName("V9 / V10 : sans effet sur une base où le cas n'existe pas")
    void sansEffetSurBaseVierge() throws Exception {
        String url = baseJetable();
        migrer(url, null);
        try (var c = connexion(url); Statement st = c.createStatement()) {
            assertThat(texte(st, "SELECT count(*) FROM agios")).isEqualTo("0");
            assertThat(texte(st, "SELECT count(*) FROM transaction")).isEqualTo("0");
        }
    }
}
