package staport.finance.gBanque.rapprochement;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import staport.finance.gBanque.AbstractIntegrationTest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Migration V4 : {@code rapprochement.date_rapprochement} passe de {@code bytea} (YearMonth Java sérialisé) à
 * {@code date}. Rejouée sur une base jetable amenée en V3 puis chargée de valeurs « héritées » au format exact
 * relevé en production (43 octets), avec les mêmes tables {@code rapprochement} et {@code rapprochement_aud}.
 */
class RapprochementDateMigrationTest extends AbstractIntegrationTest {

    /** Flux Java sérialisé d'un YearMonth : en-tête et descripteur de classe, puis bloc [type 12][année][mois]. */
    private static String legacy(int annee, int mois) {
        return "aced00057372000d6a6176612e74696d652e536572955d84ba1b2248b20c00007870"
                + "77060c" + String.format("%08x", annee) + String.format("%02x", mois) + "78";
    }

    @Test
    @DisplayName("La valeur de production (2026-01, 43 octets) est bien décodée par le jeu d'essai")
    void formatDeProduction() {
        assertThat(legacy(2026, 1))
                .isEqualTo("aced00057372000d6a6176612e74696d652e536572955d84ba1b2248b20c0000787077060c000007ea0178");
    }

    @Test
    @DisplayName("V4 : les dates héritées deviennent des dates (1er du mois), le NULL d'audit reste NULL")
    void conversionDesDonneesExistantes() throws Exception {
        String url = baseJetable();
        migrer(url, "3");
        try (Connection c = connexion(url); Statement st = c.createStatement()) {
            st.execute("SET session_replication_role = replica"); // lignes minimales : pas de banque ni de révision à créer
            st.execute("INSERT INTO rapprochement (banque_id, created_at, updated_at, statut, date_rapprochement) VALUES "
                    + "(1, now(), now(), 'EN_COURS', decode('" + legacy(2026, 1) + "', 'hex')),"
                    + "(1, now(), now(), 'CLOTURE_DEFINITIVE', decode('" + legacy(2025, 12) + "', 'hex'))");
            st.execute("INSERT INTO rapprochement_aud (rev, revtype, id, date_rapprochement) VALUES "
                    + "(1, 0, 1, decode('" + legacy(2026, 1) + "', 'hex')), (1, 1, 2, NULL)");
        }

        migrer(url, null);

        try (Connection c = connexion(url); Statement st = c.createStatement()) {
            assertThat(dates(st, "SELECT date_rapprochement FROM rapprochement ORDER BY id"))
                    .containsExactly(LocalDate.of(2026, 1, 1), LocalDate.of(2025, 12, 1));
            assertThat(dates(st, "SELECT date_rapprochement FROM rapprochement_aud ORDER BY id"))
                    .containsExactly(LocalDate.of(2026, 1, 1), null);
            assertThat(texte(st, "SELECT string_agg(data_type, ',' ORDER BY table_name) FROM information_schema.columns "
                    + "WHERE table_name IN ('rapprochement', 'rapprochement_aud') AND column_name = 'date_rapprochement'"))
                    .isEqualTo("date,date");
        }
    }

    @Test
    @DisplayName("V4 : un format binaire inattendu fait échouer la migration sans rien modifier")
    void formatInattenduRefuse() throws Exception {
        String url = baseJetable();
        migrer(url, "3");
        try (Connection c = connexion(url); Statement st = c.createStatement()) {
            st.execute("SET session_replication_role = replica");
            st.execute("INSERT INTO rapprochement (banque_id, created_at, updated_at, statut, date_rapprochement) VALUES "
                    + "(1, now(), now(), 'EN_COURS', decode('deadbeef', 'hex'))");
        }

        assertThatThrownBy(() -> migrer(url, null))
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("format binaire inattendu");

        try (Connection c = connexion(url); Statement st = c.createStatement()) {
            assertThat(texte(st, "SELECT data_type FROM information_schema.columns "
                    + "WHERE table_name = 'rapprochement' AND column_name = 'date_rapprochement'")).isEqualTo("bytea");
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Crée une base vide sur le PostgreSQL de test et renvoie son URL JDBC. */
    private static String baseJetable() throws SQLException {
        String nom = "mig_v4_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        try (Connection c = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement st = c.createStatement()) {
            st.execute("CREATE DATABASE " + nom);
        }
        return POSTGRES.getJdbcUrl().replace("/" + POSTGRES.getDatabaseName(), "/" + nom);
    }

    private static Connection connexion(String url) throws SQLException {
        return DriverManager.getConnection(url, POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    /** Applique les migrations jusqu'à la version donnée (toutes si {@code null}). */
    private static void migrer(String url, String versionCible) {
        var config = Flyway.configure()
                .dataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration");
        if (versionCible != null) {
            config.target(versionCible);
        }
        config.load().migrate();
    }

    private static List<LocalDate> dates(Statement st, String sql) throws SQLException {
        List<LocalDate> valeurs = new ArrayList<>();
        try (ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                valeurs.add(rs.getObject(1, LocalDate.class));
            }
        }
        return valeurs;
    }

    private static String texte(Statement st, String sql) throws SQLException {
        try (ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }
}
