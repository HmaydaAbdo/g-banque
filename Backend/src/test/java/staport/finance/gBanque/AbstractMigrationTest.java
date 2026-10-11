package staport.finance.gBanque;

import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Socle des tests de migrations de données : une base jetable créée sur le PostgreSQL de test, amenée à une version
 * donnée, chargée de lignes « héritées », puis migrée jusqu'au bout. Les contrôles de clés étrangères se suspendent avec
 * {@code SET session_replication_role = replica} (le compte de test est superutilisateur) pour insérer des lignes minimales.
 */
public abstract class AbstractMigrationTest extends AbstractIntegrationTest {

    /** Crée une base vide sur le PostgreSQL de test et renvoie son URL JDBC. */
    protected static String baseJetable() throws SQLException {
        String nom = "mig_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        try (Connection c = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement st = c.createStatement()) {
            st.execute("CREATE DATABASE " + nom);
        }
        return POSTGRES.getJdbcUrl().replace("/" + POSTGRES.getDatabaseName(), "/" + nom);
    }

    protected static Connection connexion(String url) throws SQLException {
        return DriverManager.getConnection(url, POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    /** Applique les migrations jusqu'à la version donnée (toutes si {@code null}). */
    protected static void migrer(String url, String versionCible) {
        var config = Flyway.configure()
                .dataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration");
        if (versionCible != null) {
            config.target(versionCible);
        }
        config.load().migrate();
    }

    protected static List<LocalDate> dates(Statement st, String sql) throws SQLException {
        List<LocalDate> valeurs = new ArrayList<>();
        try (ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                valeurs.add(rs.getObject(1, LocalDate.class));
            }
        }
        return valeurs;
    }

    protected static String texte(Statement st, String sql) throws SQLException {
        try (ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }
}
